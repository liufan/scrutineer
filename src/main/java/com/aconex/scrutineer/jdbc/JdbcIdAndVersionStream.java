package com.aconex.scrutineer.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;

import com.aconex.scrutineer.IdAndVersion;
import com.aconex.scrutineer.IdAndVersionFactory;
import com.aconex.scrutineer.IdAndVersionStream;
import com.aconex.scrutineer.LogUtils;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

public class JdbcIdAndVersionStream implements IdAndVersionStream {

    private static final Logger LOG = LogUtils.loggerForThisClass();
    private static final int DEFAULT_FETCH_SIZE = 1000;

    private final Connection connection;
    private final String sql;
	private final IdAndVersionFactory factory;

    private Statement statement;
    private ResultSet resultSet;
    private Iterator<IdAndVersion> iterator;
    private final int fetchSize;

    public JdbcIdAndVersionStream(Connection connection, String sql, IdAndVersionFactory factory){
        this(connection, sql, factory, DEFAULT_FETCH_SIZE);

    }

    public JdbcIdAndVersionStream(Connection connection, String sql, IdAndVersionFactory factory, int fetchSize) {
        this.connection = connection;
        this.sql = sql;
        this.factory = factory;
        this.fetchSize = fetchSize;
    }

    @Override
    public void open() {
        long begin = System.currentTimeMillis();
        this.iterator = createIterator(factory);
        LogUtils.info(LOG, "Executed JDBC query in %dms", (System.currentTimeMillis() - begin));
    }

    @Override
    public Iterator<IdAndVersion> iterator() {
        return iterator;
    }

    private Iterator<IdAndVersion> createIterator(IdAndVersionFactory factory) {
        try {
            createStatementAndResultSet();
            return new IdAndVersionResultSetIterator(resultSet, factory);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createStatementAndResultSet() throws SQLException {
        connection.setAutoCommit(false);
        statement = connection.createStatement();
        statement.setFetchSize(fetchSize);
        resultSet = statement.executeQuery(sql);
    }

    @Override
    public void close() {
        this.iterator = null;
        throwExceptionIfAnyCloseFails(closeResultSet(), closeStatement());
    }

    private void throwExceptionIfAnyCloseFails(SQLException... sqlExceptions) {
        if (!Iterables.all(Arrays.asList(sqlExceptions), Predicates.<Object>isNull())) {
            throw new RuntimeException("At least one error occured during close, see logs for more details, there may be multiple");
        }
    }

    @SuppressWarnings("PMD.NcssMethodCount")
    private SQLException closeStatement() {
        SQLException sqlException = null;
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                sqlException = e;
                LogUtils.error(LOG, "Cannot close statement", e);
            }
        }
        return sqlException;
    }

    @SuppressWarnings("PMD.NcssMethodCount")
    private SQLException closeResultSet() {
        SQLException sqlException = null;
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                sqlException = e;
                LogUtils.error(LOG, "Cannot close resultset", e);
            }
        }
        return sqlException;
    }

}
