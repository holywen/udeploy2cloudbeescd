import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.sample.DslBaseScript

// DslBaseScript encapsulates the magic for invoking dsl scripts
@BaseScript DslBaseScript baseScript

def myConfig = readConfigFile()
def myProjectName = myConfig.projectName

def myApplicationName = args.name
def myApplicationDesc = args.description
def myAppTier = "Tier 1"

def myUdeployEnvironments = args.environments
InputStream stream = getScriptClassLoader().getResourceAsStream("targetEnvironment.txt")
def targetEnvironment = stream?.text.trim()

myUdeployEnvironments.each { myUdeployEnvironment ->
  def myUdeployEnvironmentName = myUdeployEnvironment.name
  def myEnvironmentDesc = myUdeployEnvironment.description

  //environment
  if(myUdeployEnvironmentName.equals(targetEnvironment)) {
    println "processing environment $myUdeployEnvironmentName"

    environment myUdeployEnvironmentName, {
      projectName = myProjectName

      //properties
      loadPropertySheet(myUdeployEnvironment.propSheet?.name, myUdeployEnvironment.propSheet?.properties)

      //component properties sheets
      def componentPropSheets = myUdeployEnvironment.componentPropSheets
      componentPropSheets.each{ componentPropSheet ->
        loadPropertySheet(componentPropSheet.componentName, componentPropSheet.properties)
      }

      environmentTier myAppTier, {
        resourceName = []
      }
    }

    application myApplicationName, {
      description = myApplicationDesc
      applicationType = 'traditional'
      projectName = myProjectName

      tierMap myApplicationName + "-" + myUdeployEnvironmentName, {
        applicationName = myApplicationName
        environmentName = myUdeployEnvironmentName
        environmentProjectName = myProjectName
        }
    }
  }
}

[]


