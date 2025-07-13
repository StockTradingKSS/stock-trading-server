package com.KimStock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DSQL용 간단한 연결 풀
 * IAM 토큰 만료 문제를 해결하기 위해 연결을 5분마다 갱신
 */
@Slf4j
@Component
public class DsqlConnectionPool {

    private final DsqlConfig dsqlConfig;
    private final BlockingQueue<PooledConnection> pool;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final int maxPoolSize = 5;
    private final long maxConnectionAge = 4 * 60 * 1000; // 4분 (토큰 만료 전)

    public DsqlConnectionPool(DsqlConfig dsqlConfig) {
        this.dsqlConfig = dsqlConfig;
        this.pool = new LinkedBlockingQueue<>(maxPoolSize);
    }

    /**
     * 연결 획득
     */
    public Connection getConnection() throws SQLException {
        PooledConnection pooledConn = pool.poll();
        
        // 풀에 사용 가능한 연결이 없거나 만료된 경우
        if (pooledConn == null || isExpired(pooledConn)) {
            if (pooledConn != null) {
                closeQuietly(pooledConn.connection);
                activeConnections.decrementAndGet();
            }
            
            if (activeConnections.get() < maxPoolSize) {
                Connection newConn = dsqlConfig.getDsqlConnection();
                activeConnections.incrementAndGet();
                log.debug("새로운 DSQL 연결 생성. 활성 연결 수: {}", activeConnections.get());
                return new PooledConnectionWrapper(newConn, this);
            } else {
                throw new SQLException("DSQL 연결 풀이 가득참");
            }
        }
        
        log.debug("풀에서 DSQL 연결 재사용");
        return new PooledConnectionWrapper(pooledConn.connection, this);
    }

    /**
     * 연결 반환
     */
    public void returnConnection(Connection connection) {
        if (connection instanceof PooledConnectionWrapper) {
            Connection realConnection = ((PooledConnectionWrapper) connection).getRealConnection();
            PooledConnection pooledConn = new PooledConnection(realConnection, System.currentTimeMillis());
            
            if (!pool.offer(pooledConn)) {
                // 풀이 가득 찬 경우 연결 닫기
                closeQuietly(realConnection);
                activeConnections.decrementAndGet();
            }
        }
    }

    /**
     * 연결이 만료되었는지 확인
     */
    private boolean isExpired(PooledConnection pooledConn) {
        return System.currentTimeMillis() - pooledConn.createdTime > maxConnectionAge;
    }

    /**
     * 조용히 연결 닫기
     */
    private void closeQuietly(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.warn("연결 닫기 실패", e);
        }
    }

    /**
     * 풀된 연결 정보
     */
    private static class PooledConnection {
        final Connection connection;
        final long createdTime;

        PooledConnection(Connection connection, long createdTime) {
            this.connection = connection;
            this.createdTime = createdTime;
        }
    }

    /**
     * 연결 래퍼 (풀로 반환하기 위해)
     */
    private static class PooledConnectionWrapper implements Connection {
        private final Connection realConnection;
        private final DsqlConnectionPool pool;
        private boolean closed = false;

        PooledConnectionWrapper(Connection realConnection, DsqlConnectionPool pool) {
            this.realConnection = realConnection;
            this.pool = pool;
        }

        Connection getRealConnection() {
            return realConnection;
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                closed = true;
                pool.returnConnection(this);
            }
        }

        // 나머지 Connection 인터페이스 메서드들은 realConnection에 위임
        @Override
        public java.sql.Statement createStatement() throws SQLException {
            return realConnection.createStatement();
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
            return realConnection.prepareStatement(sql);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
            return realConnection.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return realConnection.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            realConnection.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return realConnection.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            realConnection.commit();
        }

        @Override
        public void rollback() throws SQLException {
            realConnection.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed || realConnection.isClosed();
        }

        @Override
        public java.sql.DatabaseMetaData getMetaData() throws SQLException {
            return realConnection.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            realConnection.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return realConnection.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            realConnection.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return realConnection.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            realConnection.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return realConnection.getTransactionIsolation();
        }

        @Override
        public java.sql.SQLWarning getWarnings() throws SQLException {
            return realConnection.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            realConnection.clearWarnings();
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return realConnection.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
            return realConnection.getTypeMap();
        }

        @Override
        public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
            realConnection.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            realConnection.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return realConnection.getHoldability();
        }

        @Override
        public java.sql.Savepoint setSavepoint() throws SQLException {
            return realConnection.setSavepoint();
        }

        @Override
        public java.sql.Savepoint setSavepoint(String name) throws SQLException {
            return realConnection.setSavepoint(name);
        }

        @Override
        public void rollback(java.sql.Savepoint savepoint) throws SQLException {
            realConnection.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
            realConnection.releaseSavepoint(savepoint);
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return realConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return realConnection.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return realConnection.prepareStatement(sql, columnIndexes);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return realConnection.prepareStatement(sql, columnNames);
        }

        @Override
        public java.sql.Clob createClob() throws SQLException {
            return realConnection.createClob();
        }

        @Override
        public java.sql.Blob createBlob() throws SQLException {
            return realConnection.createBlob();
        }

        @Override
        public java.sql.NClob createNClob() throws SQLException {
            return realConnection.createNClob();
        }

        @Override
        public java.sql.SQLXML createSQLXML() throws SQLException {
            return realConnection.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return realConnection.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) {
            try {
                realConnection.setClientInfo(name, value);
            } catch (SQLException e) {
                // 무시
            }
        }

        @Override
        public void setClientInfo(java.util.Properties properties) {
            try {
                realConnection.setClientInfo(properties);
            } catch (SQLException e) {
                // 무시
            }
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return realConnection.getClientInfo(name);
        }

        @Override
        public java.util.Properties getClientInfo() throws SQLException {
            return realConnection.getClientInfo();
        }

        @Override
        public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return realConnection.createArrayOf(typeName, elements);
        }

        @Override
        public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return realConnection.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            realConnection.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return realConnection.getSchema();
        }

        @Override
        public void abort(java.util.concurrent.Executor executor) throws SQLException {
            realConnection.abort(executor);
        }

        @Override
        public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {
            realConnection.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return realConnection.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return realConnection.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return realConnection.isWrapperFor(iface);
        }
    }
}
