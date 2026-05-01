
package com.mcp.host.mcp_host.controller;

import com.mcp.host.mcp_host.Services.Orchestrator;
import com.mcp.host.mcp_host.model.ExecutionPlan;
import com.mcp.host.mcp_host.model.MCPResponse;
import com.mcp.host.mcp_host.tools.CMD_EXEC;
import com.mcp.host.mcp_host.Services.SafetyCheck;
import com.mcp.host.mcp_host.model.ToolStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GenerateController {

    @Autowired
    private Orchestrator generateService;


    @PostMapping("/generate")
    public MCPResponse generate(@RequestBody InputRequest request) {
        try {
            // Step 1: Convert natural language to JSON execution plan
            ExecutionPlan plan = generateService.getExecutionPlan(request.getPrompt());
            System.out.println(" Received prompt: " + request.getPrompt());
            System.out.println(" Execution Plan Narrative: " + plan.getNarrative());
            
            // Step 2: Check for dangerous commands
            boolean isDangerous = false;
            if (plan.getSteps() != null) {
                for (ToolStep step : plan.getSteps()) {
                    if (step.getWindowsCommand() != null && SafetyCheck.isDangerous(step.getWindowsCommand())) {
                        isDangerous = true;
                        break;
                    }
                    if (step.getLinuxCommand() != null && SafetyCheck.isDangerous(step.getLinuxCommand())) {
                        isDangerous = true;
                        break;
                    }
                }
            }

            if (isDangerous) {
                MCPResponse response = new MCPResponse();
                response.status = "requires_confirmation";
                response.message = "⚠️ **DANGEROUS COMMAND DETECTED**\n\nThe plan contains potentially destructive operations. Do you want to proceed?\n\n📋 **Plan:**\n" + plan.getNarrative();
                response.plan = plan;
                return response;
            }

            return executePlanInternal(plan);
        } catch (Exception e) {
            e.printStackTrace();
            MCPResponse error = new MCPResponse();
            error.status = "error";
            error.message = "Error while generating: " + e.getMessage();
            return error;
        }
    }

    @PostMapping("/execute")
    public MCPResponse execute(@RequestBody ExecutionPlan plan) {
        try {
            return executePlanInternal(plan);
        } catch (Exception e) {
            e.printStackTrace();
            MCPResponse error = new MCPResponse();
            error.status = "error";
            error.message = "Error while executing: " + e.getMessage();
            return error;
        }
    }

    private MCPResponse executePlanInternal(ExecutionPlan plan) {
        CMD_EXEC exec = new CMD_EXEC(System.getProperty("user.home"));
        String executionOutput = exec.executePlan(plan);

        MCPResponse response = new MCPResponse();
        response.status = "success";
        
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("✅ Execution completed successfully!\n\n");
        messageBuilder.append("📋 Plan: ").append(plan.getNarrative() != null ? plan.getNarrative() : "").append("\n\n");
        messageBuilder.append("📝 Execution Results:\n").append(executionOutput);
        
        response.message = messageBuilder.toString();
        System.out.println(" Execution Complete → " + response.status);
        return response;
    }

    // DTO for request body
    public static class InputRequest {
        private String prompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }
}
