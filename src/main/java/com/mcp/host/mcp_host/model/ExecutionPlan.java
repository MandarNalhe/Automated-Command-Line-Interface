package com.mcp.host.mcp_host.model;

import java.util.List;

public class ExecutionPlan {
    private String narrative;
    private List<ToolStep> steps;

    // Getters and Setters (omitted for brevity)
    public String getNarrative() { return narrative; }
    public List<ToolStep> getSteps() { return steps; }
}