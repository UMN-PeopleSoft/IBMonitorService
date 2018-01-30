# IBMonitor
University of Minnesota PeopleSoft Integration Broker Monitoring Service

## Event Configurations 
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

