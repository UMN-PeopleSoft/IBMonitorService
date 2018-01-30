# IBMonitor
University of Minnesota PeopleSoft Integration Broker Monitoring Service

## Configuring the Monitor
There are a few layers for the configuraiton of the monitoring.  Base configuration is the highest level of configuration and is used primarily to set the email configurations, along with specifying the database type and optionally setting debug mode.

The next level of configuration is for each database, or application, to be monitored.  Here, configuration is centered around the connectivity for the database, and rules to use for default monitoring, such as status, time periods, notifications and more.

The actual events to watch for are configured at the next level, called Event Configuration.  Here, the specific criteria to monitor for are defined, along with the necessary Escalations and/or Custom Actions.

The elements available for configuration are explained below.

## Logging
The IB Monitor uses log4j for logging output.  The configuration can be set in the log4j.properties file within the project.  By default, logs are configured to be placed in a *logs* folder of the bin directory.

## Base Configuration 
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
