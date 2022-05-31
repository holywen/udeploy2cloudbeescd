import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.sample.DslBaseScript

// DslBaseScript encapsulates the magic for invoking dsl scripts
@BaseScript DslBaseScript baseScript

def myProjectName = "Holy Proj"

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

        def finishStepNodeName = applicationProcessSteps.find{ it.type == 'finish'}.name


    }
  }

}