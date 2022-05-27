gate 'PRE', stageName: args.stageName, pipelineName: args.pipelineName, projectName: args.projectName, {
    task 'approval1', {
        notificationTemplate = 'ec_default_pipeline_manual_task_notification_template'
        taskType = 'APPROVAL'
        approver = [
            'admin'
        ]
    }
}