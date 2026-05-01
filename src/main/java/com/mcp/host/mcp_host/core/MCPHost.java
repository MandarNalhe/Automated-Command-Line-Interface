package com.mcp.host.mcp_host.core;

import com.mcp.host.mcp_host.model.MCPRequest;
import com.mcp.host.mcp_host.model.MCPResponse;
import com.mcp.host.mcp_host.tools.*;

public class MCPHost {
    public MCPResponse handle(MCPRequest request){
        switch(request.tool){

            case "FileManager":
                return FileManager.handleAction(request.action,request.parameters);

            case "ApplicationLauncher":
                return ApplicationLauncher.start(request.action,request.parameters);

            case "CommandPrompt":
                return CommandPrompt.execute(request.action,request.parameters);

            case "NetworkingCommands" :
                return NetworkingCommands.execute(request.action,request.parameters);

            case "MediaTool":
                return MediaTool.handleMedia(request.action,request.parameters);

            default:
                return new MCPResponse("error","Unknown tool : "+request.tool);
        }
    }
}
