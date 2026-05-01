package com.mcp.host.mcp_host.tools;

import com.mcp.host.mcp_host.model.MCPResponse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MediaTool {
    public static MCPResponse handleMedia(String action, Map<String, String> parameters){
        switch(action){
            case "playMedia":
                return playMedia(parameters.get("path"),parameters.get("fileName"));

            case "openImage":
                return openImage(parameters.get("path"),parameters.get("fileName"));

            default:
                return new MCPResponse("error","No such Media tool found ");
        }
    }

    //tool to open the image file
    private static MCPResponse openImage(String path,String fileName){
        try {
            List<String> extensions= Arrays.asList(new String[]{".jpg",".jpeg",".png",".gif",".bmp"});
            for(String ext:extensions){
                File file=new File(path+fileName+ext);
                if(!file.exists())
                    continue;
                else{
                    ProcessBuilder pb=new ProcessBuilder("cmd","/c","start"," ",path+fileName+ext);
                    pb.start();
                    return new MCPResponse("success","Image Opened Successfully !");
                }
            }
            return new MCPResponse("error","No Such Image Found !");
        } catch (IOException e) {
            return new MCPResponse("error",e.getMessage());
        }
    }

    //tool to play the audio or video
    private static MCPResponse playMedia(String path, String fileName) {
        try {
            if (!path.endsWith("/") && !path.endsWith("\\")) {
                path += "/";
            }

            String fullPath = path + fileName;
            if (!fullPath.contains(".")) {
                fullPath += ".mp4"; // Default fallback
            }

            System.out.println("🎬 Launching media: " + fullPath);

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", fullPath);
            pb.start();

            return new MCPResponse("success", "Media Played Successfully: " + fullPath);
        } catch (IOException e) {
            return new MCPResponse("error", "Failed: " + e.getMessage());
        }
    }

}
