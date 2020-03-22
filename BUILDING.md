# Building the Application

The Multi-Timer application uses Maven for build and classpath management. The main source code is written in Java and is therefore essentially platform independent. However, the source and Maven POM files as provided are configured for a Windows 10 build.

## Windows 10

To build Multi-Timer on Windows 10 simply execute:
> C:\source\multi-timer> mvn install

This will create a Windows installer file multi-timer\modules\multi-timer-installer\target\multi-timer-installer-${version}.msi file, which gets installed into the local Maven repository as %USERPROFILE%\.m2\repository\io\github\demonfiddler\multi-timer\multi-timer-installer\${version}\multi-timer-installer-${version}.msi

The build makes use of [launch4j-maven-plugin](https://github.com/lukaszlenart/launch4j-maven-plugin) with [launch4j](http://launch4j.sourceforge.net/index.html) to create the executable Multi-Timer.exe, and [wix-maven-plugin](https://wix-maven.github.io/wix-maven-plugin/) with [Wix Toolset](https://wixtoolset.org/) to create the MSI package.

## Other Operating Systems/Versions

Building on other Windows versions or other operating systems will necessitate some local modifications to class-path and POM files.