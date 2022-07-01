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
InputStream stream = getScriptClassLoader().getResourceAsStream("targetProcess.txt")
def targetProcess = stream?.text.trim()

application myApplicationName, {
  description = myApplicationDesc
  applicationType = 'traditional'
  projectName = myProjectName

  //properties
  loadPropertySheet(args.propSheet?.name, args.propSheet?.properties)

  //application processes
  myUdeployApplicationProcesses.each{ myUdeployApplicationProcess ->
    if(myUdeployApplicationProcess.name.equals(targetProcess)){
      println "processing process ${myUdeployApplicationProcess.name}"
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
            if(appProcessStep.type == 'componentEnvironmentIterator'){
              updateStepNameMapping(processStepNameMap, appProcessStep)
            }
          }

          applicationProcessSteps.each{ appProcessStep ->
            createApplicationProcessStep(appProcessStep, myApplicationName, myUdeployApplicationProcess.name, myAppTier)
          }

          // process step dependencies
          // println("" + processStepNameMap)
          if ( finishStepNodeName != null ) {
            convertSwitchNodesEdges(applicationProcessSteps, applicationProcessEdges)
            applicationProcessEdges.each{ edge ->
              if(edge.from != null && !edge.to.equals(finishStepNodeName)){
                processDependency getRealNameFromMapping(processStepNameMap, edge.from), targetProcessStepName: getRealNameFromMapping(processStepNameMap, edge.to), {
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

def getRealNameFromMapping( nameMap, keyName){
  return nameMap[keyName]?:keyName
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
      println "updateStepNameMapping -> unsupported process step type $stepChild"
  }
  nameMap << [ (keyName):(realName) ]
}