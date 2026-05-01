package com.mcp.host.mcp_host;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX Chatbot-Style UI for AuCLI Desktop Assistant
 * Connects to Spring Boot backend at http://localhost:8080/api/generate
 */
public class DesktopAppUI extends Application {

    private static final String API_URL = "http://localhost:8080/api/generate";
    private static final String DARK_BG = "#0D1117";
    private static final String HEADER_BG = "#161B22";
    private static final String USER_MSG_BG = "#1F6FEB";
    private static final String BOT_MSG_BG = "#21262D";
    private static final String SUCCESS_MSG_BG = "#238636";
    private static final String ERROR_MSG_BG = "#DA3633";
    private static final String INPUT_BG = "#0D1117";
    private static final String ACCENT_COLOR = "#58A6FF";
    
    private TextField inputField;
    private Button sendButton;
    private ScrollPane chatScrollPane;
    private VBox chatContainer;
    private OkHttpClient httpClient;
    private Gson gson;

    @Override
    public void start(Stage primaryStage) {
        // Initialize HTTP client and JSON parser
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        gson = new Gson();

        // Create root layout
        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + DARK_BG + ";");
        root.setPrefWidth(1000);
        root.setPrefHeight(750);

        // Header with gradient effect
        HBox header = new HBox(15);
        header.setPadding(new Insets(20, 25, 20, 25));
        header.setStyle("-fx-background-color: " + HEADER_BG + "; -fx-border-width: 0 0 1 0; -fx-border-color: #30363D;");
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Icon/Logo area
        Label iconLabel = new Label("⚡");
        iconLabel.setFont(Font.font("Arial", 32));
        iconLabel.setPadding(new Insets(0, 15, 0, 0));
        
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("AuCLI Desktop Assistant");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web(ACCENT_COLOR));
        
        Label subtitleLabel = new Label("🚀 AI-Powered Command Line Automation");
        subtitleLabel.setFont(Font.font("Segoe UI", 12));
        subtitleLabel.setTextFill(Color.web("#8B949E"));
        
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(iconLabel, titleBox);
        
        // Chat area with subtle background
        chatContainer = new VBox(12);
        chatContainer.setPadding(new Insets(20));
        chatContainer.setStyle("-fx-background-color: " + DARK_BG + ";");
        
        chatScrollPane = new ScrollPane(chatContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setFitToHeight(true);
        chatScrollPane.setStyle("-fx-background: " + DARK_BG + "; -fx-background-color: " + DARK_BG + ";");
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Input area with modern design
        VBox inputContainer = new VBox(8);
        inputContainer.setPadding(new Insets(15, 20, 20, 20));
        inputContainer.setStyle("-fx-background-color: " + HEADER_BG + "; -fx-border-width: 1 0 0 0; -fx-border-color: #30363D;");
        
        HBox inputArea = new HBox(12);
        inputArea.setAlignment(Pos.CENTER);

        inputField = new TextField();
        inputField.setPromptText("💬 Ask me to run commands, create files, search the web, or automate tasks...");
        inputField.setPrefHeight(45);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setStyle("-fx-background-color: " + INPUT_BG + "; -fx-text-fill: #C9D1D9; -fx-font-size: 14px; " +
                "-fx-background-radius: 25px; -fx-border-radius: 25px; -fx-padding: 12px 20px; " +
                "-fx-border-color: #30363D; -fx-border-width: 1px;");
        inputField.setOnAction(e -> submitRequest());
        
        // Hover effect for input
        inputField.setOnMouseEntered(e -> inputField.setStyle("-fx-background-color: #161B22; -fx-text-fill: #C9D1D9; -fx-font-size: 14px; " +
                "-fx-background-radius: 25px; -fx-border-radius: 25px; -fx-padding: 12px 20px; " +
                "-fx-border-color: " + ACCENT_COLOR + "; -fx-border-width: 1px;"));
        inputField.setOnMouseExited(e -> inputField.setStyle("-fx-background-color: " + INPUT_BG + "; -fx-text-fill: #C9D1D9; -fx-font-size: 14px; " +
                "-fx-background-radius: 25px; -fx-border-radius: 25px; -fx-padding: 12px 20px; " +
                "-fx-border-color: #30363D; -fx-border-width: 1px;"));

        sendButton = new Button("➤");
        sendButton.setPrefWidth(55);
        sendButton.setPrefHeight(45);
        sendButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-size: 20px; " +
                "-fx-font-weight: bold; -fx-background-radius: 25px; -fx-cursor: hand;");
        sendButton.setOnAction(e -> submitRequest());
        
        // Hover effect for button
        sendButton.setOnMouseEntered(e -> sendButton.setStyle("-fx-background-color: #4493F8; -fx-text-fill: white; -fx-font-size: 20px; " +
                "-fx-font-weight: bold; -fx-background-radius: 25px; -fx-cursor: hand;"));
        sendButton.setOnMouseExited(e -> sendButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-size: 20px; " +
                "-fx-font-weight: bold; -fx-background-radius: 25px; -fx-cursor: hand;"));

        inputArea.getChildren().addAll(inputField, sendButton);
        
        // Hint text with icon
        Label hintLabel = new Label("⌨️  Press Enter to send • Shift+Enter for new line");
        hintLabel.setFont(Font.font("Segoe UI", 10));
        hintLabel.setTextFill(Color.web("#6E7681"));
        hintLabel.setPadding(new Insets(0, 0, 0, 5));

        inputContainer.getChildren().addAll(inputArea, hintLabel);
        root.getChildren().addAll(header, chatScrollPane, inputContainer);

        // Create scene
        Scene scene = new Scene(root, 1000, 750);
        primaryStage.setTitle("AuCLI Desktop Assistant - AI-Powered CLI Automation");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setOnCloseRequest(e -> {
            if (httpClient != null) {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            }
            Platform.exit();
        });

        // Show creative welcome message
        String welcomeMessage = "✨ Welcome to AuCLI Desktop Assistant! ✨\n\n" +
                "I'm your AI-powered command-line automation assistant. I can help you with:\n\n" +
                "🔧 **Command Execution** - Run any Windows CLI commands\n" +
                "📁 **File Operations** - Create, modify, and manage files & folders\n" +
                "🌐 **Web Search** - Search the internet for information\n" +
                "🌍 **Browser Control** - Open URLs and navigate the web\n" +
                "🤖 **AI Generation** - Generate code, summaries, and content\n" +
                "📊 **Network Tools** - Ping, network scans, and diagnostics\n\n" +
                "💡 **Just tell me what you want to do in natural language!**\n" +
                "Examples: \"Create a folder named Projects\", \"Ping google.com\", \"Search for latest Java updates\"";
        
        addBotMessage(welcomeMessage, false, true);

        primaryStage.show();
    }

    /**
     * Adds a user message to the chat
     */
    private void addUserMessage(String text) {
        Platform.runLater(() -> {
            HBox messageBox = createMessageBubble(text, USER_MSG_BG, true, false);
            chatContainer.getChildren().add(messageBox);
            scrollToBottom();
        });
    }

    /**
     * Adds a bot message to the chat
     */
    private void addBotMessage(String text, boolean isError, boolean isWelcome) {
        Platform.runLater(() -> {
            String bgColor;
            if (isError) {
                bgColor = ERROR_MSG_BG;
            } else if (isWelcome) {
                bgColor = BOT_MSG_BG;
            } else {
                bgColor = SUCCESS_MSG_BG;
            }
            HBox messageBox = createMessageBubble(text, bgColor, false, isWelcome);
            chatContainer.getChildren().add(messageBox);
            scrollToBottom();
        });
    }

    /**
     * Overloaded method for backward compatibility
     */
    private void addBotMessage(String text, boolean isError) {
        addBotMessage(text, isError, false);
    }

    /**
     * Creates a message bubble with timestamp and styling
     */
    private HBox createMessageBubble(String text, String bgColor, boolean isUser, boolean isWelcome) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(8, 0, 8, 0));

        VBox messageBox = new VBox(8);
        messageBox.setMaxWidth(650);
        messageBox.setPadding(new Insets(16, 18, 16, 18));
        
        // Enhanced styling with shadow effect
        String shadowStyle = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);";
        messageBox.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 20px; " + shadowStyle);

        TextFlow textFlow = new TextFlow();
        
        // Parse text for formatting (bold, colors, etc.)
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Color code different parts
            if (line.contains("✨") || line.contains("Welcome")) {
                Text welcomeText = new Text(line + (i < lines.length - 1 ? "\n" : ""));
                welcomeText.setFill(Color.web("#58A6FF"));
                welcomeText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
                textFlow.getChildren().add(welcomeText);
            } else if (line.startsWith("🔧") || line.startsWith("📁") || line.startsWith("🌐") || 
                      line.startsWith("🌍") || line.startsWith("🤖") || line.startsWith("📊") ||
                      line.startsWith("💡") || line.startsWith("✅") || line.startsWith("❌")) {
                Text iconText = new Text(line + (i < lines.length - 1 ? "\n" : ""));
                iconText.setFill(Color.web("#79C0FF"));
                iconText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
                textFlow.getChildren().add(iconText);
            } else if (line.contains("**") && line.contains("**")) {
                // Bold text
                String[] parts = line.split("\\*\\*");
                for (int j = 0; j < parts.length; j++) {
                    Text partText = new Text(parts[j] + (j < parts.length - 1 ? "" : (i < lines.length - 1 ? "\n" : "")));
                    if (j % 2 == 1) {
                        partText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                        partText.setFill(Color.web("#C9D1D9"));
                    } else {
                        partText.setFont(Font.font("Segoe UI", 14));
                        partText.setFill(Color.web("#C9D1D9"));
                    }
                    textFlow.getChildren().add(partText);
                }
            } else if (line.contains("Command Output:") || line.contains("Execution Results:")) {
                Text headerText = new Text(line + (i < lines.length - 1 ? "\n" : ""));
                headerText.setFill(Color.web("#7EE787"));
                headerText.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
                textFlow.getChildren().add(headerText);
            } else if (line.contains("Pinging") || line.contains("Reply from") || line.contains("Packets:")) {
                Text cmdText = new Text(line + (i < lines.length - 1 ? "\n" : ""));
                cmdText.setFill(Color.web("#A5D6FF"));
                cmdText.setFont(Font.font("Consolas", 13));
                textFlow.getChildren().add(cmdText);
            } else {
                Text normalText = new Text(line + (i < lines.length - 1 ? "\n" : ""));
                normalText.setFill(isUser ? Color.WHITE : Color.web("#C9D1D9"));
                normalText.setFont(Font.font("Segoe UI", 14));
                textFlow.getChildren().add(normalText);
            }
        }
        
        textFlow.setLineSpacing(2);

        Label timeLabel = new Label(getCurrentTime());
        timeLabel.setFont(Font.font("Segoe UI", 10));
        timeLabel.setTextFill(Color.web("#6E7681"));

        if (isUser) {
            messageBox.getChildren().addAll(textFlow, timeLabel);
            messageContainer.getChildren().add(messageBox);
        } else {
            // Add bot label with icon
            HBox botLabelBox = new HBox(8);
            botLabelBox.setAlignment(Pos.CENTER_LEFT);
            Label botIcon = new Label("⚡");
            botIcon.setFont(Font.font("Arial", 14));
            Label botLabel = new Label("AuCLI Output");
            botLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            botLabel.setTextFill(Color.web("#58A6FF"));
            botLabelBox.getChildren().addAll(botIcon, botLabel);
            
            messageBox.getChildren().addAll(botLabelBox, textFlow, timeLabel);
            messageContainer.getChildren().add(messageBox);
        }

        return messageContainer;
    }

    /**
     * Gets current time formatted
     */
    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
    }

    /**
     * Scrolls chat to bottom
     */
    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * Submits the user's input to the backend API
     */
    private void submitRequest() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) {
            return;
        }

        // Disable input during request
        sendButton.setDisable(true);
        inputField.setDisable(true);
        String userInputCopy = userInput;
        inputField.clear();

        // Add user message to chat first
        addUserMessage(userInputCopy);

        // Show typing indicator AFTER user message
        Label typingIndicator = new Label("⚡ AuCLI is thinking...");
        typingIndicator.setFont(Font.font("Segoe UI", 13));
        typingIndicator.setTextFill(Color.web("#58A6FF"));
        typingIndicator.setPadding(new Insets(8, 18, 8, 18));
        HBox typingBox = new HBox(typingIndicator);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        typingBox.setStyle("-fx-background-color: " + BOT_MSG_BG + "; -fx-background-radius: 20px;");
        Platform.runLater(() -> {
            chatContainer.getChildren().add(typingBox);
            scrollToBottom();
        });

        // Execute HTTP request on background thread
        new Thread(() -> {
            try {
                // Build request body
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("prompt", userInputCopy);

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                // Execute request
                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    Platform.runLater(() -> {
                        // Remove typing indicator
                        chatContainer.getChildren().remove(typingBox);
                        
                        if (response.isSuccessful()) {
                            parseAndDisplayResponse(responseBody);
                        } else {
                            addBotMessage("❌ Request failed with status: " + response.code() + "\n\n" + responseBody, true, false);
                        }
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typingBox);
                    addBotMessage("❌ Network error: " + e.getMessage() + "\n\nMake sure the backend is running at " + API_URL, true, false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typingBox);
                    addBotMessage("❌ Unexpected error: " + e.getMessage(), true, false);
                    e.printStackTrace();
                });
            } finally {
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Cleans terminal control characters from output
     */
    private String cleanTerminalOutput(String output) {
        if (output == null) return "";
        // Remove ANSI escape sequences and terminal control characters
        return output
            .replaceAll("\u001B\\[[\\d;]*[A-Za-z]", "") // ANSI escape codes
            .replaceAll("\\[\\?25[hl]", "") // Cursor visibility
            .replaceAll("\\[\\d+[GK]", "") // Cursor positioning
            .replaceAll("\\[\\d+[ABCD]", "") // Cursor movement
            .replaceAll("\\]0;", "") // Window title
            .replaceAll("\\[0K", "") // Clear to end of line
            .replaceAll("\\[\\d+G", "") // Cursor horizontal position
            .replaceAll("\\[\\d+[HL]", "") // Cursor position
            .replaceAll("\\[\\d+[JK]", "") // Erase display
            .trim();
    }

    /**
     * Parses the JSON response and displays it as a chat message
     */
    private void parseAndDisplayResponse(String responseBody) {
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // Check for confirmation required
            if (jsonResponse.has("status") && "requires_confirmation".equals(jsonResponse.get("status").getAsString())) {
                String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "⚠️ Dangerous Command Detected!";
                Platform.runLater(() -> {
                    addConfirmationMessage(message, jsonResponse.getAsJsonObject("plan"));
                });
                return;
            }

            StringBuilder messageBuilder = new StringBuilder();
            boolean isError = false;
            
            // Check status
            if (jsonResponse.has("status")) {
                String status = jsonResponse.get("status").getAsString();
                if ("error".equals(status)) {
                    isError = true;
                    messageBuilder.append("❌ **Error:** ");
                } else {
                    messageBuilder.append("✅ **Execution completed successfully!**\n\n");
                }
            }
            
            // Get message content
            if (jsonResponse.has("message")) {
                String message = jsonResponse.get("message").getAsString();
                
                // Check if message contains JSON parsing error
                if (message.contains("Expected BEGIN_OBJECT but was BEGIN_ARRAY")) {
                    messageBuilder.append("Backend parsing error detected.\n\n");
                    messageBuilder.append("The execution plan was generated, but there's an issue with JSON parsing in the backend.\n");
                    messageBuilder.append("This usually means the execution output format doesn't match the expected MCPRequest structure.\n\n");
                    messageBuilder.append("Error details: ").append(message);
                    isError = true;
                } else {
                    // Parse the message to extract plan and execution results
                    String[] lines = message.split("\n");
                    boolean inExecutionResults = false;
                    StringBuilder executionOutput = new StringBuilder();
                    StringBuilder planSection = new StringBuilder();
                    
                    for (String line : lines) {
                        if (line.contains("📋 Plan:") || line.contains("Plan:")) {
                            planSection.append(line).append("\n");
                        } else if (line.contains("📝 Execution Results:") || line.contains("Execution Results:")) {
                            inExecutionResults = true;
                            executionOutput.append("\n");
                        } else if (inExecutionResults) {
                            // Clean terminal control characters and format
                            String cleanedLine = cleanTerminalOutput(line);
                            if (!cleanedLine.isEmpty()) {
                                executionOutput.append(cleanedLine).append("\n");
                            }
                        } else if (!line.contains("✅ Execution completed") && !line.trim().isEmpty()) {
                            planSection.append(line).append("\n");
                        }
                    }
                    
                    // Build formatted message
                    if (planSection.length() > 0) {
                        messageBuilder.append("📋 **Plan:**\n");
                        messageBuilder.append(planSection.toString().trim()).append("\n\n");
                    }
                    
                    if (executionOutput.length() > 0) {
                        messageBuilder.append("📝 **Command Output:**\n");
                        messageBuilder.append("─────────────────────────────────────\n");
                        messageBuilder.append(executionOutput.toString());
                        messageBuilder.append("─────────────────────────────────────");
                    } else {
                        // If we couldn't parse, just show the cleaned message
                        String cleanedMessage = cleanTerminalOutput(message);
                        messageBuilder.append(cleanedMessage);
                    }
                }
            }
            
            String finalMessage = messageBuilder.length() > 0 ? messageBuilder.toString() : cleanTerminalOutput(responseBody);
            addBotMessage(finalMessage, isError, false);
            
        } catch (Exception e) {
            addBotMessage("❌ Failed to parse response: " + e.getMessage() + "\n\nRaw response:\n" + responseBody, true, false);
            e.printStackTrace();
        }
    }

    /**
     * Adds a confirmation message for dangerous commands
     */
    private void addConfirmationMessage(String message, JsonObject plan) {
        HBox messageContainer = new HBox(10);
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(8, 0, 8, 0));

        VBox messageBox = new VBox(12);
        messageBox.setMaxWidth(650);
        messageBox.setPadding(new Insets(16, 18, 16, 18));
        messageBox.setStyle("-fx-background-color: #5C1615; -fx-background-radius: 20px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");

        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("⚠️");
        Label title = new Label("Confirmation Required");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.web("#F85149"));
        headerBox.getChildren().addAll(icon, title);

        Text text = new Text(message);
        text.setFill(Color.web("#C9D1D9"));
        text.setFont(Font.font("Segoe UI", 13));
        TextFlow textFlow = new TextFlow(text);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button proceedBtn = new Button("Proceed");
        proceedBtn.setStyle("-fx-background-color: #DA3633; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        
        Button abortBtn = new Button("Abort");
        abortBtn.setStyle("-fx-background-color: #21262D; -fx-text-fill: white; -fx-border-color: #30363D; -fx-border-width: 1px; -fx-cursor: hand;");

        proceedBtn.setOnAction(e -> {
            buttonBox.setDisable(true);
            executePlan(plan);
        });

        abortBtn.setOnAction(e -> {
            buttonBox.setDisable(true);
            addBotMessage("❌ Operation aborted by user.", true, false);
        });

        buttonBox.getChildren().addAll(proceedBtn, abortBtn);
        
        messageBox.getChildren().addAll(headerBox, textFlow, buttonBox);
        messageContainer.getChildren().add(messageBox);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    /**
     * Executes the plan after user confirmation
     */
    private void executePlan(JsonObject plan) {
        Label typingIndicator = new Label("⚡ Executing plan...");
        typingIndicator.setFont(Font.font("Segoe UI", 13));
        typingIndicator.setTextFill(Color.web("#58A6FF"));
        typingIndicator.setPadding(new Insets(8, 18, 8, 18));
        HBox typingBox = new HBox(typingIndicator);
        typingBox.setAlignment(Pos.CENTER_LEFT);
        typingBox.setStyle("-fx-background-color: " + BOT_MSG_BG + "; -fx-background-radius: 20px;");
        
        Platform.runLater(() -> {
            chatContainer.getChildren().add(typingBox);
            scrollToBottom();
            sendButton.setDisable(true);
            inputField.setDisable(true);
        });

        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(
                        plan.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url("http://localhost:8080/api/execute")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    Platform.runLater(() -> {
                        chatContainer.getChildren().remove(typingBox);
                        if (response.isSuccessful()) {
                            parseAndDisplayResponse(responseBody);
                        } else {
                            addBotMessage("❌ Request failed: " + response.code(), true, false);
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatContainer.getChildren().remove(typingBox);
                    addBotMessage("❌ Error: " + e.getMessage(), true, false);
                });
            } finally {
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    inputField.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Main method to launch the JavaFX application
     */
    public static void launchUI(String[] args) {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized, continue
        }
        launch(args);
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Error launching JavaFX application: " + e.getMessage());
            System.err.println("\nTo fix JavaFX runtime issues, run with:");
            System.err.println("  java --module-path <path-to-javafx-lib> --add-modules javafx.controls,javafx.fxml -cp <classpath> com.mcp.host.mcp_host.DesktopAppUI");
            e.printStackTrace();
        }
    }
}
