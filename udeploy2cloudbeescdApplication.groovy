import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.sample.DslBaseScript

// DslBaseScript encapsulates the magic for invoking dsl scripts
@BaseScript DslBaseScript baseScript

def myConfig = readConfigFile()
def myProjectName = myConfig.projectName

def myApplicationName = args.name
def myApplicationDesc = args.description
def myUdeployApplicationProcesses = args.processes
def myAppTier = "Tier 1"

application myApplicationName, {
  description = myApplicationDesc
  applicationType = 'traditional'
  projectName = myProjectName

  //application processes
  myUdeployApplicationProcesses.each{ myUdeployApplicationProcess ->
    process myUdeployApplicationProcess.name, {
        processType = 'DEPLOY'

        //process parameters
        def applicationProcessParams = myUdeployApplicationProcess.propDefs
        applicationProcessParams.each{ applicationParameter ->
          createFormalParameter(applicationParameter)
        }

        //process steps
        def applicationProcessRootActivity = myUdeployApplicationProcess.rootActivity
        def applicationProcessEdges = applicationProcessRootActivity?.edges
        def applicationProcessSteps = applicationProcessRootActivity?.children

        def finishStepNodeName = applicationProcessSteps.find{ it.type == 'finish'}?.name
        def processStepNameMap = [:]

        applicationProcessSteps.each{ appProcessStep ->
          switch(appProcessStep.type){
            case "componentEnvironmentIterator":
              updateStepNameMapping(processStepNameMap, appProcessStep)
              def componentProcessStepInvokeData = getComponentProcessStepInvokeData(appProcessStep)
              createComponentProcessInvokeStep(componentProcessStepInvokeData, myApplicationName, myAppTier)
              break
            case "componentProcess":
              createComponentProcessInvokeStep(appProcessStep, myApplicationName,  myAppTier)
              break
            case "plugin":
              switch(appProcessStep.pluginName){
                case "Shell":
                  createShellStep(appProcessStep)
                  break
                case "File Utils":
                  switch(appProcessStep.commandName){
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
                      createDummyAppProcessStep(myApplicationName + ":"  + myUdeployApplicationProcess.name, appProcessStep, myAppTier)
                  }
                  break

                default:
                  createDummyAppProcessStep(myApplicationName + ":"  + myUdeployApplicationProcess.name, appProcessStep, myAppTier)
              }
              break
            case "finish":
              //do nothing
              break;
            case "switch":
            case "join":
            default:
              createDummyAppProcessStep(myApplicationName + ":"  + myUdeployApplicationProcess.name, appProcessStep, myAppTier)
          }
        }

        println(processStepNameMap)

    }
  }

}

def getComponentProcessStepInvokeData(appProcessStep){
  if(appProcessStep.children.first().type == "componentProcess"){
    return appProcessStep.children.first()
  } else {
    return appProcessStep.children.first().children.first()
  }
}

def updateStepNameMapping( nameMap, processStepData){
  def keyName = processStepData.name
  def realName = processStepData.name
  def stepChild = processStepData.children.first()
  switch(stepChild.type){
    case "allVersionsIterator":
    case "inventoryVersionDiff":
    case "componentEnvironmentIterator":
    case "configurationDiff":
      realName = stepChild.children.first().name
      break
    case "componentProcess":
      realName = stepChild.name
      break
    default:
      println "unsupported process step type $stepChild"
  }
  nameMap << [ (keyName):(realName) ]
}