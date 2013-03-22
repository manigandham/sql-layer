
package com.akiban.sql.pg;

import com.akiban.server.types3.Types3Switch;
import com.akiban.sql.pg.PostgresEmulatedMetaDataStatement.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Handle known system table queries from tools directly.  At some
 * point it may be possible to actually implement <code>pg_</code>
 * tables as views against Akiban's own information schema. But for
 * now, some of the queries do not even parse in Akiban SQL.
 */
public class PostgresEmulatedMetaDataStatementParser implements PostgresStatementParser
{
    private static final Logger logger = LoggerFactory.getLogger(PostgresEmulatedMetaDataStatementParser.class);

    /** Quickly determine whether a given query <em>might</em> be a
     * Postgres system table. */
    public static final String POSSIBLE_PG_QUERY = "FROM\\s+PG_|PG_CATALOG\\.";
    
    private Pattern possiblePattern;

    public PostgresEmulatedMetaDataStatementParser(PostgresServerSession server) {
        possiblePattern = Pattern.compile(POSSIBLE_PG_QUERY, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public PostgresStatement parse(PostgresServerSession server,
                                   String sql, int[] paramTypes)  {
        if (!possiblePattern.matcher(sql).find())
            return null;
        List<String> groups = new ArrayList<>();
        for (Query query : Query.values()) {
            if (query.matches(sql, groups)) {
                logger.debug("Emulated: {}{}", query, groups.subList(1, groups.size()));
                return new PostgresEmulatedMetaDataStatement(query, groups, server.getBooleanProperty("newtypes", Types3Switch.ON));
            }
        }
        return null;
    }

    @Override
    public void sessionChanged(PostgresServerSession server) {
    }

}
