package com.mcp.host.mcp_host.model;

import java.util.Map;

public class MCPResponse {

    //data members
    public String status;
    public String message;
    public ExecutionPlan plan;

    public MCPResponse(){}

    // parameterised constructor
    public MCPResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
}
