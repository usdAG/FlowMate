# FlowMate

![FlowMateLogo](images/flow-mate-dark.png)

Have you ever wondered how to consider all input-to-output correlations of a web application during a pentest? With **FlowMate**, you no longer have to. **FlowMate** is our BurpSuite extension designed to introduce taint analysis to web applications. It achieves this by monitoring all parameters sent to a target application and identifying their appearances in the corresponding responses.

This tool operates from either a black-box or grey-box perspective, eliminating the need for any modifications to the underlying infrastructure or the application itself. Moreover, it generates a visual graph that encompasses all parameters in the background. Whenever you require more intricate insights into a specific parameter, value, or URL, you can effortlessly refer to the integrated Neo4J browser to access the graph. This can be accomplished either through a single query in the Neo4J browser or by using the provided built-in query view.

## Key Features
Some key features of FlowMate are:
- Track parameter values of all applications added to the BurpSuite project scope.
- Matching of parameters can be performed live while browsing, or deferred in order to reduce load times while manually browsing the application.
- Store all data points in a local and file-based Neo4J instance.
- Integrates the Neo4J Browser directly to visualize and browse the resulting graph. No installation needed.
- Enables you to define *Sessions* within the plugin to ease tracking cross-session parameters.
- Performs automatic audit steps on the created graph to generate Findings with points of interest.
- Has the capability to define filters for parameters in order to reduce noise in the resulting data flow graph.

## Demo Video
A video demonstrating the end-to-end use of FlowMate is available on our YouTube channel.

[![FlowMate Demo Video](https://img.youtube.com/vi/BJhRhGmDATw/0.jpg)](https://www.youtube.com/watch?v=BJhRhGmDATw)

## How to Use
**FlowMate** is used best during the reconnaissance phase in a security assessment. The following steps explain on how to get started:
1. Load FlowMate into your BurpSuite with a project for your current assessment already created
2. After loading finished add the target application to the BurpSuite internal *Scope*. Only in-scope targets are tracked by FlowMate
3. Activate the detection by checking both boxes on the *Getting Started* tab of FlowMate. You can choose *Live* or *Deferred* matching. *Deferred* is recommended for bigger applications as *Live* matching might slow down the browsing experience if hundreds of parameters are matched against every response.
4. Browse the application following the *General best practices* below
5. Stop the detection before starting manual analysis. This prevents payloads and duplicate values from polluting the graph
6. Profit from the data flow graph created for you!

### General best practices
- Enter *unique* and *long enough* values (generally more than 6 characters) when browsing an application with FlowMate enabled
- Do not enter payloads during this phase
- Browse all user roles and functionality available

### What can you get from the graph?
1. You can lookup in which locations an specific parameter you are testing reappers in the application including the near surrounding of the match giving a first impression on which payloads might be useful for exploitation
2. You can more easily identify occurrences of a parameter in not directly visible places, such as in hidden input fields or when a value is used in resources like stylesheets or scripts for example
3. In conjunction with the session tracking feature you can track cross-session parameter occurrences. In case of attack vectors like Cross-Site Scripting (XSS) this may lead to attacks on higher privileged accounts (privilege escalation, account takeover)
4. If your target application consists of multiple domains, for example APIs and the actual web frontend, the graph helps to detect cross-domain occurrences of parameter matches
5. You can directly identify unsafe behavior of the application from the graph. Some examples include:
   - A user password is included in the applications sources in cleartext
   - Security enhancements such as CSRF tokens are not changed in a secure manner
   
## Installation

### If you want to use a pre-built JAR file, follow these steps
1. Download the latest pre-built jar file from the [Release page](https://github.com/usdAG/FlowMate/releases)
2. Follow the steps to install an extension from JAR file here: [Installing an extension from a file](https://portswigger.net/burp/documentation/desktop/extensions/installing-extensions#installing-an-extension-from-a-file)

### If you want to build it from source, follow these steps
1. Clone the repository, switch into it and run `mvn package`. The `target` folder then contains a built version of FlowMate
2. Follow the steps to install an extension from JAR file here: [Installing an extension from a file](https://portswigger.net/burp/documentation/desktop/extensions/installing-extensions#installing-an-extension-from-a-file)

## Reporting Issues
If you encounter issues with FlowMate please report to us via a GitHub Issue. Error Logs are written to the logfile in `~/.flowmate` folder on your system running the plugin as well as the error log console in the BurpSuite Extension tab ("Extension" Tab > "Installed" Tab > Select "FlowMate" > "Errors" Tab).

Supplying us with this information as well as a short description of when the error occurs helps us to troubleshoot and fix the issue. Furthermore, keeping the BurpState might help if we have questions back to you.
