def myProjectName = "Holy Proj"

def myApplicationName = args.name
def myApplicationDesc = args.description
def myAppTier = "Tier 1"

def myComponents = args.components

application myApplicationName, {
  description = myApplicationDesc
  applicationType = 'traditional'
  projectName = myProjectName

   
  applicationTier myAppTier, {
    description = 'auto generated application tier'
    applicationName = myApplicationName

    //components
    myComponents.each { myComponent ->
      def myComponentName = myComponent.name
      def myComponentDesc = myComponent.description
      def myComponentPlugin = getComponentPlugin( myComponent )
      
      //component
      component myComponentName, pluginName: null, {
        description = myComponentDesc
        pluginKey = myComponentPlugin
        

        //component processes
        def myComponentProcesses = myComponent.processes
        myComponentProcesses.each{ myComponentProcess ->
          process myComponentProcess.name, {
              processType = 'DEPLOY'

              //process parameters
              def componentProcessParams = myComponentProcess.propDefs
              componentProcessParams.each{ componentParameter ->
                switch(componentParameter.type){
                  case "TEXT":
                    formalParameter componentParameter.name, defaultValue: componentParameter.value, {
                      description = componentParameter.description
                      label = componentParameter.label
                      required = componentParameter.required == true? '1':'0';
                      type = 'entry'
                    }
                    break;
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
                    break;
                  default:
                    println "unsupported component parameter type " + componentParameter.type
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