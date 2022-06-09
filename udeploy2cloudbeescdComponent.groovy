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

                def finishStepNodeName = componentProcessSteps.find{ it.type == 'finish'}.name

                //process steps
                componentProcessSteps.each{ compProcessStep ->
                  switch(compProcessStep.type){
                    case "plugin":
                      switch(compProcessStep.pluginName){
                        case "Shell":
                          createShellStep(compProcessStep)
                          break
                        case "Groovy":
                          createGroovyStep(compProcessStep)
                          break
                        case "File Utils":
                          switch(compProcessStep.commandName){
                            case "Copy Directory":
                              createFileUtilCopyDirectoryStep(compProcessStep)
                              break
                            case "Create Directories":
                              createFileUtilCreateDirectoryStep(compProcessStep)
                              break
                            case "Create File":
                              createFileUtilCreateFileStep(compProcessStep, componentProcessEdges)
                              break
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
                        case "UrbanCode Deploy Versioned File Storage":
                          switch(compProcessStep.commandName){
                            case "Download Artifacts":
                              createRetrieveArtifactStep(compProcessStep, myComponentPlugin, myUdeployComponentName)
                              break
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
