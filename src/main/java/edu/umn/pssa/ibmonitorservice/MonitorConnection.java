package edu.umn.pssa.ibmonitorservice;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class MonitorConnection {
	private static final String DB_TYPE_ORACLE = "ORACLE";
	private static final String DB_TYPE_SQLSERVER = "SQLSERVER";
	private static final String CHAR_COLUMN_ORACLE = "VARCHAR2";
	private static final String CHAR_COLUMN_SQLSERVER = "VARCHAR";
	private static final String NBR_COLUMN_ORACLE = "NUMBER";
	private static final String NBR_COLUMN_SQLSERVER = "FLOAT";
	
	// Table Creation Script
	private static final String IB_MONITOR_TABLE_CREATE = "CREATE TABLE UM_IB_MONITOR \n" +
                                                          "( DATABASE_NAME     <CHAR_COLUMN>(20)    NOT NULL, \n" +
                                                          "  EVENT_NAME        <CHAR_COLUMN>(100)   NOT NULL, \n" +
                                                          "  EVENT_TYPE        <CHAR_COLUMN>(30)    NOT NULL, \n" +
                                                          "  ESCALATION        <NBR_COLUMN>         NOT NULL, \n" +
                                                          "  IBTRANSACTIONID   <CHAR_COLUMN>(36)    NOT NULL)";

	// Logger
	private static final Logger logger = Logger.getLogger(IBMonitorSvc.class.getName());

	private static String dbType;
	
	public MonitorConnection() {
		
	}
	
	public MonitorConnection(String databaseType) {
		dbType = databaseType;
	}

	public boolean testConnectivity() {
		try {
			if (dbType.toUpperCase().equalsIgnoreCase(DB_TYPE_ORACLE)) {
				Class.forName("oracle.jdbc.driver.OracleDriver");
				return true;
			} else if (dbType.toUpperCase().equalsIgnoreCase(DB_TYPE_SQLSERVER)) {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				return true;
			} else {
				logger.info("Invalid Database Type provided.  Unable to test connectivity.");
				return false;
			}
		} catch(ClassNotFoundException ce) {
			logger.info("Class Not Found Exception caught.\n" + ce.getMessage());
			return false;
		} catch (Exception e) {
			logger.info("Testing Connectivity caught Exception:\n" + e.getMessage());
			return false;
		}
	}
	
	public boolean createIBMonitorTable(Connection connection, String databaseName) {
		// Create the table.  If it already exists, move on.
		String tableCreate = "";
		if (dbType.toUpperCase().equals(DB_TYPE_ORACLE)) {
			tableCreate = IB_MONITOR_TABLE_CREATE.replaceAll("<CHAR_COLUMN>", CHAR_COLUMN_ORACLE).replaceAll("<NBR_COLUMN>", NBR_COLUMN_ORACLE);
		} else if (dbType.toUpperCase().equals(DB_TYPE_SQLSERVER)) {
			tableCreate = IB_MONITOR_TABLE_CREATE.replaceAll("<CHAR_COLUMN>", CHAR_COLUMN_SQLSERVER).replaceAll("<NBR_COLUMN>", NBR_COLUMN_SQLSERVER);			
		} else {
			logger.info("Invalid Database Type provided.  Unable to build UM_IB_MONITOR table.");
			return false;
		}

		try {
			DatabaseMetaData dbm = connection.getMetaData();
			// check if table is there
			ResultSet tables = dbm.getTables(null, null, "UM_IB_MONITOR", null);
			if (tables.next()) {
			  // Table exists
				logger.info("The UM_IB_MONITOR Table already exists in Database " + databaseName + ".");
			}
			else {
			  // Table does not exist
				logger.info("Creating the UM_IB_MONITOR Table for Database " + databaseName + ".");
				Statement stmt = connection.createStatement();
				stmt.execute(tableCreate);		
			}
			
			return true;
		} catch (SQLException e) {
			logger.info("Failed to create UM_IB_MONITOR Table. Error details are: " + e.toString());
			return false;
		}
	}

	public Connection openDBConnection(String host, String userName, String password) {
		Connection dbconn = null;

		try {
			if (dbType.toUpperCase().equalsIgnoreCase(DB_TYPE_ORACLE)) {
				dbconn = DriverManager.getConnection("jdbc:oracle:thin:@" + host, userName, password);				
			} else if (dbType.toUpperCase().equalsIgnoreCase(DB_TYPE_SQLSERVER)) {
				String sqlServerURL = "jdbc:sqlserver://<serverURL>;user=<dbUser>;Password=<dbPassword>";
				
				dbconn = DriverManager.getConnection(sqlServerURL.replace("<serverURL>", host)
						                                         .replace("<dbUser>", userName)
						                                         .replace("<dbPassword>", password));
			} else {
				logger.info("Invalid Database Type provided.  Unable to create connection.");				
			}
		} catch (SQLException se) {
			logger.info("Opening Connection caught SQL Exception:\n" + se.getMessage());
		} catch (Exception e) {
			logger.info("Opening Connection caught Exception:\n" + e.getMessage());
		}

		return dbconn;
	}

}
