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

    myComponents.each { myComponent ->
      def myComponentName = myComponent.name
      def myComponentDesc = myComponent.description
      def myComponentPlugin = getComponentPlugin( myComponent)
      
      component myComponentName, pluginName: null, {
        description = myComponentDesc
        pluginKey = myComponentPlugin
        

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