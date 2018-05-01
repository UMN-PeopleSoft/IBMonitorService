# IB Monitoring in 90 Minutes or Less
The steps below should help you get the IB Monitor running a basic configuration in less than 90 minutes.

## Prerequisites
To run the IB Monitor, you will need:
   * A machine to run the monitor on.  Choose your favorite OS.
   * Java 1.8
   * The IB Monitor
   
## Get Up and Running
**Step 1.** Go to the IB Monitor GitHub Page: <https://umn-peoplesoft.github.io/IBMonitorService/>

**Step 2.**	Choose your preferred distribution, and download from the left hand navigation/menu

**Step 3.**	Download the desired sample.<xxx> file from the left hand navigation/menu.

**Step 4.**	Copy compressed distribution to a destination on the server or workstation where the IB Monitor will run.

**Step 5.**	Extract the contents of the distribution into the destination where the IB Monitor will run.

**Step 6.**	Copy the downloaded sample.<xxx> file to the IBMonitorService/bin folder.  You can use any file name you would like
   * Ex: cp sample.yaml IBMonitorService/bin/configs.yaml

**Step 7.**	Edit the newly copied configuration file.  Edit or update the following core configurations
   * emailUser (optional)
   * emailPassword (optional)
   * emailReplyTo
   * emailHost
   * emailPort
   * onCallFile (optional)
   * debugMode (optional: default is off)
   * databaseType (Oracle or SQLServer)

**Step 8.**	Provide database connectivity configurations within a <database> element
   * databaseName
   * host
   * user
   * password
   * dbSchema
   * sleepTime (suggest 5, which translates to 5 minutes)
   * defaultNotifyTo

**Step 9.**	If specific monitoring events are desired, configure those within the monitorEvent(s).  See instructions on the IB Monitor GitHub Page for more details.  The IB Monitor does require at least one additional monitorEvent.  Sample files include PERSON_BASIC_SYNC Publication and Subscription examples.

## Running the IB Monitor on Windows
**Step 1.**	Execute the monitor.  Powershell example below
   * $env:JAVA_HOME="c:\path\to\jdk8"
   * $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
   * .\bin\IBMonitorService.bat .\configs.yaml – configuration file is only required if not “configs.xml”

## Running the IB Monitor NOT on Windows
**Step 1.**	Execute the IBMonitorService/bin/IBMonitorService file.
   * If you are not using configs.xml as the configuration file, pass the name of your configuration file in as a parameter:
      * ./IBMonitorService configs.yaml
      * ./IBMonitorService configs.yaml & -- this will run the monitor in the background.
      
**Step 2.** If you get a Java error when attempting to execute the IB Monitor, edit the IBMonitorService/bin/IBMonitorService file, adding a line to set the JAVA_HOME.

