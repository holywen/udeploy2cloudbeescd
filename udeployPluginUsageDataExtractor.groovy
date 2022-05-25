def myProjectName = "Holy Proj"

def myApplicationName = args.name
def myApplicationDesc = args.description
def myAppTier = "Tier 1"

def myComponents = args.components
def plugins = []

myComponents.each { myComponent ->
  def myComponentName = myComponent.name
  def myComponentDesc = myComponent.description
  def myComponentPlugin = getComponentPlugin( myComponent)
  
  //println myComponentPlugin
  def compProcesses = myComponent.processes
  compProcesses.each{ compProcess ->
    def compProcessesSteps = compProcess.rootActivity?.children
    compProcessesSteps.each{ myStep ->
      //println myStep.type
      if(myStep.type.equals("plugin"))
        plugins << myStep.pluginName + ":" + myStep.commandName
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


plugins.unique().sort()
