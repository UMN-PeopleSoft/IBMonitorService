package edu.umn.pssa.ibmonitorservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class Monitor extends Thread {
	
	// Logger
	private static final Logger logger = Logger.getLogger(IBMonitorSvc.class.getName());

	// Application Constants
	private static final String PUBLICATION_CONTRACT = "PubContract";
	private static final String SUBSCRIPTION_CONTRACT = "SubContract";
	private static final String MESSAGE_INSTANCE = "MessageInstance";
	private static final String DOMAIN_STATUS = "DomainStatus";
	private static final String NODE_DOWNTIME = "NodeDowntime";
	private static final String DEFAULT_PUBLICATION_MONITOR = "Default Publication Contracts";
	private static final String DEFAULT_SUBSCRIPTION_MONITOR = "Default Subscription Contracts";
	private static final String DEFAULT_MESSAGE_INSTANCE_MONITOR = "Default Message Instances";	
	private static final String MESSAGE_INSTANCE_THRESHOLD = "MsgInstanceThreshold";
	private static final String PUBLICATION_CONTRACT_THRESHOLD = "PubContractThreshold";
	private static final String SUBSCRIPTION_CONTRACT_THRESHOLD = "SubContractThreshold";
	private static final String MESSAGE_INSTANCE_AGING = "MsgInstanceAging";
	private static final String PUBLICATION_CONTRACT_AGING = "PubContractAging";
	private static final String SUBSCRIPTION_CONTRACT_AGING = "SubContractAging";
	private static final String MESSAGE_INSTANCE_RECORD = ".PSAPMSGPUBHDR";
	private static final String PUBLICATION_CONTRACT_RECORD = ".PSAPMSGPUBCON";
	private static final String SUBSCRIPTION_CONTRACT_RECORD = ".PSAPMSGSUBCON";
	private static final String MESSAGE_INSTANCE_STATUS_COLUMN = "PUBSTATUS";
	private static final String PUBLICATION_CONTRACT_STATUS_COLUMN = "PUBCONSTATUS";
	private static final String SUBSCRIPTION_CONTRACT_STATUS_COLUMN = "SUBCONSTATUS";
	private static final String SELECT_COUNT = "Select Count(*) From ";
	private static final String SELECT_MESSAGE_INSTANCES = "Select /*+ Index(PSAPMSGPUBHDR PSCPSAPMSGPUBHDR) */ IBTRANSACTIONID From ";
	private static final String SELECT_PUBLICATION_CONTRACTS = "Select /*+ Index(PSAPMSGPUBCON PSCPSAPMSGPUBCON) */ IBTRANSACTIONID From ";
	private static final String SELECT_SUBSCRIPTION_CONTRACTS = "Select /*+ Index(PSAPMSGSUBCON PSCPSAPMSGSUBCON) */ IBTRANSACTIONID From ";	
	private static final String SELECT_DOMAIN_STATUS = "Select count(*) From <SCHEMA>.PSAPMSGDOMSTAT Where DOMAIN_STATUS = 'A'";
	private static final String SELECT_EACH = "Select 'X' From UM_IB_MONITOR Where DATABASE_NAME = ':1' and EVENT_NAME = ':2' and EVENT_TYPE = ':3' and ESCALATION = :4 and IBTRANSACTIONID = ':5'";
	private static final String INSERT_TRANSACTION = "Insert Into UM_IB_MONITOR Values(':1', ':2', ':3', :4, ':5')";
	private static final String DELETE_TRANSACTIONS = "Delete From UM_IB_MONITOR Where DATABASE_NAME = ':1' and EVENT_NAME = ':2' and EVENT_TYPE = ':3' and IBTRANSACTIONID not in (:4)";
	private static final String INSERT_NODE_DOWN = "Insert Into <SCHEMA>.PSNODESDOWN Select :1, :2, :3, :4, :5, :6 From Dual Where Not Exists (Select 'X' From <SCHEMA>.PSNODESDOWN Where MSGNODENAME = :1 and TRXTYPE = :2 and IB_OPERATIONNAME = :3 and VERSIONNAME = :4)";
	private static final String REMOVE_NODE_DOWN = "Delete From <SCHEMA>.PSNODESDOWN Where MSGNODENAME = :1 and TRXTYPE = :2 and IB_OPERATIONNAME = :3 and VERSIONNAME = :4";
	private static final String ACTION_NOTIFY = "Notify";
	private static final String ACTION_CANCEL = "Cancel";
	private static final String ACTION_RESUBMIT = "Resubmit";
	private static final String ACTION_RETRY_REACT = "Retry/React";
	private static final String ACTION_NOTIFY_EACH = "Notify Each";
	private static final String ACTION_CUSTOM = "Custom";
	private static final String STATUS_OK = "";
	private static final String STATUS_ALERT = "ALERT";
	private static final String TYPE_MESSAGE = "Message";
	private static final String TYPE_THRESHOLD = "Threshold";
	private static final String TYPE_AGING = "Aging";
	private static final String TYPE_NODE_DOWN = "Node Downtime";
	private static final String WINDOW_ACTIVE = "Active";
	private static final String WINDOW_OFF_HOURS = "Off Hours";
	private static final String WINDOW_DOWN_TIME = "Down Time";
	private static final String DOMAIN_STATUS_NOTIFICATION_SUBJECT = "No Active IB Domains found in ";
	private static final String DOMAIN_STATUS_NOTIFICATION_BODY = "Monitoring did not find any active IB domains in <DATABASE>.  Please verify at least one domain is active.";
	private static final String CRLF = System.getProperty("line.separator");
	private static final String ON_CALL = "On Call";
	

	private MonitorConnection connection;
	private Vector<MonitorConfig> vMonitors;
	private Connection connMonitor;
	private Session emailSession;
	private int monitorId;
	private int downTimeFrequency = 0;
	private int defaultNotifyInterval = 0;
	private int defaultNotifyIntervalOffHours = 0;
	private Boolean domainStatusCheck = false;
	private Boolean hasNotifyEach = false;
	private long sleepTime = 0;
	private String databaseName = "";
	private String databaseHost = "";
	private String databasePassword = "";
	private String databaseUser = "";
	private String databaseSchema = "";
	private String defaultNotifyTo = "";
	private String defaultNotifyCC = "";
	private boolean monitoring = false;
	private Calendar downTimeStart = null;
	private Calendar downTimeEnd = null;
	private String defaultOffHoursTimeStart = "";
	private String defaultOffHoursTimeEnd = "";
	
	public Monitor() {
		
	}
	
	public Monitor(DatabaseType dbNode, int monitorID, boolean debugMode, String databaseType) {
		
		emailSession = createSession();
		
		monitorId = monitorID;
		vMonitors = new Vector<MonitorConfig>();
		
		// Get "Core" configurations
		databaseName = dbNode.getDatabaseName();
		databaseHost = dbNode.getHost();
		databaseUser = dbNode.getUser();
		databasePassword = dbNode.getPassword();
		databaseSchema = dbNode.getDbSchema();
		defaultNotifyTo = dbNode.getDefaultNotifyTo();
		defaultNotifyCC = dbNode.getDefaultNotifyCC();
		defaultNotifyInterval = dbNode.getDefaultNotifyInterval();
		if (defaultNotifyInterval == 0) {
			defaultNotifyInterval = 60;
		}
		
		domainStatusCheck = "ON".equalsIgnoreCase(dbNode.getDomainStatus());

		// Set Off Hours monitoring, if configured
		defaultOffHoursTimeStart = dbNode.getStartTimeOffHours();
		defaultOffHoursTimeEnd = dbNode.getEndTimeOffHours();
		defaultNotifyIntervalOffHours = dbNode.getDefaultNotifyIntervalOffHours();
		if (defaultNotifyIntervalOffHours == 0) {
			defaultNotifyIntervalOffHours = defaultNotifyInterval;
		}

		int tmpTime = dbNode.getSleepTime();
		if (tmpTime == 0) {
			sleepTime = 5 * 60 * 1000;
		} else {
			sleepTime = tmpTime * 60 * 1000;
		}

		vMonitors = populateConfigs(dbNode);
		
		// Check if we're in debug mode for this specific Database
		if (!debugMode) {
			if ("ON".equalsIgnoreCase(dbNode.getDebugMode())) {
				logger.info("Debug Mode turned on for database " + this.databaseName);
				debugMode = true;
			}
		}
		
		// Write SQL to a log file if in Debug Mode
		if (debugMode) {
			Logger debugLog = Logger.getLogger("ibmonitordebug");
			// Output SQL Statements
			for (int i = 0; i < vMonitors.size(); i++) {
				logger.info("logging Debug SQL");
				debugLog.debug("-- " + vMonitors.get(i).monitorName);
				debugLog.debug(vMonitors.get(i).sqlStatement);
				debugLog.debug(CRLF);
			}
		} else {
			// Check for connectivity
			connection = new MonitorConnection(getNodeValue(dbNode.dbType, databaseType));
			if (!connection.testConnectivity()){
				logger.info("Unable to find driver for database connectivity.  Exiting program.");
			} else {
				// Create the connection to verify connectivity
				connMonitor = connection.openDBConnection(databaseHost, databaseUser, databasePassword);
				
				if (connMonitor == null) {
					logger.info("Did not create connection for " + databaseName + " on host " + databaseHost);
		            IBMonitorSvc.terminateMonitor(monitorId);
					monitoring = false;
				} else {
					logger.info("Created connection for " + databaseName + " on host " + databaseHost);
					if (hasNotifyEach) {
						connection.createIBMonitorTable(connMonitor, databaseName);						
					}
				}
			}
		}
	}
	
 	public void run() {
 		startMonitor();
	}

	private void startMonitor() {

		Thread waitThread = new Thread();

		monitoring = true;

		while (monitoring) {
			try {
				synchronized (waitThread) {
					Thread.sleep(sleepTime);
					if (isDownTime()) {
						logger.info("Database " + databaseName + " will not be monitored during scheduled downtime. ");						
					} else {
						if (connMonitor == null) {
							connMonitor = connection.openDBConnection(databaseHost, databaseUser, databasePassword);						
						}

						// Write to ibAlerts.log file
						logger.info("Monitoring Database " + this.databaseName);
						
	                    if (connMonitor == null) {
	                       logger.info("Unable to create monitoring connection for " + databaseName);
	                    } else {
	                       // Execute Monitors
	                    	if (vMonitors.size() > 0) {
	 						   executeMonitors();
	                    	}
						}
						connMonitor.close();
						connMonitor = null;						
					}
				}
			} catch (Exception e) {
                logger.info("Monitoring in " + databaseName + " caught Exception: " + e.getMessage());				
			}
		}
	}

	// Method to populate the MonitorConfigs
	private Vector<MonitorConfig> populateConfigs(DatabaseType dbNode) {
		Vector<MonitorConfig> vConfigs;
		List<MonitorEventType> configNodes;
		MonitorEventType configNode;
		int index;
		int numConfigs = 0;
		int defaultTimeFrame;
		String defaultStatus;
		int defaultRetryCount ;
		String strStartTime = "";
		String strEndTime = "";
		String downTimeStartDay = "";
		String downTimeEndDay = "";
		
		vConfigs = new Vector<MonitorConfig>();
		configNodes = dbNode.getMonitorEvent();
		defaultStatus = getNodeValue(dbNode.getDefaultStatusToCheck(), "");
		defaultTimeFrame = getNodeValue(dbNode.getDefaultMonitorTime(), 0);
		defaultRetryCount = getNodeValue(dbNode.getDefaultRetryCount(), 0);
		
		downTimeStartDay = getNodeValue(dbNode.getDownTimeStartDay(), "");
		strStartTime = getNodeValue(dbNode.getDownTimeStart(), "");
		downTimeEndDay = getNodeValue(dbNode.getDownTimeEndDay(), "");
		strEndTime = getNodeValue(dbNode.getDownTimeEnd(), "");
		downTimeFrequency = getNodeValue(dbNode.getDownTimeFrequency(), 0);
		
		if (!strStartTime.equalsIgnoreCase("") && !strEndTime.equalsIgnoreCase("") && 
		    !downTimeStartDay.equalsIgnoreCase("") && !downTimeEndDay.equalsIgnoreCase("") &&
		    downTimeFrequency > 0 ) {
			setMonitorDownTime(downTimeStartDay, strStartTime, downTimeEndDay, strEndTime);
		}
		
		// Get each specific monitor to execute
		for (index = 0; index < configNodes.size(); index ++ ) {
			configNode = configNodes.get(index);

			// Populate Configurations
			MonitorConfig tmpMonitor = new MonitorConfig(configNode);
			
			// Store in the Vector
			vConfigs.add(index, tmpMonitor);
			numConfigs = index;
		}
		
		if (numConfigs != 0) {
			numConfigs = numConfigs + 1;
		}

		// Populate 3 more entries in vConfigs for default monitors (Pubs/Subs/Instances)
		vConfigs.add(numConfigs, populateDefaultConfig(MESSAGE_INSTANCE, 
				     generateDefaultMonitorSQL(MESSAGE_INSTANCE_STATUS_COLUMN, defaultStatus, defaultTimeFrame, defaultRetryCount, vConfigs)));
		vConfigs.add(numConfigs + 1, populateDefaultConfig(PUBLICATION_CONTRACT, 
				     generateDefaultMonitorSQL(PUBLICATION_CONTRACT_STATUS_COLUMN, defaultStatus, defaultTimeFrame, defaultRetryCount, vConfigs)));
		vConfigs.add(numConfigs + 2, populateDefaultConfig(SUBSCRIPTION_CONTRACT,
				     generateDefaultMonitorSQL(SUBSCRIPTION_CONTRACT_STATUS_COLUMN, defaultStatus, defaultTimeFrame, defaultRetryCount, vConfigs)));
		
		if (domainStatusCheck) {
			vConfigs.add(numConfigs + 3, populateDomainStatusConfig());
		}
		
		return vConfigs;
	}
	
	private String generateMonitorSQL(MonitorConfig monitorConfigs) {
		String sqlString = "";
		String statusColumn = "";
		
        if (monitorConfigs.operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT)) {
            sqlString = SELECT_SUBSCRIPTION_CONTRACTS + databaseSchema + SUBSCRIPTION_CONTRACT_RECORD;
            statusColumn = SUBSCRIPTION_CONTRACT_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(PUBLICATION_CONTRACT)) {
            sqlString = SELECT_PUBLICATION_CONTRACTS + databaseSchema + PUBLICATION_CONTRACT_RECORD;
            statusColumn = PUBLICATION_CONTRACT_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(MESSAGE_INSTANCE)) {
            sqlString = SELECT_MESSAGE_INSTANCES + databaseSchema + MESSAGE_INSTANCE_RECORD;
            statusColumn = MESSAGE_INSTANCE_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(MESSAGE_INSTANCE_THRESHOLD)) {
            sqlString = SELECT_COUNT + databaseSchema + MESSAGE_INSTANCE_RECORD;
            statusColumn = MESSAGE_INSTANCE_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(PUBLICATION_CONTRACT_THRESHOLD)) {
            sqlString = SELECT_COUNT + databaseSchema + PUBLICATION_CONTRACT_RECORD;
            statusColumn = PUBLICATION_CONTRACT_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT_THRESHOLD)) {
            sqlString = SELECT_COUNT + databaseSchema + SUBSCRIPTION_CONTRACT_RECORD;
            statusColumn = SUBSCRIPTION_CONTRACT_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(MESSAGE_INSTANCE_AGING)) {
            sqlString = SELECT_COUNT + databaseSchema + MESSAGE_INSTANCE_RECORD;
            statusColumn = MESSAGE_INSTANCE_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(PUBLICATION_CONTRACT_AGING)) {
            sqlString = SELECT_COUNT + databaseSchema + PUBLICATION_CONTRACT_RECORD;
            statusColumn = PUBLICATION_CONTRACT_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT_AGING)) {
            sqlString = SELECT_COUNT + databaseSchema + SUBSCRIPTION_CONTRACT_RECORD;
            statusColumn = SUBSCRIPTION_CONTRACT_STATUS_COLUMN;
        } else if (monitorConfigs.operationType.equalsIgnoreCase(NODE_DOWNTIME)) {
            return generateNodeDownSQL(monitorConfigs);
        } else {
        	return "";
        }
        
        // Build Where Clause
        sqlString = sqlString + " Where " + statusColumn + " in (" + monitorConfigs.status + ")";

        // Add any Service Operation Conditions
        sqlString = sqlString + createCondition("and", "IB_OPERATIONNAME", monitorConfigs.serviceOperation, "in");
        
        // Add any Service Operation EXCLUDE Conditions
        sqlString = sqlString + createCondition("and", "IB_OPERATIONNAME", monitorConfigs.serviceOperationExclude, "not in");

        // Add any Publication Node Conditions
        sqlString = sqlString + createCondition("and", "PUBNODE", monitorConfigs.pubNode, "in");

        // Add condition for Subscribing Node: Only for Publication Contracts
        if (monitorConfigs.operationType.equalsIgnoreCase(PUBLICATION_CONTRACT)) {
            sqlString = sqlString + createCondition("and", "SUBNODE", monitorConfigs.subNode, "in"); 
        }
        
        // Add condition for time period to check
        if (monitorConfigs.timeToCheck > 0) {
        	sqlString = sqlString + " and LASTUPDDTTM > (Sysdate - (" + monitorConfigs.timeToCheck + "/1440))";
        }
        
        // If this is an Aging Monitor, add the necessary check on time
        if (monitorConfigs.monitorType.equals(TYPE_AGING)) {
        	sqlString = sqlString + " and LASTUPDDTTM < (Sysdate - (" + monitorConfigs.age + "/1440))";
        }
        
        return sqlString;
	}
	
	private String createCondition(String prefix, String columnName, String nodeValue, String condition) {
        if (nodeValue.equalsIgnoreCase("")) {
        	return "";
        } else {
        	String values = "";
        	String strPrefix = "";
            // Parse the list of values
            String[] array = nodeValue.split(",");
            for (int index = 0; index < array.length ; index ++ ) {
            	values = values + strPrefix + "'" + array[index].trim() + "'";
            	strPrefix = ",";
            }
            return " " + prefix + " " + columnName + " " + condition + " (" + values + ")";
        }		
	}

	private String generateDefaultMonitorSQL(String statusColumn, String status, int timeFrame, int retryCount, Vector<MonitorConfig> vMonitorConfigs) {
		String sqlInClause = "";
		String unionString = "";
		String whereClause = "";

		//Create the UNION ALL of all other checks:
		unionString = "";
		for (int i=0; i < vMonitorConfigs.size(); i++) {
			if (vMonitorConfigs.get(i).monitorType.equalsIgnoreCase(TYPE_MESSAGE)) {
			   sqlInClause = sqlInClause + unionString + vMonitorConfigs.get(i).sqlStatement;
			   unionString = " Union All ";
			}
		}
		
        // Build Where Clause
        whereClause = " Where " + statusColumn + " in (" + status + ") and IBTRANSACTIONID not in (" + sqlInClause + ")";

        if (retryCount > 0) {
        	// Add time check condition to the Where clause to check X minutes
        	whereClause = whereClause + " and RETRYCOUNT > " + retryCount;
        }

        if (timeFrame > 0) {
        	// Add time check condition to the Where clause to check X minutes
        	whereClause = whereClause + " and LASTUPDDTTM > (Sysdate - (" + timeFrame + "/1440))";
        }
        
        return whereClause;
	}
	
	private String generateNodeDownSQL(MonitorConfig monitorConfigs) {
		String sqlString = INSERT_NODE_DOWN.replaceAll("<SCHEMA>", databaseSchema);
		
		// MSGNODENAME
		sqlString = sqlString.replaceAll(":1", generateReplacementString(monitorConfigs.subNode));
		
		// TRXTYPE: Always Outbound Asynchronous (OA)
		sqlString = sqlString.replaceAll(":2", "'OA'");
		
		// IB_OPERATIONNAME
		sqlString = sqlString.replaceAll(":3", generateReplacementString(monitorConfigs.serviceOperation));
		
		// VERSIONNAME
		sqlString = sqlString.replaceAll(":4", "' '");
		
		// EXTOPERATIONNAME
		sqlString = sqlString.replaceAll(":5", "' '");
		
		// ROUTINGDEFNNAME
		sqlString = sqlString.replaceAll(":6", "' '");
				
		return sqlString;
	}
	
	private String generateNodeDownRemovalSQL(MonitorConfig monitorConfigs) {
		String sqlString = REMOVE_NODE_DOWN.replaceAll("<SCHEMA>", databaseSchema);

		// MSGNODENAME
		sqlString = sqlString.replaceAll(":1", generateReplacementString(monitorConfigs.subNode));
		
		// TRXTYPE: Always Outbound Asynchronous (OA)
		sqlString = sqlString.replaceAll(":2", "'OA'");
		
		// IB_OPERATIONNAME
		sqlString = sqlString.replaceAll(":3", generateReplacementString(monitorConfigs.serviceOperation));
		
		// VERSIONNAME
		sqlString = sqlString.replaceAll(":4", "' '");

		return sqlString;
	}
	
	private String generateReplacementString(String value) {
		if (value.equals("") || value == null) {
			value = " ";
		}

		return "'" + value + "'";

	}
	
	private MonitorConfig populateDefaultConfig(String opType, String whereClause) {
		MonitorConfig configs = new MonitorConfig();
		String sqlString = "";
		
        if (opType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT)) {
            sqlString = SELECT_SUBSCRIPTION_CONTRACTS + databaseSchema + SUBSCRIPTION_CONTRACT_RECORD;
            configs.monitorName = DEFAULT_SUBSCRIPTION_MONITOR;
        } else if (opType.equalsIgnoreCase(PUBLICATION_CONTRACT)) {
            sqlString = SELECT_PUBLICATION_CONTRACTS + databaseSchema + PUBLICATION_CONTRACT_RECORD;
            configs.monitorName = DEFAULT_PUBLICATION_MONITOR;
        } else if (opType.equalsIgnoreCase(MESSAGE_INSTANCE)) {
            sqlString = SELECT_MESSAGE_INSTANCES + databaseSchema + MESSAGE_INSTANCE_RECORD;
            configs.monitorName = DEFAULT_MESSAGE_INSTANCE_MONITOR;
        }
        
        sqlString = sqlString + whereClause;
		
		configs.operationType = opType;
		configs.monitorType = TYPE_MESSAGE;
		configs.serviceOperation = "";
		configs.pubNode = "";
		configs.subNode = "";
		configs.status = "";
		configs.sqlStatement = sqlString;  //SQL to execute
		configs.action = "";     //Action to be performed
		configs.reaction = "";
		if (defaultNotifyTo.equals("") && defaultNotifyCC.equals("")) {
			configs.notificationFlag = false; //Notification Flag
		} else {
			configs.notificationFlag = true; //Notification Flag
		}
		configs.retryCount = 0; //# of retry attempts
		configs.alertStatus = STATUS_OK;   //NOT currently in Alert State
		configs.alertSubject = configs.monitorName + " issues found in " + databaseName + ".";
		configs.alertText = configs.monitorName + " issues found in " + databaseName + ".";
		configs.notifyTo = defaultNotifyTo;
		configs.notifyCC = defaultNotifyCC;
		configs.notificationInterval = defaultNotifyInterval; // Every hour
		configs.lastNotification = null;

		// Set the Off Hours monitoring, if configured
		if (!defaultOffHoursTimeStart.equalsIgnoreCase("") && !defaultOffHoursTimeEnd.equalsIgnoreCase("")) {
			setMonitorHours(configs, defaultOffHoursTimeStart, defaultOffHoursTimeEnd, WINDOW_OFF_HOURS);
		}
		configs.notificationIntervalOffHours = defaultNotifyIntervalOffHours; // 0 unless configured

		return configs;
	}

	private MonitorConfig populateDomainStatusConfig() {
		MonitorConfig configs = new MonitorConfig();
		String sqlString = SELECT_DOMAIN_STATUS.replace("<SCHEMA>", databaseSchema);
		
		configs.monitorType = DOMAIN_STATUS;
		configs.sqlStatement = sqlString;  //SQL to execute
		configs.action = "";     //Action to be performed
		configs.reaction = "";
		if (defaultNotifyTo.equals("") && defaultNotifyCC.equals("")) {
			configs.notificationFlag = false; //Notification Flag
		} else {
			configs.notificationFlag = true; //Notification Flag
		}
		configs.retryCount = 0; //# of retry attempts
		configs.alertStatus = STATUS_OK;   //NOT currently in Alert State
		configs.alertSubject = DOMAIN_STATUS_NOTIFICATION_SUBJECT + databaseName + ".";
		configs.alertText = DOMAIN_STATUS_NOTIFICATION_BODY.replace("<DATABASE>",  databaseName);
		configs.notifyTo = defaultNotifyTo;
		configs.notifyCC = defaultNotifyCC;
		configs.notificationInterval = defaultNotifyInterval; // Every hour
		configs.lastNotification = null;

		// Set the Off Hours monitoring, if configured
		if (!defaultOffHoursTimeStart.equalsIgnoreCase("") && !defaultOffHoursTimeEnd.equalsIgnoreCase("")) {
			setMonitorHours(configs, defaultOffHoursTimeStart, defaultOffHoursTimeEnd, WINDOW_OFF_HOURS);
		}
		configs.notificationIntervalOffHours = defaultNotifyIntervalOffHours; // 0 unless configured

		return configs;
	}

	// Method to execute each of the configured Monitor activities 
	private void executeMonitors() {

		for (int monitorIndex = 0; monitorIndex < vMonitors.size(); monitorIndex++) {
			MonitorConfig monitorConfigs = vMonitors.get(monitorIndex);
			
			if (isInWindow(WINDOW_ACTIVE, monitorConfigs, monitorConfigs.startTime, monitorConfigs.endTime)) {
				processMonitorResults(monitorConfigs);
			} else {
				// Reset lastNotification if not already null
				if (!(monitorConfigs.lastNotification == null)) {
				   monitorConfigs.lastNotification = null;
				}
				
				// Additional Node Down Logic:
				if (monitorConfigs.monitorType.equalsIgnoreCase(TYPE_NODE_DOWN) && monitorConfigs.alertStatus.equalsIgnoreCase(STATUS_ALERT)) {
					// Node should no longer be "Down"
					removeNodeDown(monitorConfigs);					
				}
			}
		}
	}
	
	// Method to process the Monitor results
	private void processMonitorResults(MonitorConfig monitorConfigs) {
		String transactionID = "";
		String notificationMessage = "";
		int transCount = 0;
		int notifyInterval = 0;
		Boolean resultsFound = false;
		Boolean reaction = false;

		try {
	        Statement stmt = connMonitor.createStatement();
	        
	        if (monitorConfigs.monitorType.equalsIgnoreCase(TYPE_NODE_DOWN)) {
		        stmt.executeUpdate(monitorConfigs.sqlStatement);
		        monitorConfigs.alertStatus = STATUS_ALERT;
	        } else {
		        ResultSet rs = stmt.executeQuery(monitorConfigs.sqlStatement);				        	
		        while (rs.next()) {
	                // For Transaction Monitors	
		        	
					if (monitorConfigs.monitorType.equalsIgnoreCase(TYPE_MESSAGE)) {
		        	   resultsFound = true;
		        	   notificationMessage = monitorConfigs.alertText;
		        
		        	   // Get the Transaction ID
		        	   transactionID = rs.getString(1);
		        	   
		        	   // Check if previously processed?
		        	   // Take action if configured
		        	   if (monitorConfigs.action.equalsIgnoreCase(ACTION_CUSTOM)) {
		        		   // Fire the Custom Action
		        		   monitorConfigs.customAction.fireAction(transactionID);		        		   
		        	   } else if (monitorConfigs.action.equalsIgnoreCase(ACTION_RETRY_REACT)) {
		        		   // First: Override resultsFound - we may or may not have to notify yet...
		        		   resultsFound = false;
		        		   // Second: Perform Retry/React logic.  Set reaction to true accordingly.
		        		   if (monitorConfigs.retryReact(transactionID)) {
		        			   reaction = true;
		        		   }
		        	   } else if (monitorConfigs.action.equalsIgnoreCase(ACTION_NOTIFY_EACH)) {
		        		   //TO DO: Need new logic.
		        		   notifyEach(monitorConfigs, transactionID);
		        		   
		        	   } else if (!monitorConfigs.action.equalsIgnoreCase("") && !monitorConfigs.action.equalsIgnoreCase(ACTION_NOTIFY)) {
		        		  executeUpdate(transactionID, monitorConfigs.operationType, monitorConfigs.action, databaseName, connMonitor);
		        		  logger.info("executeUpdate fired on Transaction ID: " + transactionID + " in " + databaseName + ": Retry Count = " + monitorConfigs.retryCount);
		        	   } else if (resultsFound){
		        		   break;  //Since we are not taking an action other than notify, we don't need to keep looping
		        	   }
		        	} else if (monitorConfigs.monitorType.equalsIgnoreCase(TYPE_THRESHOLD)) {
	                   // Get the Count
			           transCount = Integer.valueOf(rs.getString(1));
			           if (transCount > monitorConfigs.threshold) {
		   	        	  resultsFound = true;  
		   	        	  notificationMessage = monitorConfigs.alertText.replaceAll(":1", String.valueOf(transCount));
	                   }
		        	} else if (monitorConfigs.monitorType.equalsIgnoreCase(TYPE_AGING)) {
	                   // Get the Count
		        		transCount = Integer.valueOf(rs.getString(1));
		        		if (transCount > monitorConfigs.threshold) {
		        			resultsFound = true;  
		   	        	    notificationMessage = monitorConfigs.alertText.replaceAll(":1", String.valueOf(transCount));
	                    }
		        	} else if (monitorConfigs.monitorType.equalsIgnoreCase(DOMAIN_STATUS)) {
		        		transCount = Integer.valueOf(rs.getString(1));
		        		if (transCount == 0) {
		        			resultsFound = true;  
		   	        	    notificationMessage = monitorConfigs.alertText;
	                    }	        		
		        	}
		        }
	        }

	        if (monitorConfigs.action.equalsIgnoreCase(ACTION_NOTIFY_EACH) || monitorConfigs.reaction.equalsIgnoreCase(ACTION_NOTIFY_EACH)) {
	        	// Remove any Transaction IDs from UM_IB_MONITOR no longer found to be an issue
	        	stmt.execute(DELETE_TRANSACTIONS.replace(":1",  databaseName)
	        			                        .replace(":2", monitorConfigs.monitorName)
	        			                        .replace(":3", monitorConfigs.monitorType)
	        			                        .replace(":4", monitorConfigs.sqlStatement));
	        }
		} catch (SQLException s) {
			logger.info("Processing caught SQLException in " + databaseName + ": " + s.getMessage() + CRLF +
					    "SQL Statement is: " + monitorConfigs.sqlStatement);
		} catch (Exception e) {
			logger.info("Processing caught Exception in " + databaseName + ": " + e.toString() + CRLF + monitorConfigs.monitorName);
		}
		
		// Check if "reaction" is triggered: If so, set ResultsFound = true as well.
		if (reaction) {
			resultsFound = true;
		}
		
		if (resultsFound) {
			// Log in the current log file:
			logger.info(notificationMessage);
			
			// Determine if we're "Off Hours", and use the right interval
			if (isInWindow(WINDOW_OFF_HOURS, monitorConfigs, monitorConfigs.startTimeOffHours, monitorConfigs.endTimeOffHours)) {
				notifyInterval = monitorConfigs.notificationIntervalOffHours;
			} else {
				notifyInterval = monitorConfigs.notificationInterval;
			}
			
			// Notify if so configured
			if (monitorConfigs.notificationFlag == true) {
				//Send Notification if:
				// Last Notify Time is blank, or 
				// Current Time is greater than Last Notify Time + Notification Interval
				if (monitorConfigs.lastNotification == null) {
					prepareNotification(monitorConfigs, notificationMessage);
				} else if (notifyInterval > 0){
					Calendar rightNow = Calendar.getInstance();
					Calendar nextNotify = null;
					nextNotify = (Calendar)monitorConfigs.lastNotification.clone();
					nextNotify.add(Calendar.MINUTE, notifyInterval);

					if (rightNow.after(nextNotify)) {
						prepareNotification(monitorConfigs, notificationMessage);
					} else {
						// Nothing yet...
                    }			
				}
			}
			
			// Check for any necessary Escalations
			prepareEscalations(monitorConfigs, notificationMessage);

		} else if (!monitorConfigs.monitorType.equalsIgnoreCase(TYPE_NODE_DOWN)) {
			monitorConfigs.lastNotification = null;
			monitorConfigs.alertStatus = STATUS_OK;
			if (monitorConfigs.escalations == null) {
				//Do Nothing
			} else {
				// Clear out Escalations too
				for (int i=0; i < monitorConfigs.escalations.size(); i++) {
					monitorConfigs.escalations.elementAt(i).notificationSent = false;
					monitorConfigs.escalations.elementAt(i).issueDetected = null;
					monitorConfigs.escalations.elementAt(i).lastNotification = null;
				}				
			}
		}

	}
	
	private void removeNodeDown(MonitorConfig monitorConfigs) {
		// Set the Alert Status to OK
		monitorConfigs.alertStatus = STATUS_OK;
		
		// Remove the row from PSNODESDOWN
		try {
	        Statement stmt = connMonitor.createStatement();
	        stmt.executeUpdate(generateNodeDownRemovalSQL(monitorConfigs));

	        stmt.close();
		} catch (SQLException s) {
			logger.info("Processing Node Down caught SQLException in " + databaseName + ": " + s.getMessage());
		} catch (Exception e) {
			logger.info("Processing Node Down caught Exception in " + databaseName + ": " + e.getMessage());
		}
		
	}

	private Session createSession() {
		Properties props = new Properties();
		if (IBMonitorSvc.emailUser.isEmpty() || IBMonitorSvc.emailUser.equals("")) {
			props.setProperty("mail.smtp.host", IBMonitorSvc.emailHost);
			props.setProperty("mail.smtp.port", String.valueOf(IBMonitorSvc.emailPort));
			props.setProperty("mail.smtp.auth", "false");
			props.setProperty("mail.transport.protocol", "smtp");
			if (IBMonitorSvc.emailPort == 25) {
				props.setProperty("mail.smtp.starttls.enable","false"); //Important: must start TLS
			} else {
				props.setProperty("mail.smtp.starttls.enable","true"); //Important: must start TLS
			}
		} else {
			props.setProperty("mail.smtp.host", IBMonitorSvc.emailHost);
			props.setProperty("mail.smtp.port", String.valueOf(IBMonitorSvc.emailPort));
			props.setProperty("mail.smtp.auth", "true");
			props.setProperty("mail.transport.protocol", "smtp");
			props.setProperty("mail.smtp.starttls.enable","true"); //Important: must start TLS
		}

		Authenticator auth = new SMTPAuthenticator();
		Session session = Session.getInstance(props, auth);
		session.setDebug(false);
		
		return session;

	}
	
	// Method to send a notification to configured parties.
	private void sendNotification(String sendTo, String sendCC, String subject, String message) {		
		try {
		    // create a message
		    Message msg = new MimeMessage(emailSession);

		    // set the from and to address
		    InternetAddress addressFrom = new InternetAddress(IBMonitorSvc.emailReplyTo);
		    msg.setFrom(addressFrom);

		    if (!sendTo.equals("")){
		    	InternetAddress[] addressTo = parseRecipients(sendTo); 
			    msg.setRecipients(Message.RecipientType.TO, addressTo);
		    }
		   
		    if (!sendCC.equals("")){
		    	InternetAddress[] addressCC = parseRecipients(sendCC); 
			    msg.setRecipients(Message.RecipientType.CC, addressCC);
		    }

		    // Setting the Subject and Content Type
		    msg.setSubject(subject);
		    message = message + CRLF + CRLF +  "This is an automated email.  Please do not reply to sender.";
		    msg.setContent(message, "text/plain");

		    Transport.send(msg);
		} catch(MessagingException me) {
			logger.info("Error sending secure email message using password auth - " + me.getMessage());
		} catch (Exception e) {
			logger.info("Exception caught when attempting to send email: " + e.getMessage());
		}
	}
	
	private InternetAddress[] parseRecipients (String recipientList) {
		String[] tempArray;
		InternetAddress[] recipientArray;
		
		try {
			
			if (recipientList.contains(ON_CALL)) {
				//replace with list of email addresses/pagers to notify
				recipientList = updateRecipientsWithOnCall(recipientList);
			}
	        tempArray = recipientList.split(";");
	        recipientArray = new InternetAddress[tempArray.length];
	        
	        for (int index = 0; index < tempArray.length ; index ++ ) {
            	recipientArray[index] = new InternetAddress(tempArray[index]);	        		
	        }
	        
	        return recipientArray;

		} catch(MessagingException me) {
			logger.info("Error setting recipientArray values - " + me.getMessage());
		} 
		
		return null;
 	}
	
	public String updateRecipientsWithOnCall(String originalRecipients) {
		String newRecipients = "";
		String onCallRecipients = "";
		// Open the On Call File, read any items not commented out (with #)
		File onCallFile = new File(IBMonitorSvc.onCallFileName);

	    try {
			BufferedReader input = new BufferedReader(new FileReader(onCallFile));
			try {
				String line = null; // not declared within while loop
				String prefix = "";
				while ((line = input.readLine()) != null) {
					// Check if valid line (does not start with #
					if (!line.startsWith("#")) {
						try {
							if (!line.split(":")[1].equals("") || !(line.split(":") == null)) {
								onCallRecipients = onCallRecipients + prefix + line.split(":")[1];
								prefix = ";";
							}
						} catch (Exception e) {
							// Nothing, just move to the next one
						}
					}
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			logger.info("On Call File not found (or other file error.)");
			return originalRecipients.replaceAll(ON_CALL, "");
		}

		newRecipients = originalRecipients.replaceAll(ON_CALL, onCallRecipients);
		return newRecipients;
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(IBMonitorSvc.emailUser, IBMonitorSvc.emailPassword);
		}
	}

	// Method to send notification
	private void prepareNotification(MonitorConfig monitorConfigs, String message) {
		sendNotification(monitorConfigs.notifyTo, monitorConfigs.notifyCC, monitorConfigs.alertSubject, message);
		String recipients = monitorConfigs.notifyTo + ";" + monitorConfigs.notifyCC;

		monitorConfigs.alertStatus = STATUS_ALERT;
		if (recipients.contains(ON_CALL)) {
			//replace with list of email/pagers to notify
			recipients = updateRecipientsWithOnCall(recipients);
		}
		
		logger.info("   " + monitorConfigs.alertStatus + " Sent for " + monitorConfigs.alertSubject + " to " + recipients);
		monitorConfigs.lastNotification = Calendar.getInstance();
		
	}
	
	private void prepareEscalations(MonitorConfig monitorConfigs, String message) {
		Calendar now = Calendar.getInstance();
		// Notify any Escalations
		if (monitorConfigs.escalations == null) {
			// Do nothing
		} else {
			for (int i=0; i < monitorConfigs.escalations.size(); i ++) {
				Calendar nextNotify = null;
				EscalationConfig escalation = monitorConfigs.escalations.elementAt(i);
				// Set the Date/Time the issue was detected if it is null.
				if (escalation.issueDetected == null) {
					escalation.issueDetected = now;
				}
				
				// Find time an Escalation should fire
				if (!escalation.notificationSent) {
					nextNotify = (Calendar)escalation.issueDetected.clone();
					nextNotify.add(Calendar.MINUTE, escalation.escalationDelay);					
				} else if (escalation.notificationInterval > 0){
					nextNotify = (Calendar)escalation.lastNotification.clone();
					nextNotify.add(Calendar.MINUTE, escalation.notificationInterval);
				}
				
				// Check if monitor should fire
				if (nextNotify != null && nextNotify.compareTo(now) <=0 ) {
					sendNotification(escalation.email, "", monitorConfigs.alertSubject, message + CRLF + escalation.emailText);
					escalation.notificationSent = true;
					escalation.lastNotification = now;
					String recipients = monitorConfigs.notifyTo + ";" + monitorConfigs.notifyCC;
					logger.info("   ESCALATION: " + monitorConfigs.alertStatus + " Sent for " + monitorConfigs.alertSubject + " to " + recipients);
				}
			}			
		}
	}
	
	private void prepareEscalationsEach(MonitorConfig monitorConfigs, String message, String transactionID) {
		Calendar now = Calendar.getInstance();
		// Notify any Escalations
		if (monitorConfigs.escalations == null) {
			// Do nothing
		} else {
			for (int i=0; i < monitorConfigs.escalations.size(); i ++) {
				EscalationConfig escalation = monitorConfigs.escalations.elementAt(i);
				if (checkPreviouslyNotified(monitorConfigs.monitorName, monitorConfigs.monitorType, transactionID, i + 1)) {
					Calendar nextNotify = null;
					// Set the Date/Time the issue was detected - even if not null for Notify Each
					escalation.issueDetected = now;
					nextNotify = (Calendar)escalation.issueDetected.clone();
					nextNotify.add(Calendar.MINUTE, escalation.escalationDelay);					

					// Check if monitor should fire
					if (nextNotify != null && nextNotify.compareTo(now) <=0 ) {
						String subject = monitorConfigs.alertSubject + " Transaction ID: " + transactionID;
						sendNotification(escalation.email, "", subject, message + CRLF + 
								escalation.emailText.replaceAll("TRANSACTIONID", transactionID));
						escalation.notificationSent = true;
						escalation.lastNotification = now;
						String recipients = monitorConfigs.notifyTo + ";" + monitorConfigs.notifyCC;
						logger.info("   ESCALATION (EACH): " + monitorConfigs.alertStatus + " Sent for " + subject + " to " + recipients);
					}
				} else {
					escalation.notificationSent = true;
					escalation.lastNotification = now;
				}
			}			
		}
	}
	
	private void notifyEach(MonitorConfig configs, String transactionID) {
		int notifyInterval = 0;
		// Determine if we're "Off Hours", and use the right interval
		if (isInWindow(WINDOW_OFF_HOURS, configs, configs.startTimeOffHours, configs.endTimeOffHours)) {
			notifyInterval = configs.notificationIntervalOffHours;
		} else {
			notifyInterval = configs.notificationInterval;
		}

		String msg = configs.alertText + " Transaction ID: " + transactionID;
		if (checkPreviouslyNotified(configs.monitorName, configs.monitorType, transactionID, 0)) {
			prepareNotification(configs, msg);
		} else if (notifyInterval > 0){
			if (configs.lastNotification == null) {
				configs.lastNotification = Calendar.getInstance();
			}
			Calendar rightNow = Calendar.getInstance();
			Calendar nextNotify = null;
			nextNotify = (Calendar)configs.lastNotification.clone();
			nextNotify.add(Calendar.MINUTE, notifyInterval);

			if (rightNow.after(nextNotify)) {
				prepareNotification(configs, configs.alertText);
            }		
		}
		prepareEscalationsEach(configs, msg, transactionID);

	}
	
	private boolean checkPreviouslyNotified(String eventName, String eventType, String transactionID, int escalation) {
		Connection conn;
		conn =  connection.openDBConnection(databaseHost, databaseUser, databasePassword);
		
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SELECT_EACH.replace(":1", databaseName)
					                                    .replace(":2", eventName)
					                                    .replace(":3", eventType)
					                                    .replace(":4", String.valueOf(escalation))
					                                    .replace(":5", transactionID));
			if (rs.next()) {
                if (rs.getString(1).equalsIgnoreCase("X")) {
        			stmt.close();
                	return false;
                }
			}

			// If the data has not been added, insert the row, but return false.
			insertTransaction(conn, eventName, eventType, transactionID, escalation);
			stmt.close();
		} catch (SQLException s) {
			logger.info("SQLException encountered in " + databaseName + ": " + s.getMessage());
		}
		
		return true;
	}
	
	private void insertTransaction(Connection conn, String eventName, String eventType, String transactionID, int escalation) {
		try {
			// If the data has not been added, insert the row, but return false.
			Statement stmt = conn.createStatement();
			stmt.execute(INSERT_TRANSACTION.replace(":1", databaseName)
					                       .replace(":2", eventName)
					                       .replace(":3", eventType)
					                       .replace(":4", String.valueOf(escalation))
					                       .replace(":5", transactionID));

			stmt.close();
		} catch (SQLException s) {
			logger.info("SQLException encountered in " + databaseName + ": " + s.getMessage());
		}
	}

	/* How to update Messages:
	 * Status	Status String
	 *   0		ERROR
	 *   1		NEW
	 *   2		START
	 *   3 		WRKNG
	 *   4		DONE
	 *   5		RETRY
	 *   6		TIME
	 *   7		EDIT (?)
	 *   8		CNCLD
	 *   9		HOLD (?)
	 *   
	 * Publication Contract Record/Field: PSAPMSGPUBCON/PUBCONSTATUS
	 * Subscription Contract Record/Field: PSAPMSGSUBCON/SUBCONSTATUS
	 * Message Instance Record/Field: PSAPMSGPUBHDR/PUBSTATUS
	 */
	private void executeUpdate(String transactionID, String transactionType, String updAction, String databaseName, Connection connMonitor) {
		String sqlUpdate = "";
		String statusString = "";
		String statusField = "";
		String sqlRecord = "";
		int statusVal = 0;
		
		//Determine the Record/Field to update
		if (transactionType.equalsIgnoreCase(PUBLICATION_CONTRACT)) {
			sqlRecord = "PSAPMSGPUBCON";
			statusField = "PUBCONSTATUS";
		} else if (transactionType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT)) {
			sqlRecord = "PSAPMSGSUBCON";
			statusField = "SUBCONSTATUS";
		} else if (transactionType.equalsIgnoreCase(MESSAGE_INSTANCE)) {
			sqlRecord = "PSAPMSGPUBHDR";
			statusField = "PUBSTATUS";
		}
		
		// Set the Status Field and Status String
		if (updAction.equalsIgnoreCase(ACTION_CANCEL)) {
			statusVal = 8;
			statusString = "CNCLD";
		} else if (updAction.equalsIgnoreCase(ACTION_RESUBMIT)) {
			statusVal = 1;
			statusString = "NEW";
		} else {
			return;
		}
		
		// Build the SQL Update
		if (!sqlRecord.equalsIgnoreCase("") && !statusString.equalsIgnoreCase("")) {
			sqlUpdate = "Update " + databaseSchema + "." + sqlRecord;
			sqlUpdate = sqlUpdate + " Set " + statusField + " = " + statusVal + ", STATUSSTRING = '" + statusString + "'";
			sqlUpdate = sqlUpdate + " Where IBTRANSACTIONID = '" + transactionID + "'";
			
			try {
				Statement stmt = connMonitor.createStatement();
				stmt.executeUpdate(sqlUpdate);	
				stmt.close();
			} catch (SQLException s) {
				logger.info("SQLException encountered in " + databaseName + ": " + s.getMessage());
			}	
		} else {
			logger.info("Unable to update Transaction ID: " + transactionID + " in " + databaseName + ".");
		}

	}
	
	private void setMonitorDownTime(String tmpStartDay, String tmpStartTime, String tmpEndDay, String tmpEndTime) {
		String [] startTimes;
		String [] endTimes;

		Calendar today = Calendar.getInstance();
		int currentDayofWeek = today.get(Calendar.DAY_OF_WEEK);
		int currentDay = today.get(Calendar.DAY_OF_MONTH);
		int currentMonth = today.get(Calendar.MONTH);
		int currentYear = today.get(Calendar.YEAR);

        startTimes = tmpStartTime.split(":");
        endTimes = tmpEndTime.split(":");

        downTimeStart = Calendar.getInstance();
        downTimeEnd = Calendar.getInstance();

        downTimeStart.set(currentYear, currentMonth, currentDay, 
        		          Integer.parseInt(startTimes[0]), Integer.parseInt(startTimes[1]));
        downTimeEnd.set(currentYear, currentMonth, currentDay, 
        		        Integer.parseInt(endTimes[0]), Integer.parseInt(endTimes[1]));

       	// Shift the date accordingly
       	downTimeStart.add(Calendar.DATE, (Integer.parseInt(tmpStartDay) - currentDayofWeek));
       	downTimeEnd.add(Calendar.DATE, (Integer.parseInt(tmpEndDay) - currentDayofWeek));

        if (downTimeStart.after(downTimeEnd)) {
        	downTimeEnd.add(Calendar.DATE, downTimeFrequency);
        }
        
        boolean adjust = true;
        
        while (adjust) {
            if (downTimeEnd.before(today)) {
            	downTimeStart.add(Calendar.DATE, downTimeFrequency);
            	downTimeEnd.add(Calendar.DATE, downTimeFrequency);
            } else {
            	adjust = false;
            }
        	
        }
 	}
	
	private void setMonitorHours(MonitorConfig monitor, String tmpStartTime, String tmpEndTime, String strWindowType) {
		String [] startTimes;
		String [] endTimes;
		Calendar today = Calendar.getInstance();
		int currentDay = today.get(Calendar.DAY_OF_MONTH);
		int currentMonth = today.get(Calendar.MONTH);
		int currentYear = today.get(Calendar.YEAR);

        startTimes = tmpStartTime.split(":");
        endTimes = tmpEndTime.split(":");

        // Create the startTime and endTime values
        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        startTime.set(currentYear, currentMonth, currentDay, 
        		      Integer.parseInt(startTimes[0]), Integer.parseInt(startTimes[1]));
        endTime.set(currentYear, currentMonth, currentDay, 
        		    Integer.parseInt(endTimes[0]), Integer.parseInt(endTimes[1]));
        
        if (strWindowType.equals(WINDOW_ACTIVE)) {
        	monitor.startTime = startTime;
        	monitor.endTime = endTime;
        	
        	// If Start Time is after End Time, increase End Time
        	if (monitor.startTime.after(monitor.endTime)) {
        		monitor.endTime.add(Calendar.DATE, 1);
        	}        	
        } else if (strWindowType.equals(WINDOW_OFF_HOURS)) {
        	monitor.startTimeOffHours = startTime;
        	monitor.endTimeOffHours = endTime;
        	
        	// If Start Time Off Hours is after End Time Off Hours, increase End Time Off Hours
        	if (monitor.startTimeOffHours.after(monitor.endTimeOffHours)) {
        		monitor.endTimeOffHours.add(Calendar.DATE, 1);
        	}
          }
	}

	private boolean isDownTime() {
		Calendar rightNow = Calendar.getInstance();

		if (downTimeStart == null) {
			return false;
		} else {
			if (downTimeStart.before(rightNow) && downTimeEnd.after(rightNow)) {
				// Valid monitor for right now
				return true;
			} else if (downTimeEnd.before(rightNow)) {
				// Need to increment the date on the monitor
	        	downTimeStart.add(Calendar.DATE, downTimeFrequency);
	        	downTimeEnd.add(Calendar.DATE, downTimeFrequency);
	        	return false;
			}
		}
		
		return false;
	}
	
	private boolean isInWindow(String strWindowType, MonitorConfig monitor, Calendar startTime, Calendar endTime ) {
		Calendar rightNow = Calendar.getInstance();

		if (startTime == null) {
			if (strWindowType.equals(WINDOW_OFF_HOURS)) {
				// Monitor is not configured with specific off hours times - so we are NEVER in "Off Hours"
				return false;
			} else {
				// Monitor is not configured with specific times - so we are always in that period
				return true;				
			}
		} else {
			// Monitor has time conditions
			if (startTime.before(rightNow) && endTime.after(rightNow)) {
				// Valid monitor for right now
				return true;
			} else if (endTime.before(rightNow)) {
				if (strWindowType.equals(WINDOW_ACTIVE)) {
					int daysToAdd = 1;
					if (monitor.monitorType.equalsIgnoreCase(TYPE_NODE_DOWN)) {
						daysToAdd = monitor.frequency;
					}
					// Need to increment the date on the monitor
		        	monitor.startTime.add(Calendar.DATE, daysToAdd);
		        	monitor.endTime.add(Calendar.DATE, daysToAdd);
		        	return false;
				} else if (strWindowType.equals(WINDOW_OFF_HOURS)) {
					// Need to increment the date on the monitor Off Hours settings
		        	monitor.startTimeOffHours.add(Calendar.DATE, 1);
		        	monitor.endTimeOffHours.add(Calendar.DATE, 1);
		        	return false;
				} else if (strWindowType.equals(WINDOW_DOWN_TIME)) {
					// Need to increment the date on the monitor Down Times
		        	downTimeStart.add(Calendar.DATE, downTimeFrequency);
		        	downTimeEnd.add(Calendar.DATE, downTimeFrequency);
		        	return false;					
				}
			}
		}

		
		return false;
	}
	
	private class MonitorConfig {
		public String monitorName;
		public String monitorType;
		public String operationType;
		public String serviceOperation;
		public String serviceOperationExclude;
		public String pubNode;
		public String subNode;
		public String status;
		public int age;
		public int threshold;
		public String action;
		public String reaction;
		public String alertStatus;
		public String sqlStatement;
		public String notifyTo;
		public String notifyCC;
		public String alertSubject;
		public String alertText;
		public int retryCount;
		public int timeToCheck;
		public int notificationInterval;  //specified in minutes
		public Calendar lastNotification;
		public Calendar startTime;
		public Calendar endTime;
		public Calendar startTimeOffHours;
		public Calendar endTimeOffHours;
		public int notificationIntervalOffHours;  //specified in minutes
		public boolean notificationFlag;
		public int frequency;  // Specified in days
		public Vector<EscalationConfig> escalations;
		public CustomActionConfig customAction;
		
		private MonitorConfig() {
			
		}

		private MonitorConfig(MonitorEventType configNode) {
			List<EscalationType> escalationList;
			String strStartDay = "";
			String strStartTime = "";
			String strEndDay = "";
			String strEndTime = "";
			String offHoursTimeStart = "";
			String offHoursTimeEnd = "";

			monitorName = configNode.getMonitorName();
			operationType = configNode.getOperationType();

			if (operationType.equalsIgnoreCase(PUBLICATION_CONTRACT) || 
				operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT) || 
				operationType.equalsIgnoreCase(MESSAGE_INSTANCE) ) {
				monitorType = TYPE_MESSAGE;
			} else if (operationType.equalsIgnoreCase(PUBLICATION_CONTRACT_THRESHOLD) || 
					   operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT_THRESHOLD) || 
					   operationType.equalsIgnoreCase(MESSAGE_INSTANCE_THRESHOLD) ) {
				monitorType = TYPE_THRESHOLD;
			} else if (operationType.equalsIgnoreCase(PUBLICATION_CONTRACT_AGING) || 
					   operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT_AGING) || 
					   operationType.equalsIgnoreCase(MESSAGE_INSTANCE_AGING) ) {
				monitorType = TYPE_AGING;
			} else if (operationType.equalsIgnoreCase(NODE_DOWNTIME)) {
				monitorType = TYPE_NODE_DOWN;
			}

			serviceOperation = getNodeValue(configNode.getServiceOperation(), "");
			serviceOperationExclude = getNodeValue(configNode.getServiceOperationExclude(), "");
			pubNode = getNodeValue(configNode.getPublishNode(), "");
			subNode = getNodeValue(configNode.getSubscribeNode(), "");
			status = getNodeValue(configNode.getStatus(), "");
			age = getNodeValue(configNode.getAge(), 0);
			threshold = getNodeValue(configNode.getThreshold(), 0);
			timeToCheck = getNodeValue(configNode.getTimeToCheck(), 0);

			action = getNodeValue(configNode.getAction(), "");
			reaction = getNodeValue(configNode.getReaction(), "");
			
			// Check for hasNotifyEach
			if (action.equalsIgnoreCase(ACTION_NOTIFY_EACH) || reaction.equalsIgnoreCase(ACTION_NOTIFY_EACH)) {
				hasNotifyEach = true;
			}
			notifyTo = getNodeValue(configNode.getNotifyTo(), "");
			notifyCC = getNodeValue(configNode.getNotifyCC(), "");
			alertSubject = getNodeValue(configNode.getAlertSubject(), "");
			alertText = getNodeValue(configNode.getAlertText(), "");
			
			// Set notificationFlag based on Action
			if (action.equalsIgnoreCase(ACTION_NOTIFY)) {
				notificationFlag = true;				
			} else if (action.equalsIgnoreCase(ACTION_CANCEL)) {
				notificationFlag = true;
			} else if (action.equalsIgnoreCase(ACTION_CUSTOM)) {
				notificationFlag = true;
			} else if (action.equalsIgnoreCase(ACTION_RETRY_REACT)) {
				notificationFlag = true;
			} else {
				notificationFlag = false;				
			}
			
			retryCount = getNodeValue(configNode.getRetryCount(), 0);
			notificationInterval = getNodeValue(configNode.getNotifyInterval(), 0);
			alertStatus = STATUS_OK;
			lastNotification = null;
			
			if (monitorType.equalsIgnoreCase(TYPE_NODE_DOWN)) {
				strStartDay = getNodeValue(configNode.getDownTimeStartDay(), "");
				strStartTime = getNodeValue(configNode.getDownTimeStart(), "");
				strEndDay = getNodeValue(configNode.getDownTimeEndDay(), "");
				strEndTime = getNodeValue(configNode.getDownTimeEnd(), "");
				frequency = getNodeValue(configNode.getDownTimeFrequency(), 0);

				// Schedule first occurrence
				setNodeDownTime(strStartDay, strStartTime, strEndDay, strEndTime);
				
			} else {
				strStartTime = getNodeValue(configNode.getStartTime(), "");
				strEndTime = getNodeValue(configNode.getEndTime(), "");				

				// Allow any Monitor to use Start/End times
				if (!strStartTime.equalsIgnoreCase("") && !strEndTime.equalsIgnoreCase("")) {
						setMonitorHours(this, strStartTime, strEndTime, WINDOW_ACTIVE);
				}
			}
			

			// Set Off Hours monitoring, if configured
			// If we have a specific notification interval - use Off Hours
			notificationIntervalOffHours = getNodeValue(configNode.getNotifyIntervalOffHours(), 0);
			if (notificationIntervalOffHours > 0) {
				offHoursTimeStart = getNodeValue(configNode.getStartTimeOffHours(), defaultOffHoursTimeStart);
				offHoursTimeEnd = getNodeValue(configNode.getEndTimeOffHours(), defaultOffHoursTimeEnd);

				if (!offHoursTimeStart.equalsIgnoreCase("") && !offHoursTimeEnd.equalsIgnoreCase("")) {
					setMonitorHours(this, offHoursTimeStart, offHoursTimeEnd, WINDOW_OFF_HOURS);
				}				
			}
			
			// Get any custom actions
			if (action.equalsIgnoreCase(ACTION_CUSTOM)) {
				customAction = new CustomActionConfig(configNode.getCustomAction().get(0));
			}
			
			// Configure any Escalations
			escalationList = configNode.getEscalation();
			// Get each specific monitor to execute
			for (int index = 0; index < escalationList.size(); index ++ ) {
				if  (escalations == null) {
					escalations = new Vector<EscalationConfig>();
				}
				escalations.add(index, new EscalationConfig(escalationList.get(index)));
			}

			sqlStatement = generateMonitorSQL(this);
		}
		
		private void setNodeDownTime(String tmpStartDay, String tmpStartTime, String tmpEndDay, String tmpEndTime) {
			String [] startTimes;
			String [] endTimes;

			Calendar today = Calendar.getInstance();
			int currentDayofWeek = today.get(Calendar.DAY_OF_WEEK);
			int currentDay = today.get(Calendar.DAY_OF_MONTH);
			int currentMonth = today.get(Calendar.MONTH);
			int currentYear = today.get(Calendar.YEAR);

	        startTimes = tmpStartTime.split(":");
	        endTimes = tmpEndTime.split(":");

	        startTime = Calendar.getInstance();
	        endTime = Calendar.getInstance();

	        startTime.set(currentYear, currentMonth, currentDay, 
	        		          Integer.parseInt(startTimes[0]), Integer.parseInt(startTimes[1]));
	        endTime.set(currentYear, currentMonth, currentDay, 
	        		        Integer.parseInt(endTimes[0]), Integer.parseInt(endTimes[1]));

	       	// Shift the date accordingly
	       	startTime.add(Calendar.DATE, (Integer.parseInt(tmpStartDay) - currentDayofWeek));
	       	endTime.add(Calendar.DATE, (Integer.parseInt(tmpEndDay) - currentDayofWeek));

	        if (startTime.after(endTime)) {
	        	endTime.add(Calendar.DATE, frequency);
	        }
	        
	        boolean adjust = true;
	        
	        while (adjust) {
	            if (endTime.before(today)) {
	            	startTime.add(Calendar.DATE, frequency);
	            	endTime.add(Calendar.DATE, frequency);
	            } else {
	            	adjust = false;
	            }
	        	
	        }
		}
		
		// Function to perform the Retry/React logic
		private boolean retryReact(String transactionID) {
			boolean react = false;
			String sqlRecord = "";
			String statusField = "";
			
			//Determine the Record/Field to update
			if (operationType.equalsIgnoreCase(PUBLICATION_CONTRACT)) {
				sqlRecord = "PSAPMSGPUBCON";
				statusField = "PUBCONSTATUS";
			} else if (operationType.equalsIgnoreCase(SUBSCRIPTION_CONTRACT)) {
				sqlRecord = "PSAPMSGSUBCON";
				statusField = "SUBCONSTATUS";
			} else if (operationType.equalsIgnoreCase(MESSAGE_INSTANCE)) {
				sqlRecord = "PSAPMSGPUBHDR";
				statusField = "PUBSTATUS";
			}

			try {
				Statement stmt = connMonitor.createStatement();
				String sqlUpdate = "Update " + databaseSchema + "." + sqlRecord;
				sqlUpdate = sqlUpdate + " Set " + statusField + " = " + 1 + ", STATUSSTRING = 'NEW'";
				sqlUpdate = sqlUpdate + " Where IBTRANSACTIONID = '" + transactionID + "' and RETRYCOUNT < " + retryCount;
				if (stmt.executeUpdate(sqlUpdate) == 0) {
					react = true;
					if (reaction.equalsIgnoreCase(ACTION_CANCEL)) {
						executeUpdate(transactionID, operationType, reaction, databaseName, connMonitor);
						logger.info("Retry/Resubmit executeUpdate fired on Transaction ID: " + transactionID + " in " + databaseName + ": Retry Count = " + retryCount);
					} else if (reaction.equalsIgnoreCase(ACTION_CUSTOM)) {
						// Fire the Custom Action
						customAction.fireAction(transactionID);
					} else if (reaction.equals(ACTION_NOTIFY_EACH)) {
						notifyEach(this, transactionID);
					} else if (!reaction.equals(ACTION_NOTIFY)) {
						logger.info("Invalid Reaction encountered on Transaction ID: " + transactionID + " in " + databaseName);
					}
				}
				stmt.close();
			} catch (SQLException s) {
				logger.info("SQLException encountered in " + databaseName + ": " + s.getMessage());
			}	

			return react;
		}
		
	}
	
	private String getNodeValue(Object nodeValue, String defaultValue) {
		if (nodeValue == null) {
			return defaultValue;
		} else {
			return (String)nodeValue;
		}
	}

	private int getNodeValue(Object nodeValue, int defaultValue) {
		if (nodeValue == null) {
			return defaultValue;
		} else {
			return (Integer)nodeValue;
		}
	}

	private class EscalationConfig{
		public String email;
		public String emailText;
		public boolean notificationSent;
		public int escalationDelay;
		public int notificationInterval;  //specified in minutes
		public Calendar issueDetected;
		public Calendar lastNotification;


		private EscalationConfig(){

		}

		private EscalationConfig(EscalationType escalationNode) {
			EmailAdditionsType emailAdditions;

			notificationSent = false;
			email = getNodeValue(escalationNode.getNotifyTo(), "");
			escalationDelay = getNodeValue(escalationNode.getEscalationDelay(), 0);
			notificationInterval = getNodeValue(escalationNode.getNotificationInterval(), 0);
			issueDetected = null;
			lastNotification = null;
			emailText = "";

			emailAdditions = escalationNode.getEmailAdditions();
			emailText = emailText + CRLF + getEmailAdditions(emailAdditions);
		}

		private String getEmailAdditions(EmailAdditionsType emailAddition) {
			String tmpText = "";
			List<String> emailLines;

			emailLines = emailAddition.getEmailLine();
			// Get each specific email Tag
			for (int index = 0; index < emailLines.size(); index ++ ) {
				tmpText = tmpText + CRLF + emailLines.get(index);
			}

			return tmpText;

		}

	}

	private class CustomActionConfig{
		private Vector<String> sqlCommands;

		private CustomActionConfig(){

		}

		private CustomActionConfig(CustomActionType customActionNode) {
			sqlCommands = getSQLCommands(customActionNode.getSqlCommand());
		}

		private Vector<String> getSQLCommands(List<String> sqlList) {
			Vector<String> sqlCommands = new Vector<String>();

			// Get each specific SQLs to execute
			for (int index = 0; index < sqlList.size(); index ++ ) {
				sqlCommands.add(sqlList.get(index));
			}

			return sqlCommands;

		}
		
		private void fireAction(String transactionID) {
			try {
				Statement stmt = connMonitor.createStatement();
				for (int index = 0; index < sqlCommands.size(); index ++) {
					String sqlCommand = sqlCommands.get(index).replaceAll("<TRANSACTIONID>", transactionID);
					stmt.executeUpdate(sqlCommand);
				}
				stmt.close();
			} catch (SQLException s) {
				logger.info("SQLException encountered in " + databaseName + ": " + s.getMessage());
			}	
		}

	}

}
