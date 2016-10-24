package edu.umn.pssa.ibmonitorservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MonitorConnection {
	
	private static String dbType;
	
	public MonitorConnection() {
		
	}
	
	public MonitorConnection(String databaseType) {
		dbType = databaseType;
	}

	public boolean testConnectivity() {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			return true;
		} catch(ClassNotFoundException e) {
			return false;
		}
	}

	public Connection openDBConnection(String host, String userName, String password) {
		Connection dbconn = null;

		try {
			if (dbType.equalsIgnoreCase("ORACLE")) {
				dbconn = DriverManager.getConnection("jdbc:oracle:thin:@" + host, userName, password);				
			}
		} catch (SQLException e) {
			//logger.info("Opening Connection caught SQL Exception: " + e.getMessage());
		}

		return dbconn;
	}

}
