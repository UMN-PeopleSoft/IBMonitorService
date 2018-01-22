package edu.umn.pssa.ibmonitorservice;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.json.JSONObject;
import org.json.XML;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

public class IBMonitorSvc {
	
	// Default configuration file
	private static String configFile = "configs.xml";

	// Logger
	private static final Logger logger = Logger.getLogger(IBMonitorSvc.class.getName());
	
	private static int monitoredDBs;
	private static Vector<MonitorInfo> dbMonitors = new Vector<MonitorInfo>();
	private static Thread waitThread = new Thread();
	
	// DOM Source for configuration
	private static DOMSource configDOM;

	public static String onCallFileName = "";
	public static String emailReplyTo = "";
	public static String emailPassword = "";
	public static String emailHost = "";
	public static String emailUser = "";
	public static int emailPort = 0;
	private static boolean debugMode = false;
	private static String databaseType = "";
	
    public static void main(String[] args) {
    	logger.info("Started Monitor");
    	
    	if (args != null && args.length > 0) {
    		try {
        		configFile = args[0];
        		logger.info("Overriding Default Configuration File: Using " + configFile);
    		} catch (Exception e) {
    			logger.info("Unable to find configuration file: " + configFile);
    		}
    	}
    	
        new IBMonitorSvc();
    }
    
	private DOMSource createDOM(String fileName) {
		String xml = "";

		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			String content = new String(Files.readAllBytes(Paths.get(fileName)));
			if (fileName.toLowerCase().endsWith(".xml")) {
				xml = content;
			} else if (fileName.toLowerCase().endsWith(".json")) {
				JSONObject json = new JSONObject(content);
				xml = XML.toString(json);
			} else if (fileName.toLowerCase().endsWith(".yaml")) {
				xml = XML.toString(new JSONObject(convertYamlToJson(content)));
			}

			Document doc = db.parse(new InputSource(new StringReader(xml)));
			return new DOMSource(doc);
		} catch (Exception e) {
			logger.info("Unable to find configuration file: " + configFile);
		}

		logger.info("Invalid Configurations Supplied.  Exiting Program.");
		return null;
	}
	
	private static String convertYamlToJson(String yamlString) {
	    Yaml yaml= new Yaml();
	    @SuppressWarnings("unchecked")
		Map<String,Object> map= (Map<String, Object>) yaml.load(yamlString);

	    JSONObject jsonObject=new JSONObject(map);
	    return jsonObject.toString();
	}

    private IBMonitorSvc() {
    	boolean monitor;
    	String dbName = "";    	
    	
		monitor = monitorStartup();
		
		while (monitor) {
			// Check for disconnected ibMonitors every 5 Minutes
			try {
				synchronized (waitThread) {
					Thread.sleep(300000); // 5 minutes
					for (int i = 0; i < monitoredDBs; i++) {
						MonitorInfo dbMonitor = dbMonitors.get(i);
						if (dbMonitor.ibMonitor == null) {
							dbMonitor.ibMonitor = new Monitor(dbMonitor.databaseNode, dbMonitor.monitorID, debugMode, databaseType);
							dbMonitor.ibMonitor.start();
							dbName = dbMonitor.databaseName;
						}
					}
				}
			} catch (Exception e) {
				logger.info("Exception caught while Monitoring " + dbName + ": "  + e.getMessage());
			}
		}
	
	}

	private boolean monitorStartup() {
		String dbName = "";
		monitoredDBs = 0;
		
		try {
			JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();
			configDOM = createDOM(configFile);
			
			if (configDOM != null) {
				JAXBElement<ConfigType> configElement = u.unmarshal(configDOM, ConfigType.class);
				ConfigType configs = configElement.getValue();		
				
				// Get the Core Configurations
				onCallFileName = configs.getOnCallFile();
				emailReplyTo = configs.getEmailReplyTo();
				emailUser = configs.getEmailUser();
				emailPassword = configs.getEmailPassword();
				emailHost = configs.getEmailHost();
				emailPort = configs.getEmailPort();
				debugMode = "ON".equalsIgnoreCase(configs.getDebugMode());
				
				// Log we're in debug mode for everyone
				if (debugMode) {
					logger.info("Debug Mode turned on globally.");
				} else {
					// verify we have connectivity
					if (configs.getDbType() == null) {
						logger.info("No database type specified while not in debug mode.  Exiting program.");
						System.exit(1);
					} else {
						databaseType = configs.getDbType();
						MonitorConnection testConnection = new MonitorConnection(databaseType);
						if (!testConnection.testConnectivity()){
							logger.info("Unable to find driver for database connectivity.  Exiting program.");
							System.exit(1);
						}
					}
				}

				List<DatabaseType> databases = configs.getDatabase();
				
				for (int dbNum=0; dbNum < databases.size(); dbNum++) {
					// Read XML configurations:		
					// For each DB to monitor, create/run a new clsMonitor
	                DatabaseType databaseNode = databases.get(dbNum);
	                
	                dbName = databaseNode.getDatabaseName();
	                MonitorInfo tempMonitorInfo = new MonitorInfo();
	                tempMonitorInfo.monitorID = monitoredDBs;
	                tempMonitorInfo.databaseNode = databaseNode;
	                tempMonitorInfo.databaseName = dbName;
	                tempMonitorInfo.ibMonitor = new Monitor(databaseNode, monitoredDBs, debugMode, databaseType);
					
	                // Only start Monitor if not in Debug mode
	                if (!debugMode) {
	                    tempMonitorInfo.ibMonitor.start();
	                    
	                    dbMonitors.add(monitoredDBs, tempMonitorInfo);
	    				monitoredDBs = monitoredDBs + 1;  
	                }
	                
	            } //end of if clause				
			}
                
		} catch (JAXBException e1) {
			logger.info("JAXB Exception caught opening configurations " + configFile + ": "  + e1.getMessage());
		}

		if (monitoredDBs > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	// Method to cancel a monitor, if an exception has occurred or for other reasons
	public static void terminateMonitor(int monitorNum) {
		for (int i = 0; i < monitoredDBs; i++) {
			MonitorInfo dbMonitor = dbMonitors.get(i);
			if (dbMonitor.monitorID == monitorNum) {
				dbMonitor.ibMonitor = null;
			}
		}
	}
		
	private class MonitorInfo {
		public int monitorID;
		public String databaseName;
		public DatabaseType databaseNode;
		public Monitor ibMonitor;
		
	}

}