package com.mcp.host.mcp_host.tools;

import com.mcp.host.mcp_host.model.MCPResponse;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

public class ApplicationLauncher {
    public static MCPResponse start(String action, Map<String,String> parameters){
        switch(action){

            case "openApplication":
                return openApplication(parameters.get("fileName"));

            default:
                return new MCPResponse("error","Unknown Application Launcher tool");
        }
    }

    private static MCPResponse openApplication(String appName){
        try{
            ProcessBuilder pb=new ProcessBuilder("cmd","/c","start"," ",appName);
            pb.start();
            return new MCPResponse("success","Application launched Successfully");
        } catch (IOException e) {
            return new MCPResponse("error","Failed to launch aplication ");
        }

    }
}
