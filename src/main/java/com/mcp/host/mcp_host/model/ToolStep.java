
package com.mcp.host.mcp_host.model;

public class ToolStep {
    // These names must match the JSON keys precisely: 'tool', 'prompt', 'description'.
    private String tool;
    private String windowsCommand; // Renamed from 'command'
    private String description;
    private String linuxCommand;

    public String getLinuxCommand() {
        return linuxCommand;
    }

    // Getters and Setters (omitted for brevity)
    public String getTool() { return tool; }

    public String getWindowsCommand() {
        return windowsCommand;
    }

    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("[%s] Prompt: %s\nDesc: %s", tool, windowsCommand, description, linuxCommand);
    }
}