package com.electriccloud.commander.dsl.sample

import org.codehaus.groovy.control.CompilerConfiguration
import com.electriccloud.commander.dsl.DslDelegate
import com.electriccloud.commander.dsl.DslDelegatingScript
import java.util.logging.Logger
import groovy.json.*

abstract class DslBaseScript extends DslDelegatingScript {

  private final static Logger logger = Logger.getLogger("")

  /**
   * Utility method for creating a pipeline based on the pre-defined template
   */
  def createPipelineFromTemplate(String projectName, String pipelineName) {
    evalDslScript('scripts/pipelineTemplate.dsl',
        [projectName: projectName, pipelineName: pipelineName])
  }

  /**
   * Utility method for adding pre gate approval task to the given
   * pipeline stage
   */
  def addPreGateApprovalTask(String projectName, String pipelineName, String stageName) {
    evalDslScript('scripts/pregateApprovalTask.dsl',
        [projectName: projectName, pipelineName: pipelineName, stageName: stageName])
  }

  def addFormalParameter(def args=[:]){
    def evalResult = evalDslScript('scripts/formalParameter.dsl', args)
  }

  // boiler-plate code to evaluate dsl scripts from within dsl
  def evalDslScript(String dslFile, def args=[:]) {
    println ("Executing dsl file:" + dslFile + " with args: " + args)
    // Find file in classpath
    InputStream stream = this.scriptClassLoader
        .getResourceAsStream(dslFile)
    def dslScript = stream?.text

    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setScriptBaseClass(DelegatingScript.class.getName());
    GroovyShell sh = new GroovyShell(this.scriptClassLoader, cc);
    DelegatingScript script = (DelegatingScript)sh.parse(dslScript)
    script.setDelegate(this.delegate);
    script.binding = new Binding(args: args)
    return script.run();

  }

  def readConfigFile(){
    def jsonSlurper = new JsonSlurper()
    def configsJson = scriptClassLoader.getResourceAsStream("config.json").text.trim()
    def config = jsonSlurper.parseText(configsJson)
    return config
  }

  /**
   * Intercept the DslDelegate so it can be set as the delegate on the
   * dsl scripts being evaluated in context of the parent dsl script.
   * Before setting the delegate, also capture the script's class loader
   * before the dslDelegate hijacks the calls. This is needed to get the
   * reference to the groovy class loader used for evaluating the DSL
   * script passed in to <code>evalDslScript</code>.
   */
  private def delegate;
  private def scriptClassLoader;

  void setDelegate(DslDelegate delegate) {
    this.scriptClassLoader = this.class.classLoader
    this.delegate = delegate;
    super.setDelegate(delegate)
  }

  def getScriptClassLoader(){
    return this.scriptClassLoader
  }
    // end boiler-plate

  def loadPropertySheet(sheetPath, args) {
    def sheetFullPath = ( sheetPath == null) ? "" : sheetPath + "/"
    //println "loadPropertySheet -> " + sheetFullPath + " : " +  args
    args.each{ propItem ->
      property sheetFullPath + propItem.name, value: propItem.value, {
        description =  propItem.description
      }
    }
  }

  def createFormalParameter(def args) {
    switch(args.type){
      case "TEXT":
        formalParameter args.name, defaultValue: args.value, {
        description = args.description
        label = args.label
        required = args.required == true? '1':'0';
        type = 'entry'
        }
        break
      case "SELECT":
        formalParameter args.name, defaultValue:  args.value, {
        expansionDeferred = '0'
        label = args.label
        def optionsMap = [:]
        args.allowedValues.each { allowedValue ->
          optionsMap[(allowedValue.label)] = allowedValue.value
        }

        options = optionsMap
        required = args.required == true? '1':'0';
        type = 'select'
        }
        break
      case "TEXTAREA":
        formalParameter args.name, defaultValue: args.value, {
        description = args.description
        label = args.label
        required = args.required == true? '1':'0';
        type = 'textarea'
        }
        break
      case "CHECKBOX":
        formalParameter args.name, defaultValue: args.value,{
        checkedValue = 'true'
        type = 'checkbox'
        uncheckedValue = 'false'
        }
        break
      case "SECURE":
        formalParameter args.name, defaultValue: null, {
        label = args.label
        required = args.required == true? '1':'0';
        type = 'credential'
        }
        break
      default:
        println "createFormalParameter -> unsupported component parameter type " + args
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

  def setMavenComponentID(args){
    // println "setMavenComponentID -> " + args
    def groupId = args.find{it.name == "groupId"}.value
    def artifactId = args.find{it.name == "artifactId"}.value
    def qualifier = args.find{it.name ==  "qualifier"}.value
    def extension = args.find{it.name ==  "extension"}.value
    def repoUrl = args.find{it.name ==  "repoUrl"}.value
    repoUrl = repoUrl.endsWith("/") ? repoUrl.substring(0,repoUrl.lastIndexOf('/')): repoUrl

    def repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1)
    def serverUrl = repoUrl.substring(0, repoUrl.lastIndexOf('/'))
    // println " serverURL: ${serverUrl} repoName: ${repoName}"
    property 'ec_content_details', {
      property 'artifact', value: "${groupId}:${artifactId}"
      classifier = qualifier
      property 'pluginProjectName', value: 'EC-Maven'
      property 'repository', value: repoName
      property 'server', value: serverUrl
      property 'type', value: extension
      property 'overwrite', value: '1'
      pluginProcedure = 'Retrieve Artifact'
      resultProperty = '/myJob/retrievedArtifactVersions/$[assignedResourceName]'
    }
  }

  def createComponentProcessInvokeStep(args, appName, appTier){
    def allowFailure = args.allowFailure
    // println "process step creation: $args" 
    processStep args.name, {
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      applicationTierName = appTier
      processStepType = 'process'
      subcomponent = args.componentName
      subcomponentApplicationName = appName
      subcomponentProcess = args.componentProcessName
    }
  }

  def createFileUtilCopyDirectoryStep(args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def sourceDir = stepProperties.sourceDir
    def destDir = stepProperties.destDirList
    def force = stepProperties.force
    processStep args.name, {
      actualParameter = [
        'destinationFile': destDir,
        'replaceDestinationIfPreexists': (force == "true") ? '1' : '0',
        'sourceFile': sourceDir,
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'Copy'
      subproject = '/plugins/EC-FileOps/project'
    }
  }

  def createFileUtilDeleteFilesandDirectoriesStep(args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def includes = stepProperties.includes
    def excludes = stepProperties.excludes
    def baseDir = stepProperties.baseDir

    def procedureToCall = includes == "**/*" ? "DeleteDirectory" : "DeleteFile"
    processStep args.name, {
      if(procedureToCall == "DeleteDirectory"){
        actualParameter = [
          'Path': baseDir,
          'Recursive': '1',
        ]
      } else {
        actualParameter = [
          'Path': baseDir + includes,
        ]
      }
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = procedureToCall
      subproject = '/plugins/EC-FileOps/project'
    }
  }

  def createLinuxSystemToolsSetFilePermissionsStep(args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def file = stepProperties.includes
    def mode = stepProperties.mod

    processStep args.name, {
      actualParameter = [
        'Mode': mode,
        'Owner': '',
        'Path': file,
        'Recursive': '1',
        'Verbose': '1',
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'ChangeOwnershipOrPermissions'
      subproject = '/plugins/EC-FileOps/project'
    }
  }

  def createFileUtilMoveDirectoryStep(args) {
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def sourceDir = stepProperties.sourceDir
    def includes = stepProperties.includes
    def destDir = stepProperties.destDir
    def force = stepProperties.force
    processStep args.name, {
      actualParameter = [
        'destinationFile': destDir,
        'replaceDestinationIfPreexists': (force == "true") ? '1' : '0',
        'sourceFile': sourceDir + "/" + includes,
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'Move'
      subproject = '/plugins/EC-FileOps/project'
    }
  }

  def createFileUtilUnzipStep(args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def destDir = stepProperties.dir
    def zipFile = stepProperties.zip

    processStep args.name, {
      actualParameter = [
            'destinationDir': destDir,
            'zipFile': zipFile,
          ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'Unzip File'
      subproject = '/plugins/EC-FileOps/project'
    }
  }

  def createFileUtilUntarTarballStep(args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def destDir = stepProperties.dir.toString()
    def tarFile = stepProperties.tarball.toString()
    def compression = stepProperties.compression
    def overwrite = stepProperties.overwrite

    processStep args.name, {
      actualParameter = [
        'commandToRun': "tar xvf '" + tarFile + "' --directory '" + destDir + "'",
        'shellToUse': '',
      ]
      workingDirectory = ''
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'command'
      subprocedure = 'RunCommand'
      subproject = '/plugins/EC-Core/project'
    }
  }

  def createFileUtilCreateDirectoryStep(args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def dir = stepProperties.dir
    processStep args.name, {
      actualParameter = [
        'Path': dir,
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'CreateDirectory'
      subproject = '/plugins/EC-FileOps/project'
    }
  }

  def createFileUtilCreateFileStep(args, activityEdges){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def file = stepProperties.file
    def contents = stepProperties.contents
    def overwrite = stepProperties.overwrite
    def customEncoding = stepProperties.customEncoding

    processStep args.name, {
      actualParameter = [
        'Mode': '0600',
        'Name': file,
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'CreateEmptyFile'
      subproject = '/plugins/EC-FileOps/project'
    }

    def newStepName = args.name + "_AddContentToFile"
    processStep newStepName, {
      actualParameter = [
        'AddNewLine': '1',
        'Append': (overwrite == "true") ? '0' : '1',
        'Content': contents,
        'Path': file,
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'plugin'
      subprocedure = 'AddTextToFile'
      subproject = '/plugins/EC-FileOps/project'
    }

    insertNodeAfter(activityEdges, args.name, newStepName)
  }

  def insertNodeAfter(activityEdges, nodeName, newNodeName){
    //println "edges before update:" + activityEdges
    def currentEdge = activityEdges.find{it.from == nodeName}
    def currentToNode = currentEdge.to

    activityEdges.removeIf{it.from == nodeName}
    activityEdges << ["from":nodeName, "to": newNodeName, "type": currentEdge.type, "value": currentEdge.value] 
    activityEdges << ["from":newNodeName, "to": currentToNode, "type": "SUCCESS", "value": ""]
    //println "update edges:" + activityEdges
  }

  def createRetrieveArtifactStep(args, myComponentPlugin, myComponentName){
    switch(myComponentPlugin){
      case "EC-Maven":
        processStep args.name, {
          actualParameter = [
            'artifact': '$[/myComponent/ec_content_details/artifact]',
            'classifier': '$[/myComponent/ec_content_details/classifier]',
            'config': '$[/myComponent/ec_content_details/config]',
            'directory': '$[/myComponent/ec_content_details/directory]',
            'overwrite': '$[/myComponent/ec_content_details/overwrite]',
            'repository': '$[/myComponent/ec_content_details/repository]',
            'resultProperty': '$[/myComponent/ec_content_details/resultProperty]',
            'server': '$[/myComponent/ec_content_details/server]',
            'type': '$[/myComponent/ec_content_details/type]',
            'version': '$[/myJob/ec_' + myComponentName +'-version]',
          ]
          processStepType = 'component'
          subprocedure = 'Retrieve Artifact'
          subproject = '/plugins/EC-Maven/project'
        }
        break
      case "EC-Artifact":
        processStep args.name, {
          actualParameter = [
            'artifactName': '$[/myComponent/ec_content_details/artifactName]',
            'artifactVersionLocationProperty': '$[/myComponent/ec_content_details/artifactVersionLocationProperty]',
            'filterList': '$[/myComponent/ec_content_details/filterList]',
            'overwrite': '$[/myComponent/ec_content_details/overwrite]',
            'retrieveToDirectory': '$[/myComponent/ec_content_details/retrieveToDirectory]',
            'versionRange': '$[/myJob/ec_' + myComponentName +'-version]',
          ]
          processStepType = 'component'
          subprocedure = 'Retrieve'
          subproject = '/plugins/EC-Artifact/project'
        }
        break
      case "EC-Artifactory":
        //todo implement EC-Artifactory
      default:
        createDummyCompProcessStep("createRetrieveArtifactStep ->", args)
        break
    }
  }

  def createGeneralUtilitiesWaitStep(def args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def duration = stepProperties.duration

    processStep args.name, {
      actualParameter = [
        'commandToRun': "println 'Start sleeping'\nsleep " + duration + " * 1000\nprintln 'Finished sleeping'",
        'shellToUse': 'ec-groovy',
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'command'
      subprocedure = 'RunCommand'
      subproject = '/plugins/EC-Core/project'
    }
  }

  def createGroovyStep(def args){
    def allowFailure = args.allowFailure
    def stepProperties = args.properties
    def scriptBody = stepProperties.scriptBody
    //println "Groovy plugin: $commandName ${args.name}"
    processStep args.name, {
      actualParameter = [
        'commandToRun': scriptBody,
        'shellToUse': 'ec-groovy',
      ]
      errorHandling = (allowFailure == true) ? 'failProcedure' : 'abortJob'
      processStepType = 'command'
      subprocedure = 'RunCommand'
      subproject = '/plugins/EC-Core/project'
    }
  }

  def createShellStep(def args){
    def useImpersonation = args.useImpersonation
    def allowFailure = args.allowFailure
    def impersonationUsername = args.impersonationUsername
    def impersonationUseSudo = args.impersonationUseSudo
    def commandName = args.commandName
    def stepProperties = args.properties
    def scriptBody = stepProperties.scriptBody
    def shellInterpreter = stepProperties.shellInterpreter
    def directoryOffset = stepProperties.directoryOffset
    //todo: fix impersonation
    //println "Shell plugin: $commandName ${args.name}"

    processStep args.name, {
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
  }

  def convertBranchType(def branchType){
    switch(branchType){
      case "VALUE":
        return "ALWAYS"
      case "FAILURE":
        return "ERROR"
      default:
        return branchType
    }
  }

  def createDummyCompProcessStep(contextPath, dummyStep ){
    createDummyAppProcessStep(contextPath, dummyStep, null)
  }

  def createDummyAppProcessStep(contextPath, dummyStep, appTierName){
    println "context path: $contextPath"
    def outputString =  "createDummyAppProcessStep -> unsupported component process step name: ${dummyStep.name} type: ${dummyStep.type}"
    if(dummyStep.type.equals("plugin"))
      outputString +=  "->" + dummyStep.pluginName + ":" + dummyStep.commandName
    println outputString
    processStep dummyStep.name, {
      actualParameter = [
      'commandToRun': 'echo dummyStep',
      ]
      if(appTierName != null){
        applicationTierName = appTierName
      }
      processStepType = 'command'
      subprocedure = 'RunCommand'
      subproject = '/plugins/EC-Core/project'
    }
  }
}

