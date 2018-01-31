# Installing the Monitor
The first step to installing the monitor is to acquire a pre-built disribution or pull the project and build your own distribution.

## Prerequisites
### Running the IB Monitor
To run the IB Monitor, you will need:
   * A machine to run the monitor on.  Choose your favorite OS.
   * Java 1.8
   * The IB Monitor
   
### Contributing to the IB Monitor
   * Git
   * Java Development Environment
  
## Acquiring the IB Monitor
### Acquiring a Pre-Built Distribution
At the top of the page, you will see two links:
   * Download .zip
   * Download .tar.gz

Choose the distribution package you'd like, click and download.

### Building Your Own Distribution
To build your own distribution, pull the project if you have not already done so to obtain a local copy.

`git clone https://github.com/UMN-PeopleSoft/IBMonitorService`

When you are ready to distribute the application to your monitoring platform, run the `assembleDist` task.  Once complete, you will have a .tar and .zip file you can use for monitoring.

If you want a simple "one-liner" to pull and build, you can run the following:
`git clone https://github.com/UMN-PeopleSoft/IBMonitorService && cd IBMonitorService && ./gradlew assembleDist`

## Installing the Monitor
Once you have a distribution, extract the files into a folder on the machine where you plan to run the monitor.
