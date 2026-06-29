package com.github.namuan.fintech;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

final class ThrowingDataSource implements DataSource {
    @Override public Connection getConnection() throws SQLException { throw new SQLFeatureNotSupportedException("test data source does not open connections"); }
    @Override public Connection getConnection(String username, String password) throws SQLException { throw new SQLFeatureNotSupportedException("test data source does not open connections"); }
    @Override public PrintWriter getLogWriter() { return null; }
    @Override public void setLogWriter(PrintWriter out) { }
    @Override public void setLoginTimeout(int seconds) { }
    @Override public int getLoginTimeout() { return 0; }
    @Override public Logger getParentLogger() { return Logger.getGlobal(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
