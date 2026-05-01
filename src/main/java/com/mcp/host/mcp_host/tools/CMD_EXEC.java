package com.mcp.host.mcp_host.tools;

// NOTE: To run this code, you MUST add the pty4j library and its native dependencies
// to your project (e.g., via Maven or Gradle).


import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.mcp.host.mcp_host.Services.SafetyCheck;
import com.mcp.host.mcp_host.model.ExecutionPlan;
import com.mcp.host.mcp_host.model.ToolStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Paths;

// You will need to uncomment and resolve these imports if using pty4j
// import com.pty4j.PtyProcess;
// import com.pty4j.PtyProcessBuilder;

public class CMD_EXEC {


    // The persistent PTY session object, central to solving Problem 2 (Directory Context)

    private final PtySession ptySession;

    private String response = "";
   @Value("${apiKey1}")
    private String apiKey;


    // Initial CWD from the orchestrator
    private final String currentDirectory;
    // --- NEW: INTERNAL DATA PROPAGATION CONTEXT ---
    // This map holds intermediate results between tools (e.g., search results for file writes)
    private final Map<String, String> executionContext;
    private static final String LAST_SEARCH_RESULT_KEY = "LAST_SEARCH_RESULT";
    // ---------------------------------------------


    public String executePlan(ExecutionPlan plan) {
        System.out.println("\n--- Starting Execution: " + plan.getNarrative() + " ---");
        // Clear context before a new plan starts
        this.executionContext.clear();

        for (ToolStep step : plan.getSteps()) {
            System.out.println("\n[STEP EXECUTION] Tool: " + step.getTool());
            System.out.println("  Description: " + step.getDescription());
            // NOTE: Using getPrompt() to match the updated ToolStep model
            System.out.println("  Prompt/Command: " + step.getWindowsCommand());

            try {
                if(SafetyCheck.isDangerous(step.getWindowsCommand())){
                    System.out.println("This may affect system "+step.getWindowsCommand());
                }
                System.out.println("Running in sandbox "+ step.getLinuxCommand());
                String sandboxOutput = SafetyCheck.runInSandbox(step.getLinuxCommand());
                System.out.println("Sandbox output = "+sandboxOutput);
                // NOTE: Using getPrompt() for the switch case parameters
                String output = switch (step.getTool()) {
                    case "CMD_EXEC" -> handleCmdExec(step.getWindowsCommand());
                    case "FILE_WRITE" -> handleFileWrite(step.getWindowsCommand());
                    case "WEB_SEARCH" -> handleWebSearch(step.getWindowsCommand());
                    case "BROWSER_NAVIGATE" -> handleBrowserNavigate(step.getWindowsCommand());
                    case "AI_GENERATE" -> handleAiGenerate(step.getWindowsCommand());
                    default -> throw new IllegalArgumentException("Unknown tool: " + step.getTool());
                };
                System.out.println("  OUTPUT: " + output);
                response += "\n"+output;
                if(response.toLowerCase().contains("error")){
                    String error = this.executionContext.get(LAST_SEARCH_RESULT_KEY);
                    Client client = Client.builder()
                            .apiKey(apiKey)
                            .build();

                    GenerateContentConfig config = GenerateContentConfig.builder()
                            .responseMimeType("application/json")
                            .build();
                    GenerateContentResponse errorOutput =
                            client.models.generateContent(
                                    "gemini-2.5-flash",
                                    error,
                                    config);
                    System.out.println(errorOutput.text());
                    response += "\n"+errorOutput.text();
                }
            } catch (Exception e) {
                System.err.println("  ERROR: Execution failed for step " + step.getTool() + ". Reason: " + e.getMessage());
                // In a real app, you would stop or ask the user for guidance here.
                break;
            }
        }
        System.out.println("--- Execution Finished. Final CWD: " + ptySession.getCurrentDirectory() + " ---");
        return response;
    }

    private String handleBrowserNavigate(String prompt) {
        return "";
    }

    public CMD_EXEC(String currentDirectory) {
        this.currentDirectory = currentDirectory;
        // Ensure initialCWD ends with a separator for cleaner relative path handling
        this.ptySession = new PtySession(currentDirectory);
        this.executionContext = new HashMap<>(); // Initialize the context map
    }

    // --- TOOL HANDLERS ---

    /** * Executes command using the persistent PTY session.
     * This is the core method for the CMD_EXEC tool.
     */
    private String handleCmdExec(String prompt) throws IOException {
        System.out.println("Prompt : "+prompt);
        // Solves Problem 2: Directory Context is maintained by the PtySession object.
        String output =  ptySession.execute(prompt);
        this.executionContext.put(LAST_SEARCH_RESULT_KEY, output);
        return output;
    }

    /** Writes content to the filesystem. */
    private String handleFileWrite(String prompt) throws IOException {

        try{
            String contentToWrite = this.executionContext.get(LAST_SEARCH_RESULT_KEY);
            if (contentToWrite == null || contentToWrite.isEmpty()) {
                throw new IOException("Cannot write file: Context is missing content from previous search/AI step.");
            }

            String filePathStr = prompt.replace("file_write", "").trim();

            // remove quotes if present
            filePathStr = filePathStr.replace("'", "").replace("\"", "");
            System.out.println(filePathStr);
            Path filePath = Paths.get(ptySession.getCurrentDirectory(), filePathStr);

            Files.createDirectories(filePath.getParent()); // ensure dirs exist
            Files.write(filePath, contentToWrite.getBytes());

            return "[SUCCESS] Wrote " + contentToWrite.length() + " bytes to " + filePath;
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return "";
        // return String.format("[SIMULATION] File written successfully to %s (relative to CWD). Content length: %d", filePath.toString(), contentToWrite.length());
    }

    /** Simulates a web search and stores the result in the internal context. */
    private String handleWebSearch(String prompt) {
        // In the real implementation, this method would call the Gemini API
        // to get the final grounded text result from the search.

        // SIMULATION: Hardcoded result, as if the search was just executed.
        String simulatedResult = String.format(
                "The search for '%s' returned this summary: [DATA: The current price is $2000 USD per ounce as of Nov 2025.]",
                prompt
        );

        // --- NEW: STORE RESULT IN CONTEXT ---
        this.executionContext.put(LAST_SEARCH_RESULT_KEY, simulatedResult);
        // ------------------------------------

        return "[SIMULATION] Search successful. Result saved to internal context for the next step.";
    }

    /** Simulates an AI operation (like code generation or summarization) and stores the result. */
    private String handleAiGenerate(String prompt) {
        // REAL LOGIC: This would trigger a new API call to the LLM for a non-orchestration task.
        System.out.println("Executing AI_GENERATE: " + prompt);
        try {
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .build();
            String context = executionContext.get(LAST_SEARCH_RESULT_KEY);
            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash",
                            prompt+" "+context,
                            config);
                /*Content userContent = Content.builder()
                        .role("user")
                        .parts(List.of(Part.text(prompt))) // Part.text() is used
                        .build();

                GenerateContentResponse response = aiModel.generateContent(
                        Collections.singletonList(userContent)
                );*/
            String generatedText = response.text();


            this.executionContext.put(LAST_SEARCH_RESULT_KEY, generatedText);


            return "[AI_GENERATE SUCCESS] Output:\n" + generatedText;

        } catch (ApiException e) {
            System.err.println("Gemini API Error for AI_GENERATE: " + e.getMessage());
            return "[AI_GENERATE ERROR] Failed to generate content: " + e.getMessage();
        }

        // SIMULATION: Hardcoded result
        /*String simulatedResult = String.format(
                "The AI generated the following content based on prompt '%s':\n");
        return simulatedResult;*/
    }
}

