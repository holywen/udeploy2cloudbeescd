import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.sample.DslBaseScript

// DslBaseScript encapsulates the magic for invoking dsl scripts
@BaseScript DslBaseScript baseScript

def myConfig = readConfigFile()
def myProjectName = myConfig.projectName

def myApplicationName = args.name
def myApplicationDesc = args.description
def myAppTier = "Tier 1"

def myUdeployComponents = args.components
InputStream stream = getScriptClassLoader().getResourceAsStream("targetComponent.txt")
def targetComponent = stream?.text.trim()

application myApplicationName, {
  description = myApplicationDesc
  applicationType = 'traditional'
  projectName = myProjectName


  applicationTier myAppTier, {
    description = 'auto generated application tier'
    applicationName = myApplicationName

    //components
    myUdeployComponents.each { myComponent ->
      def myUdeployComponentName = myComponent.name
      def myComponentDesc = myComponent.description
      def myComponentPlugin = getComponentPlugin( myComponent )

      //component
      if(myUdeployComponentName.equals(targetComponent)) {
        println "processing component $myUdeployComponentName"
        component myUdeployComponentName, pluginName: null, {
          description = myComponentDesc
          pluginKey = myComponentPlugin

          def componentPropSheets = myComponent.propSheetGroup.propSheets

          //component identification
          switch(myComponentPlugin){
            case "EC-Maven":
              def mavenPropDefs = componentPropSheets.find{it.name == 'MavenComponentProperties'}.properties
              setMavenComponentID(mavenPropDefs)
              break;
            case "EC-Artifactory":
            case "EC-Artifact":
              println "unsupported component identification type: $myComponentPlugin"
          }

          //component properties
          componentPropSheets.each{ componentPropSheet ->
            loadPropertySheet(componentPropSheet.name, componentPropSheet.properties)
          }

          //component processes
          def myUdeployComponenProcesses = myComponent.processes
          myUdeployComponenProcesses.each{ myUdeployComponenProcess ->
            process myUdeployComponenProcess.name, {
                processType = 'DEPLOY'

                //process parameters
                def componentProcessParams = myUdeployComponenProcess.propDefs
                componentProcessParams.each{ componentParameter ->
                  createFormalParameter(componentParameter)
                }

                def componentProcessRootActivity = myUdeployComponenProcess.rootActivity
                def componentProcessEdges = componentProcessRootActivity?.edges
                def componentProcessSteps = componentProcessRootActivity?.children

                def finishStepNodeNames = componentProcessSteps.findAll{ it.type == 'finish'}?.collect { it.name }
                // println "finishStepNodeNames: " + finishStepNodeNames

                //process steps
                componentProcessSteps.each{ compProcessStep ->
                  createComponentProcessStep(compProcessStep, componentProcessEdges, myUdeployComponentName, myUdeployComponenProcess.name, myComponentPlugin)
                }

                //process step dependencies
                if ( finishStepNodeName != null ) {
                  convertSwitchNodesEdges(componentProcessSteps, componentProcessEdges)
                  componentProcessEdges.each{ edge ->
                    if(edge.from != null && !finishStepNodeNames.contains(edge.to)){
                      processDependency edge.from , targetProcessStepName: edge.to, {
                        branchType = convertBranchType(edge.type)
                        if(edge.type == "VALUE"){
                          // println "edge.branchCondition: " + edge.branchCondition
                          // println "edge.branchConditionName: " + edge.branchConditionName
                          branchCondition = edge.branchCondition
                          branchConditionName = edge.branchConditionName
                          branchConditionType = 'CUSTOM'
                        }
                      }
                    }
                  }
                }
            }
          }
        }
      }
    }
  }
}
