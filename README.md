# IBMonitor
University of Minnesota PeopleSoft Integration Broker Monitoring Service

## Monitor Overview
The PeopleSoft Integration Broker Service was developed by the University of Minnesota to provide robust monitoring of asynchronous messaging into and out of the PeopleSoft Enterprise.  Through simple XML configuration, the monitor will check at defined intervals for pre-defined scenarios, when if found, will trigger configured actions such as notification.  To ensure complete monitoring, with no issues able to slip by unnoticed, there is logic included in the monitoring to look for issues outside of those specifically configured.  These are called **Default Monitors**.

The Integration Broker Monitor is capable of not only monitoring inbound and outbound messages, but it can also monitor for Active IB Domains, backlogged messages, and schedule Node downtime.

Database access is required to run the monitor, and the ID used for connectivity will need access to the following PeopleSoft tables:
  * PSAPMSGPUBHDR
  * PSAPMSGPUBCON
  * PSAPMSGSUBCON
  * PSAPMSGDOMSTAT
  * PSNODESDOWN
  
Additionally, the IB Monitor will create a table called UM_IB_MONITOR in the schema for the Database User used for executing the monitors.  This table is used with the "Notify Each" logic.
  
## Installing the Monitor
To install the IB Monitor Service, pull the project to obtain a local copy.

`git clone https://github.com/UMN-PeopleSoft/IBMonitorService`

When you are ready to distribute the application to your monitoring platform, run the `assembleDist` task.  Once complete, you will have a .tar and .zip file you can use for monitoring.

If you want a simple "one-liner" to pull and build, you can run the following:
`git clone https://github.com/UMN-PeopleSoft/IBMonitorService && cd IBMonitorService && ./gradlew assembleDist`
  
## Configuring the Monitor
There are a few layers for the configuraiton of the monitoring.  Base configuration is the highest level of configuration and is used primarily to set the email configurations, along with specifying the database type and optionally setting debug mode.

The next level of configuration is for each database, or application, to be monitored.  Here, configuration is centered around the connectivity for the database, and rules to use for default monitoring, such as status, time periods, notifications and more.

The actual events to watch for are configured at the next level, called Event Configuration.  Here, the specific criteria to monitor for are defined, along with the necessary Escalations and/or Custom Actions.

The elements available for configuration are explained below.
### Logging
The IB Monitor uses log4j for logging output.  The configuration can be set in the log4j.properties file within the project.  By default, logs are configured to be placed in a *logs* folder of the bin directory.

### Base Configuration 
Some core information must be configured for notifications to be sent. This information is configured in the nodes under configs, and includes: 
  * **emailUser:** This is the user id for the email account used to send email/pager notifications. 
  * **emailPassword:** This is the password for emailUser. 
  * **emailReplyTo:** This is the email address used as the "Reply To" email address for notifications. 
  * **emailHost:** This is the host address for emailUser. Example: smtp.gmail.com
  * **emailPort:** This is the port for the emailHost. Example: 465
  * **onCallFile:** This configuration allows for specifying the location of a file containing On Call information. The expected format of the file should be:
   * name:notification
   * #name:notification - this format will be treated as having the name commented out, and will not send a notification.
  * **debugMode:** Flag to set a debug mode, which will only write the generated SQL to a file.
  * **dbType:** Parameter to set the database type, ie: Oracle.

### Database Configurations 
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

### Event Configurations 
Within each set of database configurations, multiple events can be set up. Each event can watch for a specified set of conditions. An event can notify via page/email, automatically resubmit or cancel a message, or execute a custom action (SQL).  Default checks are performed which look for events not specifically configured, ie: catches everything but the configured events, so you don't have to configure every possible scenario. Events are configured with the following: 
 * **monitorName:** This is a name for the monitored event. 
 * **operationType:** This specifies if the monitored event is for Operation Instances, Publication Contracts or Subscription Contracts. Valid values are: 
   * MessageInstance - Message Instance event. 
   * PubContract - Publication Contract event.
   * SubContract - Subscription Contract event. 
   * MsgInstanceThreshold - Message Instance Threshold event.
   * PubContactThreshold - Publication Contract Threshold event.
   * SubContactThreshold - Subscription Contract Threshold event. 
   * MsgInstanceAging - Message Instance Aging event.
   * PubContactAging - Publication Contract Threshold Aging event.
   * SubContactAging - Subscription Contract Threshold Aging event. 
 * **serviceOperation:** This configuration specifies the Service Operation(s) to monitor. If more than one Service Operation is to be monitored, separate the values with commas. **Note:** When configuring a Node Down event, use a single space to designate all operations on the specified node as "Down".
 * **serviceOperationExclude:** This configuration is similar to serviceOperation (above), but specifies the Service Operation(s) to exclude from the monitor. If more than one Service Operation is to be excluded, separate the values with commas. 
 * **publishNode:** This can specify the publishing node for the monitored event. Useful when the same message could be sent from multiple systems which may dictate how the event is handled. 
 * **subscribeNode:** This can specify the subscribing node for the monitored event. Useful when the same message can be sent to multiple systems which may dictate how the event is handled. 
 * **status:** This is a comma separated list of the numeric values associated with each status to look for. A list of values is provided above. 
 * **timeToCheck:** This is the amount of time passed the monitor will check. When set to 0, there is no time limit to how far back the monitor will look. For example, if the defaultMonitorTime is set to 60, the monitor will only look at the last hour for the default conditions. 
 * **retryCount:** This is the configuration for the maximum number of Retry attempts for the Action of "Retry/React". 
 * **theshold:** This is a numeric value designating the threshold at which the event is to be triggered. 
 * **age:** A numeric value representing the age in number of minutes for messages to trigger the event.
 * **action:** This configuration indicates what action to take. Valid values are:
   * Notify - sends a page/email to specified recipients.
   * Notify Each - sends a notification and/or escalation per Transaction ID found.
   * Cancel - cancels the message(s). 
   * Resubmit - resubmits the message(s). 
   * Retry/React - attempts to resubmit the message until the configured retryCount is reached, then fires the configured reaction.
   * Custom - Perform a custom SQL action
 * **reaction:** This configuration indicates what action to take when an Action of Retry/React exceeds the maximum retry count. Valid values are:
   * Notify - sends a page/email to specified recipients
   * Notify Each - sends a notification and/or escalation per Transaction ID found.
   * Cancel - cancels the message(s).
   * Custom - Perform a custom SQL action 
 * **notifyTo:** This configuration is used to provide the list of recipients for the Notification of default events. If a value of On Call is found in the configurations, it will include the active recipients from the onCallFile configuration, if the file is valid. 
 * **notifyCC:** This configuration is the same as above, however the recipients will be CC'd on the notification. 
 * **alertSubject:** This is the subject line of the alert to be sent out. 
 * **alertText:** This is the body of the alert to be sent out. 
 * **notifyInterval:** This configuration specifies the period of time to wait (in minutes) between notifications of the same event. ie: if a message remains in error, resend the notification at each notifyInterval minutes until the condition is no longer found. 
 * **startTime:** This is a configuration used to designate the start time (daily) of the monitor. Value should be entered as HH:MM. This configuration is valid for both Threshold monitors and messaging issues. 
 * **endTime:** This is a configuration used to designate the end time (daily) of the monitor. Value should be entered as HH:MM. This configuration is valid for both Threshold monitors and messaging issues. 
 * **notifyIntervalOffHours:** This configuration specifies the period of time to wait (in minutes) between notifications of the same event when in a configured "Off Hours" period. ie: if a message remains in error, resend the notification at each notifyIntervalOffHours minutes until the condition is no longer found. 
 * **startTimeOffHours:** This is a configuration used to designate the start time (daily) of the Off Hours period. Value should be entered as HH:MM. This configuration is valid for both Threshold monitors and messaging issues. NOTE: If left blank, but a notifyIntervalOffHours is configured for the event, the defaultStartTimeOffHours will be used. 
 * **endTimeOffHours:** This is a configuration used to designate the end time (daily) of the Off Hours period. Value should be entered as HH:MM. This configuration is valid for both Threshold monitors and messaging issues. NOTE: If left blank, but a notifyIntervalOffHours is configured for the event, the defaultEndTimeOffHours will be used. 
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

### Escalation Configurations 
Each Event can be configured to have any number of Escalations.  Escalations will fire independent of the parent Event and the other Escalations.  They can fire immediately, or after a defined delay.  Escalations can also include additional text in the notifications.  Escalations are configured with the following:
 * **notifyTo:** This configuration is used to provide the list of recipients for the Notification of default events. If a value of On Call is found in the configurations, it will include the active recipients from the onCallFile configuration, if the file is valid. 
 * **escalationDelay:** This configuration is used to specify the delay (in minutes) before firing the notification when the parent event is triggered. 
 * **notifyInterval:** This configuration specifies the period of time to wait (in minutes) between notifications of the same event. ie: if a message remains in error, resend the notification at each notifyInterval minutes until the condition is no longer found. 

### Email Addition Configurations 
Each Escalation can be configured to have any number of Email Additions.  Email Additions are configured with the following:
 * **emailLine:** Text to be added to the notification, such as email or text message.  

### Custom Action Configurations 
If an Event is set to use an Action of "Custom", the SQL for the Action should be configured in the Custom Actions section of the XML.  Custom Actions are configured with the following:
 * **sqlCommand:** The SQL to fire when the parent Event is triggered.  **NOTE:** To include the Transaction ID of the message in the SQL, place **\<TRANSACTIONID\>** in the SQL command, and it will be replaced with the actual Transaction ID at execution time.  

