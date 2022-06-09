def myComponents = args.components
def plugins = []

myComponents.each { myComponent ->
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

plugins.unique().sort()
