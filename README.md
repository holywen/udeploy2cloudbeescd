# udeploy2cloudbeescd
Convert udeploy exported json file to cloudbees CD DSL
## Prerequisites
1. User need to install ectool jq
2. User need to have connectivity to the cloudbees CD server (8443 port)
3. The .sh scripts are runnign in bash

## Usage
1. export application json file from Udeploy, save it to path/to/\<appname>.json
2. run
```
ectool --server <your CD server host name> login <your user name>
```
3. run
```
./udeploy2cloudbeescd.sh <CD project name> <path/to/appname.json>
```
   * example:
```
./udeploy2cloudbeescd.sh "Holy Proj" top10UdeployApps/Example-App.json
```

## supported features
  1. Import application and components, generate application and components process
### Details:
  1. Application properties
  2. Application processes
        * application process propDefs (parameters)
        * Component Process invoke (componentProcess)
        * Application process Manual Task (applicationManualTask)
        * Component Process invoke wrapper
            * "allVersionsIterator" place holder
            * "inventoryVersionDiff" place holder
            * "componentEnvironmentIterator" place holder
            * "configurationDiff" place holder

        * process steps dependencies (rootActivity.edges)
        * process steps (rootActivity.children)
  2. Component definition (artifacts id etc)
  3. Component properties
  4. Components processes
        * Component process propDefs (parameters)
        * process steps dependencies (rootActivity.edges)
            * type:switch node logic
                * environment switch
                * property switch
            * type:join node logic
        * process steps (rootActivity.children)
            * type:plugin calling steps place holder
            * type:switch node place holder
            * type:join node place holder
  4. Environment
        * Environment properties
  5. plugins
        1. Shell
            * Shell
        2. Groovy:
            * "Run Groovy Script"
        5. "UrbanCode Deploy Versioned File Storage"
            * "Download Artifacts"
        1. File Utils
            *   "Copy Directory"
            *   "Create Directories"
            *   "Create File"
            *   "Delete Files and Directories???
             (Note: conversion is not working if there are multiple files need to be deleted or wild cards '*' is used.)
            *   "Unzip"
            *   "Untar Tarball"
            *   "Move Directory"
        3. Linux System Tools:
            * "Set file permissions"
             (Note: conversion is not working if there are multiple files need to be updated or wild cards '*' is used.)
        6. General Utilities
            * Wait
        7. Service Control Manager:
            * Check If Service Exists
            * Delete Service
            * Disable Service
            * Enable Service"
            * Start Service"
            * Stop Service"
  6. property substitution
        * `${p:environment/propertyName}` -> `$[/myEnvironment/propertyName]`
        * `${p:environment.propertyName}` -> `$[/myEnvironment/propertyName]`
        * `${p:resource/propertyName}` -> `$[/myResource/propertyName]`
        * `${p:resource.propertyName}` -> `$[/myResource/propertyName]`
        * `${p:component/propertyName}` -> `$[/myComponent/propertyName]`
        * `${p:component.propertyName}` -> `$[/myComponent/propertyName]`
        * `${p:app/propertyName}` -> `$[/myApplication/propertyName]`
        * `${p:app.propertyName}` -> `$[/myApplication/propertyName]`
        * `${p:propertyName}` -> `$[propertyName]`
        * `${p:propertyName}` -> `$[propertyName]`
        * `${propertyName}` -> `$[propertyName]`
        * `${propertyName}` -> `$[propertyName]`
## todo
  1. Application processes
        * Component Process invoke wrapper
            * "allVersionsIterator"
            * "inventoryVersionDiff"
            * "componentEnvironmentIterator"
            * "configurationDiff"
  3. Components processes
        * impersonation in process steps
  5. plugins (most used ones from the top 10 apps)
        1. File Utils
            *   "Flip Line Endings"
            *   "Replace Tokens"
            *   "Update XML File with XPath"
        4. "UrbanCode Deploy Configuration Management???
            * "Install Template"
