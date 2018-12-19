package com.vanguard.jira.workflowcouncil.inttest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.vanguard.jira.workflowcouncil.domain.ResolutionService;
import com.vanguard.jira.workflowcouncil.domain.ResolvedWorkflowService;

import static com.vanguard.jira.workflowcouncil.domain.ResolutionService.*;
import static com.vanguard.jira.workflowcouncil.domain.ResolvedWorkflowService.*;

@Path("/run")
public class ResolutionServiceIntTest 
{
	private static final String WONT_DO_RESOLUTION = "10100";

	@Inject
	private ResolutionService objectUnderTest;
	
	@JiraImport
	@Inject
	private WorkflowManager wfManager;
	
    @JiraImport
    @Inject
    private UserManager userManager;

    @GET
    @AnonymousAllowed
    @Produces({MediaType.TEXT_PLAIN})
    public Response runAllIntTests()
    {
    	StringBuffer responseString = new StringBuffer();
    	
    	responseString.append(testWorkflowIsUpdated("Simple Workflow Is Updated Correctly", "Workflow Council Int test - Simple Workflow"));
    	responseString.append(testWorkflowIsUpdated("Complicated Workflow Is Updated Correctly", "Workflow Council Int test - Complicated Workflow"));
    	responseString.append(testExistingPostFunctionIsNotUpdated());
    	
    	return Response.ok(responseString.toString()).build();
    }
    
    private StringBuffer testExistingPostFunctionIsNotUpdated()
	{
    	String testTitle = "Existing resolution post-function is untouched";
    	String workflowName = "Workflow Council Int test - Pre-existing post-function";
    	
    	StringBuffer resultString = new StringBuffer();
		printHeader(resultString, testTitle);
    	
		JiraWorkflow workingCopyOfWorkflow = givenAWorkingCopyOf(workflowName);
    	whenResolutionPostFunctionsAreAddedTo(workingCopyOfWorkflow);
    	thenExistingResolutionPostFunctionIsNotTouched(resultString, workingCopyOfWorkflow, workflowName);
    	thenWorkflowBackUpExists(workflowName, resultString);
    	tearDown(workingCopyOfWorkflow);

    	printFooter(resultString);

    	return resultString;
	}
    
	private StringBuffer testWorkflowIsUpdated(String testTitle, String workflowName)
    {
    	StringBuffer resultString = new StringBuffer();
    	printHeader(resultString, testTitle);
    	
    	JiraWorkflow workingCopyOfWorkflow = givenAWorkingCopyOf(workflowName);
    	whenResolutionPostFunctionsAreAddedTo(workingCopyOfWorkflow);
    	thenResolutionPostFunctionsAreAddedCorrectly(resultString, workingCopyOfWorkflow, workflowName);
    	thenWorkflowBackUpExists(workflowName, resultString);
    	tearDown(workingCopyOfWorkflow);

    	printFooter(resultString);

    	return resultString;
    	
    }
	
    private void thenExistingResolutionPostFunctionIsNotTouched(StringBuffer resultString,
	        JiraWorkflow workingCopyOfWorkflow, String workflowName)
	{
		JiraWorkflow updatedWorkflow = this.wfManager.getWorkflow(workflowName + " - working copy");
		ActionDescriptor theOnlyCommonAction = (ActionDescriptor)updatedWorkflow.getDescriptor().getCommonActions().values().iterator().next();
		FunctionDescriptor postFunction = (FunctionDescriptor)theOnlyCommonAction.getUnconditionalResult().getPostFunctions().get(FRONT);
		
		resultString.append(
				assertResult(
				WONT_DO_RESOLUTION.equals(postFunction.getArgs().get(FIELD_VALUE_KEY)), 
				"Existing resolution post function should not be changed for action " + theOnlyCommonAction.getId(), 
				"Action " + theOnlyCommonAction.getId() + " was supposed to have a post function setting resolution to Won't Do. Instead, it sets it to " + postFunction.getArgs().get(FIELD_VALUE_KEY)));
	}
    
	private void thenWorkflowBackUpExists(String workflowName, StringBuffer resultString)
	{
		boolean found = false;
		int numberFound = 0;
		for(JiraWorkflow workflow : this.wfManager.getWorkflows())
		{
			if(workflow.getDescription().contains(backupWorkflowDescriptionName(workflowName)))
			{
				found = true;
				numberFound++;
			}
		}
		
		
		resultString.append(assertResult(found, "Workflow backup of " + workflowName + " should exist", "Workflow backup of " + workflowName + " not found"));
		resultString.append(assertResult(numberFound == 1, "Workflow backup of " + workflowName + " should have only 1 backup", "Workflow backup of " + workflowName + " had " + numberFound + " backups."));
	}

	private void printFooter(StringBuffer resultString)
	{
		resultString.append('\n');
    	resultString.append("-------------");
    	resultString.append('\n');
	}

	private void printHeader(StringBuffer resultString, String testName)
	{
		resultString.append("Test: ");
		resultString.append(testName);
    	printFooter(resultString);
	}

    private void tearDown(JiraWorkflow workingCopyOfWorkflow)
	{
		this.wfManager.deleteWorkflow(workingCopyOfWorkflow);
	}

    private void thenResolutionPostFunctionsAreAddedCorrectly(StringBuffer resultString,
	        JiraWorkflow workingCopyOfWorkflow, String workflowName)
	{
		JiraWorkflow updatedWorkflow = this.wfManager.getWorkflow(workflowName + " - working copy");
    	
    	assertResolutionPostFunctionsAreAddedCorrectly(resultString, updatedWorkflow.getDescriptor().getGlobalActions(), workingCopyOfWorkflow);
    	assertResolutionPostFunctionsAreAddedCorrectly(resultString, updatedWorkflow.getDescriptor().getCommonActions().values(), workingCopyOfWorkflow);
    	
    	for(Object stepObj : updatedWorkflow.getDescriptor().getSteps())
    	{
    		StepDescriptor step = (StepDescriptor)stepObj;
    		
    		assertResolutionPostFunctionsAreAddedCorrectly(resultString, step.getActions(), workingCopyOfWorkflow);
    	}
	}
    
    @SuppressWarnings("rawtypes")
	private void assertResolutionPostFunctionsAreAddedCorrectly(StringBuffer resultString,
    		Collection actions, JiraWorkflow workingCopyOfWorkflow)
    {
    	for(Object actionObj : actions)
    	{
    		ActionDescriptor action = (ActionDescriptor)actionObj;
    		assertThatResolutionPostFunctionExistsAndIsCorrect(resultString, stepColorMapFrom(workingCopyOfWorkflow), action);
    	}
    }

    private void whenResolutionPostFunctionsAreAddedTo(JiraWorkflow workingCopyOfWorkflow)
	{
		this.objectUnderTest.addResolutionPostFunctionsTo(workingCopyOfWorkflow);
	}

	private JiraWorkflow givenAWorkingCopyOf(String workflowName)
	{
		ApplicationUser wfCouncilUser = this.userManager.getUserByName(WF_COUNCIL_USER);
    	JiraWorkflow workflow = this.wfManager.getWorkflow(workflowName);
		JiraWorkflow workingCopyOfWorkflow = this.wfManager.copyWorkflow(wfCouncilUser, workflowName + " - working copy", "", workflow);
		return workingCopyOfWorkflow;
	}

	private Map<Integer, StatusCategory> stepColorMapFrom(JiraWorkflow workingCopyOfWorkflow)
	{
		Map<Integer, StatusCategory> stepColorMap = new HashMap<Integer, StatusCategory>();
		
		for(Status statusInWorkflow : workingCopyOfWorkflow.getLinkedStatusObjects())
		{
			int stepId = workingCopyOfWorkflow.getLinkedStep(statusInWorkflow).getId();
			StatusCategory statusColor = statusInWorkflow.getStatusCategory();
			stepColorMap.put(stepId, statusColor);
		}
		return stepColorMap;
	}

	private void assertThatResolutionPostFunctionExistsAndIsCorrect(StringBuffer resultString,
	        Map<Integer, StatusCategory> stepColorMap, ActionDescriptor action)
	{
		FunctionDescriptor postFunction = (FunctionDescriptor)action.getUnconditionalResult().getPostFunctions().get(FRONT);
		
		if(isAResolutionPostFunction(postFunction))
		{
			assertPostFunctionIsCorrectForAction(resultString, stepColorMap, action, postFunction);
		}
		else
		{
			failTheTestForResolutionPostFunctionNotFirst(resultString, action);
		}
	}

	private void failTheTestForResolutionPostFunctionNotFirst(StringBuffer resultString, ActionDescriptor action)
	{
		resultString.append(
				assertResult(
				false, 
				"Resolution post function should be first in the list", 
				"Action " + action.getId() + " does not have a resolution post function at the front of the list"));
	}

	private void assertPostFunctionIsCorrectForAction(StringBuffer resultString,
	        Map<Integer, StatusCategory> stepColorMap, ActionDescriptor action, FunctionDescriptor postFunction)
	{
		int stepIdOfAction = action.getUnconditionalResult().getStep();
		StatusCategory colorOfPostFunction = stepColorMap.get(stepIdOfAction);
		if(COMPLETE_STATUS_CATEGORY_NAME.equals(colorOfPostFunction.getName()))
		{
			resultString.append(
					assertResult(
					DONE_RESOLUTION.equals(postFunction.getArgs().get(FIELD_VALUE_KEY)), 
					"Resolution post function should set to done for action " + action.getId(), 
					"Action " + action.getId() + " was supposed to have a post function setting resolution to Done. Instead, it sets it to " + postFunction.getArgs().get(FIELD_VALUE_KEY)));
			
		}
		else
		{
			resultString.append(
					assertResult(
					!DONE_RESOLUTION.equals(postFunction.getArgs().get(FIELD_VALUE_KEY)), 
					"Resolution post function should set to Unresolved for action " + action.getId(), 
					"Action " + action.getId() + " was supposed to have a post function setting resolution to Unresolved. Instead, it sets it to " + postFunction.getArgs().get(FIELD_VALUE_KEY)));
			
		}
	}

	private boolean isAResolutionPostFunction(FunctionDescriptor postFunction)
	{
		return postFunction.getArgs().containsKey(FIELD_NAME_KEY) && 
		   postFunction.getArgs().get(ResolvedWorkflowService.FIELD_NAME_KEY).equals(RESOLUTION_FIELD_NAME);
	}
    
    private StringBuffer assertResult(boolean conditionToBeTrue, String assertionTitle, String failureMessage)
    {
    	StringBuffer assertionMessage = new StringBuffer();
    	
    	assertionMessage.append(assertionTitle);
    	assertionMessage.append(": ");
    	
    	if(conditionToBeTrue)
    	{
    		assertionMessage.append("[Passed] ");
    	}
    	else
    	{
    		assertionMessage.append("[Failed] ");
    		assertionMessage.append(failureMessage);
    	}
    	assertionMessage.append('\n');
    	
    	return assertionMessage;
    }
}
