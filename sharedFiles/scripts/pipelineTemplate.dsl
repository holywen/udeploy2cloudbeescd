project args.projectName, {

  pipeline args.pipelineName, {

    stage 'Stage 1', {

      task 'Task 1', {
        description = ''
        actualParameter = [
          'commandToRun': "echo 'do something'",
          'shellToUse': 'sh',
        ]
      }

      task 'Task 2', {
        actualParameter = [
          'commandToRun': "echo 'do another task'",
          'shellToUse': 'sh',
        ]
      }
    }
  }
}
