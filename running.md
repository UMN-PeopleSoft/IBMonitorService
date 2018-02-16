# Running the Monitor
Once you have the monitor installed, and a set of configurations in place, the next step is to actually run the monitor.

## Running on Windows using Powershell
   * $env:JAVA_HOME="c:\path\to\jdk8"
   * $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
   * .\bin\IBMonitorService.bat .\configs.yaml -- pass in configuration file only if not using configs.xml

## Running on Windows without Powershell

## Running on OS Other than Windows
From the IBMonitorService folder:
   * ./IBMonitorService configs.yaml -- pass in configuration file only if not using configs.xml
   * ./IBMonitorService configs.yaml & -- this will run the monitor in the background.
