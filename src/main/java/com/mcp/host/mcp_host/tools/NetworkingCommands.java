package com.mcp.host.mcp_host.tools;

import com.mcp.host.mcp_host.model.MCPResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

public class NetworkingCommands {
    public static MCPResponse execute(String action, Map<String,String> parameters){
        switch(action){
            case "network":
                return execNetworking(parameters.get("command"));

            default:
                return new MCPResponse("error","Can't find the desired operations ");
        }
    }
    private static MCPResponse execNetworking(String command){
        try {
            List<String> cmd= Arrays.asList(command.split(" "));
            ProcessBuilder pb=new ProcessBuilder(cmd);
            Process process= pb.start();
            int exitCode=process.waitFor();
            StringBuilder sb=new StringBuilder();
            String line;
            BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line=reader.readLine())!=null){
                sb.append(line).append("\n");
            }
            if(exitCode==0){
                return new MCPResponse("success",sb.toString());
            }
            else return new MCPResponse("error","Exited with the status code "+exitCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
