package com.mcp.host.mcp_host.tools;

import com.mcp.host.mcp_host.model.MCPResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CommandPrompt {
    public static MCPResponse execute(String action, Map<String,String> parameters){

        switch (action){

            case "Independent":
                return execIndependent(parameters.get("command"));
            case "Dependent":
                return execDependent(parameters.get("path"), parameters.get("command"));
            default:
                return new MCPResponse("error","Unknown Command Execution");
        }
    }

    // tool to exec the commands within the given directory
    private static MCPResponse execDependent(String path,String command){
        try {
            List<String> cmd=Arrays.asList(command.split(" "));
            ProcessBuilder pb=new ProcessBuilder(cmd);
            File file=new File(path);
            pb.directory(file);
            Process process=pb.start();
            int exitcod= process.waitFor();
            BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder sb=new StringBuilder();
            while((line= reader.readLine())!=null){
                sb.append(line).append("\n");
            }
            if(exitcod==0){
                return new MCPResponse("success",sb.toString());
            }
            else return new MCPResponse("error","Command Execution Failed ");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // tool to execute the commands that are independent of the directory location
    private static MCPResponse execIndependent(String command){
        try {
            List<String> cmd= Arrays.asList(command.split(" "));
            ProcessBuilder pb=new ProcessBuilder(cmd);
            Process process= pb.start();
            int exit=process.waitFor();
            BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder sb=new StringBuilder();
            while((line=reader.readLine())!=null){
                sb.append(line).append("\n");
            }
            if(exit==0){
                return new MCPResponse("success",sb.toString());
            }
            else return new MCPResponse("error","Exited with code "+exit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
