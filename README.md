# udeploy2cloudbeescd
Convert udeploy exported json file to cloudbees CD DSL
## Usage
1. export application json file from Udeploy, save it to path/to/<appname>.json
2. run ectool --server <your CD server host name> login <your user name>
3. run ./udeploy2cloudbeescd.sh <CD project name> <path/to/appname.json>
    * example: ./udeploy2cloudbeescd.sh "Holy Proj" top10UdeployApps/COG-Tech-CMD-PTR-Dory.json

## supported features
  1. Import application and components, generate application and components process
### Details:
  1. Application processes
        * application process propDefs (parameters)
        * Component Process invoke (componentProcess)
        * Component Process invoke wrapper
            * "allVersionsIterator" place holder
            * "inventoryVersionDiff" place holder
            * "componentEnvironmentIterator" place holder
            * "configurationDiff" place holder

        * process steps dependencies (rootActivity.edges)
        * process steps (rootActivity.children)
  2. Components processes
        * Component process propDefs (parameters)
        * process steps dependencies (rootActivity.edges)
        * process steps (rootActivity.children)
            * type:plugin calling steps place holder
            * type:switch node place holder
            * type:join node place holder
  3. plugins
        1. Shell
            * Shell

## todo
  1. Application properties
  2. Application processes
        * properties
        * Component Process invoke wrapper
            * "allVersionsIterator"
            * "inventoryVersionDiff"
            * "componentEnvironmentIterator"
            * "configurationDiff"
  3. Component definition (artifacts id etc)
  4. Component properties
  5. Components processes
        * properties
        * process steps dependencies (rootActivity.edges)
            * type:switch node logic
                * environment switch
                * property switch
            * type:join node logic (maybe not needed)
  6. Environment
        * Environment properties
  6. plugins (most used ones from the top 10 apps)
        1. File Utils
            *  "Copy Directory"
            *   "Create Directories"
            *   "Create File"
            *   "Delete Files and Directories“
            *   "Flip Line Endings"
            *   "Move Directory"
            *   "Replace Tokens"
            *   "Untar Tarball"
            *   "Unzip"
            *   "Update XML File with XPath"
        2. Groovy:
            * "Run Groovy Script"
        3. Linux System Tools:
            * "Set file permissions"
        4. "UrbanCode Deploy Configuration Management“
            * "Install Template"
        5. "UrbanCode Deploy Versioned File Storage"
            * "Download Artifacts"
        6. General Utilities
            * Wait
        7. Service Control Manager:
            * Check If Service Exists
            * Delete Service
            * Disable Service
            * Enable Service"
            * Start Service"
            * Stop Service"