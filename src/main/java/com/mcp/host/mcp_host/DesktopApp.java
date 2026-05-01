package com.mcp.host.mcp_host;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main launcher that starts Spring Boot backend and then launches JavaFX UI
 */
@SpringBootApplication
public class DesktopApp {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Start Spring Boot in a separate thread
        Thread springBootThread = new Thread(() -> {
            springContext = SpringApplication.run(McpHostApplication.class, args);
        });
        springBootThread.setDaemon(false);
        springBootThread.start();

        // Wait for Spring Boot to start (check if port 8080 is ready)
        waitForSpringBoot();

        // Launch JavaFX UI
        Application.launch(DesktopAppUI.class, args);
    }

    /**
     * Wait for Spring Boot to be ready by checking if the server is up
     */
    private static void waitForSpringBoot() {
        int maxWait = 30; // Maximum wait time in seconds
        int waited = 0;
        
        while (waited < maxWait) {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8080/actuator/health");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    System.out.println("Spring Boot backend is ready!");
                    return;
                }
            } catch (Exception e) {
                // Server not ready yet, wait and retry
            }
            
            try {
                Thread.sleep(1000);
                waited++;
                if (waited % 5 == 0) {
                    System.out.println("Waiting for Spring Boot to start... (" + waited + "s)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // If we get here, assume Spring Boot is starting (might not have actuator endpoint)
        System.out.println("Assuming Spring Boot is ready. Launching UI...");
    }
}

