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

