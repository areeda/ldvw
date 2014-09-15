/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.areeda.jaDatabaseSupport;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set of routines for accessing mySql or sqlite databases.
 * This class is not meant as a "unifying wrapper" but rather a centralized collection that
 * helps perform common operations and keeps track of statistical information and errors.
 * 
 * @author joe areeda
 */
public class Database
{

    private String host = "";
    private Integer port = 3306;
    private String dbname = "";
    private String user = "";
    private String pass = "";
    private DbType dbt;

    private String connUrl = "";
    private Connection conn;
    private Connection conn2;       // alternate needed for streaming result sets
    private Statement stmt;
    private final HashMap<String, Table> tables = new HashMap<>();
    private HashMap<String, String> tableList = null;
    
    private long nQueries = 0;
    private long totalMs = 0;

    /**
     * define which type of database driver we're to use.  
     * It also affects the encoding and the table/column definitions
     */
    public enum DbType { SQLITE, MYSQL};
    /**
     * Constructor defines connection parameters but does not make a connection.  Assumes a mysql db
     * @param databaseName
     * @param userName
     * @param password
     * @throws java.sql.SQLException
     */
    public Database(String databaseName, String userName, String password) throws SQLException
    {
        init(databaseName, userName,password,"mysql");
    }

    /**
     *
     * @param databaseName
     * @param userName
     * @param password
     * @param type
     * @throws SQLException
     */
    public Database (String databaseName, String userName, String password, String type) throws SQLException
    {
        init(databaseName, userName,password,type);
    }

    private void init(String databaseName, String userName, String password, String type) throws SQLException
    {
        this.dbname = databaseName;
        this.user = userName;
        this.pass = password;

        if (type.equalsIgnoreCase("mysql"))
        {
            dbt = DbType.MYSQL;
        }
        else if (type.equalsIgnoreCase("sqlite"))
        {
            dbt = DbType.SQLITE;
        }
        else
        {
            throw new SQLException("Unknown database type: " + type);
        }
        CType.setDbt(dbt);
    }
    public void makeConnection() throws SQLException, ClassNotFoundException
    {
        try
        {
            switch(dbt)
            {
                case MYSQL:
                    Class.forName("com.mysql.jdbc.Driver");
                break;

                case SQLITE:
                    Class.forName("org.sqlite.JDBC");
                    break;
            }
            makeConnUrl();
            conn = DriverManager.getConnection(connUrl, user, pass);
            conn2 = null;
            stmt = conn.createStatement();
        }
        catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            throw (ex);
        }
    }
    /**
     * Close the database connection.  This should be done every time;
     */
    public void close()
    {
        try
        {
            if (stmt != null)
            {
                stmt.close();
                stmt=null;
            }
            if (conn!=null)
            {
                conn.close();
                conn = null;
            }
            
            if (conn2 != null)
            {
                conn2.close();
                conn2 = null;
            }
            
                        
        }
        catch (SQLException ex)
        {
           // well we tried, don't care if it didn't work
        }
    }
    private void makeConnUrl()
    {
        switch(dbt)
        {
            case MYSQL:
                connUrl = "jdbc:mysql://";

                connUrl += (host.length() > 0 ? host : "localhost");
                connUrl += ":" + port.toString() + "/" + dbname;
                break;

            case SQLITE:
                connUrl = "jdbc:sqlite:" + dbname;
                break;
        }
    }

    public String getDbname()
    {
        return dbname;
    }

    public void setDbname(String dbname)
    {
        this.dbname = dbname;
    }

    public DbType getDbt()
    {
        return dbt;
    }

    public void setDbt(DbType dbt)
    {
        this.dbt = dbt;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    protected Connection getConn()
    {
        return conn;
    }

    public String getPass()
    {
        return pass;
    }

    public void setPass(String pass)
    {
        this.pass = pass;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String[] getTableNames()
    {
        String[] ret = new String[0];
        try
        {
            if (this.tableList == null)
            {
                buildTableList();
            }
            ArrayList<String> tbl = new ArrayList<String>(tableList.keySet());
            Collections.sort(tbl);
            ret = tbl.toArray(ret);
        }
        catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    public void getTablesFromDB()
    {
        String[] tblNames = getTableNames();

        for (String t : tblNames)
        {
            Table tbl = new Table(this, t);
            tbl.makeColumnsFromDB();

            this.tables.put(t, tbl);
        }
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        return this.conn.getMetaData();
    }

    /**
     *
     * Same as Conection.createStatment
     * @see Connection#createStatement() 
     * @return java.sql.Statement object on primary connection
     * @throws SQLException
     */
    public Statement createStatement() throws SQLException
    {
        return conn.createStatement();
    }
    /**
     *
     * Same as Connection.createStatment but uses a second connection which is needed
     * if say one connection is doing a streaming query.
     * @return java.sql.Statement object on secondary connection
     * @throws SQLException
     */
    public Statement createStatement2() throws SQLException
    {
        if (conn2 == null)
        {
            conn2 = DriverManager.getConnection(connUrl, user, pass);
        }
        return conn2.createStatement();
    }

    /**
     * Create a PreparedStatement for database operations
     * @see Connection#prepareStatement(java.lang.String, int) 
     * @param sqlStmt  an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param genKeys - a flag indicating whether auto-generated keys should be returned; one of Statement.RETURN_GENERATED_KEYS or Statement.NO_GENERATED_KEYS
     * @return
     * @throws SQLException 
     */
    public PreparedStatement prepareStatement(String sqlStmt, int genKeys) throws SQLException
    {
        PreparedStatement ret;
        if (conn == null)
        {
            throw new IllegalStateException("Database:preparedStatement called with out a connection");
        }
        ret = conn.prepareStatement(sqlStmt, genKeys);
        return ret ;
    }

    /**
     * Create a statement object for reading large result sets that are too big to want to keep in memory
     * @see Connection#createStatement(int, int) 
     * @param fetchSize how many records to fetch at one time
     * @return a statement object to run queries
     * @throws SQLException 
     */
    public Statement createStatement(int fetchSize) throws SQLException
    {
        Statement newstmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                              java.sql.ResultSet.CONCUR_READ_ONLY);
        // note:  mysql does not seem to use any fetch size except Integer.MIN_VALUE
        // which does seem to work.
        newstmt.setFetchSize(Integer.MIN_VALUE);
        return newstmt;
    }

    /**
     * Check if the named table exists in the database
     * @param tblNam the name of the table to verify
     * @return true if table exists (no matter how it's defined)
     */
    public boolean tableExists(String tblNam)
    {
        boolean ret = false;
        try
        {
            buildTableList();
            ret = tableList.containsKey(tblNam);
        }
        catch (SQLException ex)
        {
            // ignore
        }
        return ret;
    }

    /**
     * Create a Map of existing tables.
     * Note: this will query the database each time so buffering must be done
     *  outside this routine.
     */
    private void buildTableList() throws SQLException
    {
        if (tableList == null)
        {
            this.tableList = new HashMap<>();

            switch (dbt)
            {
                case MYSQL:
                    String q = "SHOW FULL TABLES";
                    ResultSet rs = stmt.executeQuery(q);
                    String fld = "Tables_in_" + this.dbname;
                    String typ = "Table_type";
                    while (rs.next())
                    {
                        String name = rs.getString(fld);
                        String ttyp = rs.getString(typ);
                        tableList.put(name, ttyp);
                    }
                    break;

                case SQLITE:
                    String qs = "select name from sqlite_master where type = 'table'";
                    ResultSet rss = stmt.executeQuery(qs);
                    while(rss.next())
                    {
                        String name = rss.getString("name");
                        tableList.put(name, "sqllite");
                    }
                    break;
            }
        }
    }
    /**
     * Executes the given SQL query string, which returns a single ResultSet object.
     * This implementation keeps statistics and handles errors
     * 
     * @throws java.sql.SQLException
     * @SEE Statement#executeQuery
     * @param q an SQL statement to be sent to the database, typically a static SQL SELECT statement
     * @return a ResultSet object that contains the data produced by the given query or null if statement failed (different from std)
     */
    public ResultSet executeQuery(String q) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        ResultSet ret = null;
        int cnt = 3;
        boolean keepTrying = true;
        SQLException rex = null;

        while(cnt > 0 & keepTrying)
        {
            try
            {
                cnt--;
                Statement st = createStatement();
                ret = st.executeQuery(q);
                keepTrying = false;
            }
            catch (SQLException ex)
            {
                rex = ex;
            }
        }
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        if (rex != null)
        {
            throw rex;
        }
        return ret;
    }
    
    /**
     * Execute a prepared statement, keeping track of timing
     * @param ps
     * @return
     * @throws SQLException
     */
    public ResultSet executeQuery(PreparedStatement ps) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        ResultSet ret;
        ret = ps.executeQuery();
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    
    public ResultSet executeUpdateGetKeys(String updateSql) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        ResultSet ret=null;
        
        if (stmt.executeUpdate(updateSql, Statement.RETURN_GENERATED_KEYS) > 0)
        {
            ret = stmt.getGeneratedKeys();
        }

        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    /**
     * Executes the given SQL query string, which returns a single ResultSet object.
     * This implementation keeps statistics and handles errors
     * This form of executeQuery would be used if you wanted to specify fetch size for a sequential
     * read operation
     * 
     * @see Database#createStatement(int) 
     * @SEE Statement#executeQuery
     * @param stmt previously created statement
     * @param q query string
     * @return the results
     * @throws SQLException 
     */
    public ResultSet executeQuery(Statement stmt, String q) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        ResultSet ret;
        ret = stmt.executeQuery(q);
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    /**
     * Executes the SQL statement in this PreparedStatement object, which must be an SQL Data Manipulation Language (DML) statement, such as INSERT, UPDATE or DELETE; or an SQL statement that returns nothing, such as a DDL statement.
     * @param ps the properly prepared statement to execute
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements or (2) 0 for SQL statements that return nothing
     * @throws SQLException 
     */
    public int executeUpdate(PreparedStatement ps) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        int ret;
        ret = ps.executeUpdate();
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    /**
     * Retrieves any auto-generated keys created as a result of executing this Statement object. If this Statement object did not generate any keys, an empty ResultSet object is returned.
     * @param ps the properly prepared statement to execute
     * @return a ResultSet object containing the auto-generated key(s) generated by the execution of this Statement object
     * @throws SQLException 
     */
    public ResultSet getGeneratedKeys(PreparedStatement ps) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        ResultSet ret;
        ret = ps.getGeneratedKeys();
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    /**
     * Executes the given SQL statement, which may return multiple results. In some (uncommon)
     * situations, a single SQL statement may return multiple result sets and/or update counts.
     * Normally you can ignore this unless you are (1) executing a stored procedure that you know
     * may return multiple results or (2) you are dynamically executing an unknown SQL string. 
     * 
     * The execute method executes an SQL statement and indicates the form of the first result. You must
     * then use the methods getResultSet or getUpdateCount to retrieve the result, and
     * getMoreResults to move to any subsequent result(s).
     *
     * @param q any SQL statement
     * @return true if the first result is a ResultSet object; false if it is an update count or there are no results
     * @throws SQLException 
     */
    public boolean execute(String q) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        boolean ret = stmt.execute(q);
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    /**
     * Executes the SQL statement in this PreparedStatement object, which may be any kind of SQL statement. 
     * Some prepared statements return multiple results; the execute method handles these complex 
     * statements as well as the simpler form of statements handled by the methods executeQuery and 
     * executeUpdate.
     * 
     * The execute method returns a boolean to indicate the form of the first result. 
     * You must call either the method getResultSet or getUpdateCount to retrieve the result; 
     * you must call getMoreResults to move to any subsequent result(s).
     * @param ps the properly prepared statement to execute
     * @return true if the first result is a ResultSet object; false if the first result is an update count or there is no result
     * @throws SQLException 
     */
    public boolean execute(PreparedStatement ps) throws SQLException
    {
        long strtMs = System.currentTimeMillis();
        boolean ret;
        ret = ps.execute();
        nQueries++;
        totalMs += System.currentTimeMillis() - strtMs;
        return ret;
    }
    
    /**
     * Deletes a table from the database
     * @param tempTbl
     */
    public void dropTable(String tempTbl)
    {
        String dt = "DROP TABLE IF EXISTS " + tempTbl;
        try
        {
            execute(dt);
        }
        catch (SQLException ex)
        {
            // this one we'll let slide
        }
    }
    
    /**
     * get number of queries since object was created
     * 
     * @return the number
     */
    public long getnQueries()
    {
        return nQueries;
    }

    /**
     * Get the total time in ms spent executing SQL statements
     * @return
     */
    public long getTotalMs()
    {
        return totalMs;
    }
    /**
     * Request all the interesting stuff from the InformationSchema table mysql only
     * @return Result set from the query
     * @throws java.sql.SQLException
     */
    public ResultSet getISStats() throws SQLException
    {
        String q = "select table_name,update_time,row_format,table_rows,avg_row_length,"
                    + "data_length,max_data_length,index_length,data_free,create_options "
                + "from information_schema.tables where table_schema='" + getDbname() + "'";
        
        ResultSet ret = executeQuery(q);
        return ret;
    }    
}
