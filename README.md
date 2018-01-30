# Monitor Overview
The PeopleSoft Integration Broker Service was developed by the University of Minnesota to provide robust monitoring of asynchronous messaging into and out of the PeopleSoft Enterprise.  Through simple XML configuration, the monitor will check at defined intervals for pre-defined scenarios, when if found, will trigger configured actions such as notification.  To ensure complete monitoring, with no issues able to slip by unnoticed, there is logic included in the monitoring to look for issues outside of those specifically configured.  These are called **Default Monitors**.

The Integration Broker Monitor is capable of not only monitoring inbound and outbound messages, but it can also monitor for Active IB Domains, backlogged messages, and schedule Node downtime.

Database access is required to run the monitor, and the ID used for connectivity will need access to the following PeopleSoft tables:
  * PSAPMSGPUBHDR
  * PSAPMSGPUBCON
  * PSAPMSGSUBCON
  * PSAPMSGDOMSTAT
  * PSNODESDOWN
  
Additionally, the IB Monitor will create a table called UM_IB_MONITOR in the schema for the Database User used for executing the monitors.  This table is used with the "Notify Each" logic.
