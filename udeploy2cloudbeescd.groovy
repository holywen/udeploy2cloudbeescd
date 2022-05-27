import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.sample.DslBaseScript

// DslBaseScript encapsulates the magic for invoking dsl scripts
@BaseScript DslBaseScript baseScript

def myProjectName = "Holy Proj"

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
                  switch(componentParameter.type){
                    case "TEXT":
                      formalParameter componentParameter.name, defaultValue: componentParameter.value, {
                        description = componentParameter.description
                        label = componentParameter.label
                        required = componentParameter.required == true? '1':'0';
                        type = 'entry'
                      }
                      break
                    case "SELECT":
                      formalParameter componentParameter.name, defaultValue:  componentParameter.value, {
                        expansionDeferred = '0'
                        label = componentParameter.label
                        def optionsMap = [:]
                        componentParameter.allowedValues.each { allowedValue ->
                          optionsMap[(allowedValue.label)] = allowedValue.value
                        }

                        options = optionsMap
                        required = componentParameter.required == true? '1':'0';
                        type = 'select'
                      }
                      break
                    case "TEXTAREA":
                      formalParameter componentParameter.name, defaultValue: componentParameter.value, {
                        description = componentParameter.description
                        label = componentParameter.label
                        required = componentParameter.required == true? '1':'0';
                        type = 'textarea'
                      }
                      break
                    case "CHECKBOX":
                      formalParameter componentParameter.name, defaultValue: componentParameter.value,{
                        checkedValue = 'true'
                        type = 'checkbox'
                        uncheckedValue = 'false'
                      }
                      break
                    case "SECURE":
                      formalParameter componentParameter.name, defaultValue: null, {
                        label = componentParameter.label
                        required = componentParameter.required == true? '1':'0';
                        type = 'credential'
                      }
                      break
                    default:
                      println "unsupported component parameter type " + componentParameter
                  }

                }

                def componentProcessRootActivity = myUdeployComponenProcess.rootActivity
                def componentProcessEdges = componentProcessRootActivity?.edges
                def componentProcessSteps = componentProcessRootActivity?.children

                //process steps
                componentProcessSteps.each{ compProcessStep ->
                  switch(compProcessStep.type){
                    case "plugin":
                      switch(compProcessStep.pluginName){
                        case "Shell":
                          def useImpersonation = compProcessStep.useImpersonation
                          def allowFailure = compProcessStep.allowFailure
                          def impersonationUsername = compProcessStep.impersonationUsername
                          def impersonationUseSudo = compProcessStep.impersonationUseSudo
                          def commandName = compProcessStep.commandName
                          def stepProperties = compProcessStep.properties
                          def scriptBody = stepProperties.scriptBody
                          def shellInterpreter = stepProperties.shellInterpreter
                          def directoryOffset = stepProperties.directoryOffset
                          //todo: fix impersonation
                          //println "Shell plugin: $commandName ${compProcessStep.name}"

                          processStep compProcessStep.name, {
                            actualParameter = [
                              'commandToRun': scriptBody,
                              'shellToUse': shellInterpreter,
                            ]
                            workingDirectory = directoryOffset
                            errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
                            processStepType = 'command'
                            subprocedure = 'RunCommand'
                            subproject = '/plugins/EC-Core/project'
                          }
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
                              createDummyProcessStep(myUdeployComponentName + ":"  + myUdeployComponenProcess.name, compProcessStep)
                          }
                          break

                        default:
                          createDummyProcessStep(myUdeployComponentName + ":"  + myUdeployComponenProcess.name, compProcessStep)
                      }
                      break
                    case "finish":
                      //do nothing
                      break;
                    case "switch":
                    case "join":

                    default:
                      createDummyProcessStep(myUdeployComponentName + ":"  + myUdeployComponenProcess.name, compProcessStep)
                  }

                }

                //process step dependencies
                componentProcessEdges.each{ edge ->

                }
            }
          }


        }
      }

    }
  }
}


def getComponentPlugin( component ) {
    if(component.sourceConfigPluginName.equals("Maven")){
        return "EC-Maven"
    } else if (component.sourceConfigPluginName.equals("Artifactory")){
        return "EC-Artifactory"
    } else {
        return "EC-Artifact"
    }
}

def createDummyProcessStep(contextPath, dummyStep ){
  println "context path: $contextPath"
  def outputString =  "            unsupported component process step name: ${dummyStep.name} type: ${dummyStep.type}"
  if(dummyStep.type.equals("plugin"))
    outputString +=  "->" + dummyStep.pluginName + ":" + dummyStep.commandName
  println outputString
  processStep dummyStep.name, {
    actualParameter = [
      'commandToRun': 'echo dummyStep',
    ]
    processStepType = 'command'
    subprocedure = 'RunCommand'
    subproject = '/plugins/EC-Core/project'
  }
}