package com.mcp.host.mcp_host.Services;// NOTE: In a real project, you would need imports for:
// - JSON libraries (e.g., com.google.gson.Gson)
// - HTTP Client (e.g., java.net.http.HttpClient or an API SDK)
// - Model classes (ToolStep, ExecutionPlan)

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.gson.Gson;
import com.mcp.host.mcp_host.model.ExecutionPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Component
public class Orchestrator {

    @Value("${apiKey}")
    private String apiKey ;

    // The fixed, standard working directory your tools will operate in.
    private final String CURRENT_CWD = System.getProperty("user.home");

    // Stores the history of user and model turns for continuity.
    private List<Map<String, Object>> chatHistory = new java.util.ArrayList<>();

    // System Instruction content from system_prompt_pty_optimized.txt
    private final String SYSTEM_INSTRUCTION_CONTENT = """
        You are a deterministic, goal-oriented AI Orchestrator running inside a stateful desktop environment with a persistent PTY (Pseudo-Terminal) session. Your sole task is to translate complex user requests into a precise, sequential JSON execution plan.

        ### 1. Available Tools & Definitions
        You MUST use one of the following tools for every step. The 'prompt' field MUST contain the exact input needed by the tool.

        1.  **CMD_EXEC**: Executes a single operating system shell command on windows.
            * **Prompt Field:** MUST contain the exact, runnable, single-line shell command and its parameters (e.g., `git status -s`, `mkdir new_project`, or `cd /home/user/`).
            * **Crucial:** This tool maintains CWD state in the PTY session. Use `cd` explicitly to change directories.
        2.  **AI_GENERATE**: Used for general purpose AI operations (summarization, analysis, writing, or code generation).
            * **Prompt Field:** The specific instruction for the AI (e.g., `Summarize the following text...`, or `Write a 5-line Python script to calculate Fibonacci sequence`).
        3.  **FILE_WRITE**: Writes content (from a previous step's output) to a specified file path.
            * **Prompt Field:** The full file path (absolute or relative to CWD) to write the content to (e.g., `report.txt`).

        ### 2. Output & Data Flow Rules

        * **JSON Format:** Your entire response MUST be a single JSON object conforming exactly to the required schema (`narrative`, `steps` array). DO NOT include any text, conversational remarks, or markdown outside the JSON structure.
        * **Sequential Data Flow (Context):** The output from `WEB_SEARCH` or `AI_GENERATE` is automatically saved to an internal context map and is immediately available for the NEXT step if that step is `FILE_WRITE`. You MUST plan the sequence accordingly.
        * **CWD:** The Current Working Directory is always relative to: ${CURRENT_CWD}
        """; // Using Java Text Block for readability

    public Orchestrator() {
        // Ensure the base directory exists when the app starts
        new java.io.File(CURRENT_CWD).mkdirs();
    }


    public ExecutionPlan getExecutionPlan(String userPrompt) throws Exception {
        // 1. Add user prompt to contents (history)
        Map<String, Object> userPart = Map.of("text", userPrompt);
        Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(userPart));
        chatHistory.add(userContent);
        // 2. Build the API payload (Request JSON)
        String systemInstruction = buildSystemInstruction();
        // The Java code to serialize the full request payload (omitted, requires Gson/Jackson)
        String requestJson = buildRequestJson(systemInstruction);
        System.out.println();
        // 3. Make the HTTP POST Request (omitted, requires HTTP client setup)
        Client client = Client.builder() .apiKey(apiKey) .build();
        GenerateContentConfig config = GenerateContentConfig.builder() .responseMimeType("application/json") .build();
        GenerateContentResponse response = client.models.generateContent( "gemini-2.5-flash", requestJson, config);
        String responseBody = response.text(); System.out.println(responseBody);
        // 4. Extract and parse the model's JSON text
        String planJsonText = responseBody;
        // 5. Deserialize the plan JSON into Java objects
        Gson gson = new Gson();
        ExecutionPlan plan = gson.fromJson(planJsonText, ExecutionPlan.class);
        //ExecutionPlan plan = new ExecutionPlan(); // SIMULATED
        // 6. Add the model's JSON response to history for conversation context
        Map<String, Object> modelPart = Map.of("text", planJsonText);
        Map<String, Object> modelContent = Map.of("role", "model", "parts", List.of(modelPart));
        chatHistory.add(modelContent);
        return plan;
    }
    /**
     * Constructs the specific system instruction for the AI Orchestrator.
     * This defines the AI's role and rules.
     */
    private String buildSystemInstruction() {
        return SYSTEM_INSTRUCTION_CONTENT;
    }

    /**
     * Constructs the full request payload for the Gemini API call as a Map
     * which would be serialized into JSON using Gson or Jackson.
     * * The final payload MUST contain: contents, systemInstruction, tools, and generationConfig.
     */
    private String buildRequestJson(String systemInstruction) {
        // Define the JSON schema for the response structure (part of generationConfig)
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "narrative", Map.of("type", "STRING"),
                        "steps", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "tool", Map.of("type", "STRING", "enum", List.of("CMD_EXEC", "AI_GENERATE", "FILE_WRITE")),
                                                "windowsCommand", Map.of("type", "STRING"), // Renamed field
                                                "description", Map.of("type", "STRING"),
                                                "linuxCommand",Map.of("type","STRING")
                                        ),
                                        "required", List.of("tool", "windowsCommand", "description","linuxCommand")
                                )
                        )
                ),
                "required", List.of("narrative", "steps")
        );

        // Define the generation configuration (structured output settings)
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema
        );

        // Define the system instruction structure
        Map<String, Object> instruction = Map.of(
                "parts", List.of(Map.of("text", systemInstruction))
        );

        // Define the tools (Google Search Grounding)
        List<Map<String, Object>> tools = List.of(
                Map.of("google_search", Map.of())
        );

        // Assemble the final payload map with all required components
        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", chatHistory);           // (1) Chat history and user prompt
        payload.put("systemInstruction", instruction);  // (2) System persona and rules
        payload.put("tools", tools);                    // (3) Tool definition (Google Search)
        payload.put("generationConfig", generationConfig); // (4) Structured output configuration

        // NOTE: In a real Java app, you MUST use a library (like Gson or Jackson)
        // to serialize the 'payload' Map into the final JSON String for the HTTP request body.
        Gson gson = new Gson();
        return gson.toJson(payload);
    }

    // --- SIMULATION METHODS (Replace with real HTTP/JSON logic) ---

    // Parses the outer response JSON to find candidates[0].content.parts[0].text
    private String extractPlanJson(String responseBody) {
        // Note the simulation uses 'prompt' instead of 'command' now.
        return "{ \"narrative\": \"Simulated plan to create directory, find news, and save link.\", \"steps\": [ {\"tool\": \"CMD_EXEC\", \"prompt\": \"mkdir /home/user/AI_Workspace/reports\", \"description\": \"Creates a dedicated directory.\"}, {\"tool\": \"WEB_SEARCH\", \"prompt\": \"latest 2026 world cup news article link\", \"description\": \"Find the most relevant article link.\"}, {\"tool\": \"FILE_WRITE\", \"prompt\": \"reports/wc_news.txt\", \"description\": \"Save the link to the specified file.\"} ] }";
    }

    // Simulates the API response for testing the flow
    private String simulateApiResponse() {
        return "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"" + extractPlanJson("dummy") + "\"}]}}]}";
    }

}


