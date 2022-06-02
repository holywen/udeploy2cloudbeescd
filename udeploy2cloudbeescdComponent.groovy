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

                def finishStepNodeName = componentProcessSteps.find{ it.type == 'finish'}.name

                //process steps
                componentProcessSteps.each{ compProcessStep ->
                  switch(compProcessStep.type){
                    case "plugin":
                      switch(compProcessStep.pluginName){
                        case "Shell":
                          createShellStep(compProcessStep)
                          break
                        case "File Utils":
                          switch(compProcessStep.commandName){
                            case "Copy Directory":
                            case "Create Directories":
                            case "Create File":
                            case "Delete Files and Directories":
                            case "Flip Line Endings":
                            case "Move Directory":
                            case "Replace Tokens":
                            case "Untar Tarball":
                            case "Unzip":
                            case "Update XML File with XPath":
                            default:
                              createDummyCompProcessStep(myUdeployComponentName + ":"  + myUdeployComponenProcess.name, compProcessStep)
                          }
                          break

                        default:
                          createDummyCompProcessStep(myUdeployComponentName + ":"  + myUdeployComponenProcess.name, compProcessStep)
                      }
                      break
                    case "finish":
                      //do nothing
                      break;
                    case "switch":
                    case "join":
                    default:
                      createDummyCompProcessStep(myUdeployComponentName + ":"  + myUdeployComponenProcess.name, compProcessStep)
                  }

                }

                //process step dependencies
                if ( finishStepNodeName != null ) {
                  componentProcessEdges.each{ edge ->
                    if(edge.from != null && !edge.to.equals(finishStepNodeName)){
                      processDependency edge.from , targetProcessStepName: edge.to, {
                        //todo: need to handle the "VALUE" type correctly
                        branchType = edge.type == "VALUE" ? "ALWAYS":edge.type
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