package com.tarkhangurbanli.libcommonlogger.listener;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.springframework.core.env.Environment;

/**
 * Implementation of {@link QueryExecutionListener} that logs SQL queries executed
 * by the proxied DataSource.
 *
 * <p>This listener logs the raw SQL query, execution time, number of rows affected,
 * and optionally inlines query parameters depending on the active Spring profile
 * and configured properties.</p>
 *
 * <p>Parameter logging is enabled only in {@code dev} or {@code local} profiles for safety,
 * and a warning is issued if enabled in other profiles.</p>
 *
 * <p>Query formatting attempts to inline parameters for better readability, especially
 * for batched INSERT statements.</p>
 *
 * <p>Logs output to standard output using {@code System.out.printf}, but could be adapted
 * to use logger as well.</p>
 *
 * @author Tarkhan Gurbanli
 * @since 1.0.0
 */
@Slf4j
public class QueryExecutionListenerImpl implements QueryExecutionListener {

    private final boolean paramLogEnabled;

    /**
     * Constructs a new listener instance.
     *
     * <p>Determines whether inline parameter logging is enabled based on the
     * active Spring profiles and configuration properties.
     * Inline parameter logging is only allowed for 'dev' or 'local' profiles,
     * and a warning is logged if parameters are enabled outside these profiles.</p>
     *
     * @param env the Spring Environment used to access active profiles and properties
     */
    public QueryExecutionListenerImpl(Environment env) {
        String profiles = env.getProperty("spring.profiles.active", "");
        boolean isDevOrLocal = Stream.of(profiles.split(","))
                .map(String::trim)
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local"));

        boolean paramLogging = env.getProperty("spring.jpa.sql-logging.show-parameters", Boolean.class, false);

        if (!isDevOrLocal) {
            if (paramLogging) {
                log.warn("Parameter logging is ENABLED in non-dev environment [{}]; ignoring inline parameters for safety.",
                        profiles);
                paramLogging = false;
            }
            log.warn("SQL logging is ENABLED in non-dev environment [{}]; consider disabling before production.",
                    profiles);
        }

        this.paramLogEnabled = isDevOrLocal && paramLogging;
    }

    /**
     * Called before a query executes.
     *
     * <p>Currently no logic is implemented in this method but can be
     * extended to perform actions prior to query execution.</p>
     *
     * @param executionInfo information about the query execution context
     * @param queryInfoList list of query details about the SQL statements to be executed
     */
    @Override
    public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        // No action needed before query execution
    }

    /**
     * Called after a query executes.
     *
     * <p>Logs the executed SQL query (optionally with inline parameters),
     * the number of rows affected or columns returned, batch size if applicable,
     * and the elapsed execution time.</p>
     *
     * @param execInfo information about the query execution, including timing and result
     * @param queryInfoList details about the executed SQL queries
     */
    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        String rawSql = getRawSql(queryInfoList);
        String query = (paramLogEnabled && rawSql.contains("?"))
                ? formatSqlWithParams(rawSql, queryInfoList)
                : normalize(rawSql);

        long timeMs = execInfo.getElapsedTime();
        int batchSize = execInfo.getBatchSize();
        Object result = execInfo.getResult();
        String batchInfo = batchSize > 1 ? " batchSize=" + batchSize : "";

        if (result instanceof ResultSet) {
            int cols = 0;
            try {
                cols = ((ResultSet) result).getMetaData().getColumnCount();
            } catch (SQLException ignored) {
                // Ignore metadata retrieval failures
            }
            System.out.printf("Query: %s | cols=%d%s time=%dms%n", query, cols, batchInfo, timeMs);
        } else {
            int rowsAffected = extractRowCount(result, batchSize);
            System.out.printf("Query: %s | rowsAffected=%d%s time=%dms%n", query, rowsAffected, batchInfo, timeMs);
        }
    }

    /**
     * Extracts the number of rows affected by the executed query.
     *
     * <p>Supports various result types such as an array of counts (batch updates)
     * or a single integer (single update count).</p>
     *
     * @param result the execution result object
     * @param batchSize size of the batch if the query was executed in batch mode
     * @return the total number of affected rows
     */
    private int extractRowCount(Object result, int batchSize) {
        if (result instanceof int[] counts) {
            return batchSize > 0 ? batchSize : Arrays.stream(counts).filter(c -> c > 0).sum();
        }
        if (result instanceof Integer single) {
            return single;
        }
        return 0;
    }

    /**
     * Formats the raw SQL query by inlining parameter values for better readability.
     *
     * <p>Special handling is applied for batched INSERT statements to format
     * multiple value tuples neatly. For other statements, parameters are replaced
     * sequentially where placeholders '?' appear.</p>
     *
     * @param rawSql the raw SQL query with parameter placeholders
     * @param queryInfoList list containing parameter details for each query
     * @return the SQL query string with parameters inlined
     */
    private String formatSqlWithParams(String rawSql, List<QueryInfo> queryInfoList) {
        String sqlLc = rawSql.toLowerCase();
        if (sqlLc.startsWith("insert") && sqlLc.contains("values")) {
            int idx = sqlLc.indexOf("values");
            String prefix = rawSql.substring(0, idx + "values".length());
            List<String> tuples = new ArrayList<>();
            for (QueryInfo qi : queryInfoList) {
                for (List<ParameterSetOperation> paramOps : qi.getParametersList()) {
                    if (paramOps.isEmpty()) {
                        continue;
                    }
                    String tuple = paramOps.stream()
                            .map(p -> formatValue(p.getArgs()[1]))
                            .collect(Collectors.joining(", ", "(", ")"));
                    tuples.add(tuple);
                }
            }
            if (!tuples.isEmpty()) {
                return normalize(prefix) + " " + String.join(", ", tuples) + ";";
            }
        }

        List<String> filled = new ArrayList<>();
        for (QueryInfo qi : queryInfoList) {
            for (List<ParameterSetOperation> paramOps : qi.getParametersList()) {
                if (paramOps.isEmpty()) {
                    continue;
                }
                String filledSql = rawSql;
                for (ParameterSetOperation p : paramOps) {
                    Object val = p.getArgs()[1];
                    filledSql = filledSql.replaceFirst("\\?", Matcher.quoteReplacement(formatValue(val)));
                }
                filled.add(filledSql);
            }
        }
        if (!filled.isEmpty()) {
            return normalize(String.join(" ; ", filled)) + ";";
        }
        return normalize(rawSql);
    }

    /**
     * Formats a parameter value for SQL inline insertion.
     *
     * <p>Null values are converted to the string 'null', numbers are converted directly,
     * and strings are enclosed in single quotes with internal quotes escaped.</p>
     *
     * @param val the parameter value to format
     * @return the formatted string representation for SQL
     */
    private String formatValue(Object val) {
        if (val == null) {
            return "null";
        }
        if (val instanceof Number) {
            return val.toString();
        }
        String str = String.valueOf(val).replace("'", "\\'");
        return "'" + str + "'";
    }

    /**
     * Concatenates raw SQL queries from the list of {@link QueryInfo}.
     *
     * @param queryInfoList list of query info objects containing raw SQL strings
     * @return concatenated SQL queries separated by " ; "
     */
    private String getRawSql(List<QueryInfo> queryInfoList) {
        return queryInfoList.stream()
                .map(QueryInfo::getQuery)
                .collect(Collectors.joining(" ; "));
    }

    /**
     * Normalizes the SQL query string by trimming whitespace and replacing
     * multiple spaces or line breaks with a single space.
     *
     * @param sql the raw SQL string
     * @return normalized SQL string
     */
    private String normalize(String sql) {
        return sql.trim().replaceAll("\\s+", " ");
    }

}
