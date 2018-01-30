# Database Configurations 
For each database to be monitored, connection and default event information can be configured in nodes under base configurations. The available database configurations include: 
 * **databaseName:** Specify the common name for the database. This does not have to match the actual database, nor does it have to match the value in the tnsnames.ora file. This is used in the monitor to know what database it is watching, and the name provided will be used for things such as logging output.
 * **dbType:** Parameter to set the database type if different than the "global" database type.
 * **host:** This is the host string the monitor will use to connect to the database. The Oracle connections do not require the databases be entered in a tnsnames.ora file. Instead, a connection string in the format of :/ is used. 
 * **user:** This is the database user which will establish connections for the monitor. 
 * **password:** This is the password for user. 
 * **dbSchema:** This is the schema for the monitored application. 
 * **sleepTime:** This is the number of minutes the monitor will sleep after each check of the configured events before checking again. 
 * **defaultMonitorTime:** This is the amount of time passed the monitor will check. When set to 0, there is no time limit to how far back the monitor will look. For example, if the defaultMonitorTime is set to 60, the monitor will only look at the last hour for the default conditions.
 * **defaultStatusToCheck:** This is a comma separated list of the numeric values associated with each status to look for. Typically, the values monitored are 0,5,6 which corresponds to Errors, Retries and Timeouts. Here is a listing of all Status Values: 
   * 0 - ERROR 
   * 1 - NEW 
   * 2 - START 
   * 3 - WRKNG 
   * 4 - DONE 
   * 5 - RETRY 
   * 6 - TIME 
   * 7 - EDIT 
   * 8 - CNCLD 
   * 9 - HOLD 
 * **defaultRetryCount:** This is a configuration for future use. It will dictate how many times to attempt an action such as Resubmit or Cancel. 
 * **defaultNotifyTo:** This configuration is used to provide the list of recipients for the Notification of default events. If a value of On Call is found in the configurations, it will include the active recipients from the onCallFile configuration, if the file is valid. 
 * **defaultNofityCC:** This configuration is the same as above, however the recipients will be CC'd on the notification. 
 * **defaultNotifyInterval:** This is used to configure the Notification Interval for "Default" notifications. If missing, a value of 60 minutes is used by default. 
 * **downtimeStartDay:** This is a configuration used to designate the start day of the downtime. Valid values are:
   * 1 - Sunday 
   * 2 - Monday 
   * 3 - Tuesday 
   * 4 - Wednesday 
   * 5 - Thursday 
   * 6 - Friday 
   * 7 - Saturday 
 * **downtimeStart:** This is a configuration used to designate the start time of the downtime. Value should be entered as HH:MM, using a 24 hour clock. 
 * **downtimeEndDay:** This is a configuration used to designate the end day of the downtime. Valid values are the same as those for downtimeStartDay. 
 * **downtimeEnd:** This is a configuration used to designate the end time of the downtime. Value should be entered as HH:MM, using a 24 hour clock. 
 * **downtimeFrequency:** This configuration specifies the number of days between downtime windows. For example, if the value 7 is provided, the downtime would be weekly. 
 * **defaultNotifyIntervalOffHours:** This is used to configure the Notification Interval for "Default" notifications during "Off Hours". 
 * **startTimeOffHours:** This is a configuration used to designate the start time (daily) of the Off Hours period for Default events, and it can be inherited by other events. 
 * **endTimeOffHours:** This is a configuration used to designate the end time (daily) of the Off Hours period for Default events, and it can be inherited by other events. 
 * **domainStatus:** Flag to turn on monitoring of the Domain Status (PSAPMSGDOMSTAT) for the application.  Notification will be sent to the default notification group when a count of 0 Active Domains is found.
 * **debugMode:** Flag to set a debug mode, which will only write the generated SQL to a file.
