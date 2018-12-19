## Jira Workflow Council Add-On Integration Tests

The Jira Workflow Council Add-On Integration Tests provide a REST end-point to automatically test the Jira Workflow Council Add-On.

## Why was this add-on created?

This was created to provide a way to automatically test the Jira Workflow Council Add-On in an environment in which the Atlassian-recommended method of downloading & installing a Jira Software instance during a build is not feasible. This has been tested with Jira Software 7.11.0.

## How to use this add-on

* Simply build this add-on using the Atlassian SDK using the following Maven goals: 
```
clean install deploy
```

* Once the JAR file is created, install the add-on in your non-production Jira Software instance. Create a user with the username "jira-workflow-council-user" to be the author of the workflow changes.
* Import the following workflows in your instance.
  * src/test/resources/Workflow Council Int test - Complicated Workflow.xml
  * src/test/resources/Workflow Council Int test - Pre-existing post-function.xml
  * src/test/resources/Workflow Council Int test - Simple Workflow.xml
* Install the Jira Workflow Council Add-On in your non-production Jira Software instance.
* Access the following end-point.

```
<jira base url>/rest/workflowcouncilinttest/1.0/run
```

The output will be a text-based description of test cases and results.
