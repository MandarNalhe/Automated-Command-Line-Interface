package com.mcp.host.mcp_host.tools;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Conceptual PTY Session using pty4j principles.
 * This class handles the persistent state of the shell session.
 */
class PtySession {
    // Stores the current directory of the session, updated by 'cd' commands.
    private String currentDirectory;

    // Conceptual fields for pty4j interaction

    private PtyProcess ptyProcess;
    private Writer shellWriter; // To send commands to the shell
    private BufferedReader shellReader; // To read output from the shell

    // Timeouts and markers for reading output from the shell
    private static final int READ_TIMEOUT_MS = 10000;
    private static final String CMD_SUCCESS_MARKER = "___CMD_END___"; // Unique marker to signal the end of command output

    public PtySession(String currentDirectory) {
        this.currentDirectory = currentDirectory;
        // --- PTY4J CONCEPTUAL INITIALIZATION ---
         try {
        //     // 1. Determine the shell command based on OS (conceptual)
             String shellPath = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd.exe" : "/bin/bash";
             String[] command = {shellPath, shellPath.endsWith("bash") ? "-i" : ""};
             Map<String, String> env = new HashMap<>(System.getenv());
             if (!env.containsKey("TERM")) env.put("TERM", "xterm");
        //     // 2. Start the PTY process
             System.out.println("Path : "+Arrays.stream(command).findFirst());
             this.ptyProcess = new PtyProcessBuilder(command)
                     .setDirectory(currentDirectory)
                     .setEnvironment(env)
                     .start();
        //
        //     // 3. Set up streams
             this.shellWriter = new BufferedWriter(new OutputStreamWriter(ptyProcess.getOutputStream()));
             this.shellReader = new BufferedReader(new InputStreamReader(ptyProcess.getInputStream()));
        //
        //     // Execute a dummy command to ensure the shell prompt is flushed and streams are ready
             // execute("echo 'Shell Ready'");
         } catch (Exception e) {
            System.err.println("Failed to initialize PTY: " + e.getMessage());
        //     // Handle critical initialization failure
         }

        // --- SIMULATION (REMOVE IN PRODUCTION) ---
        // Simulating the shell writer and reader using standard Java streams for conceptual demo
       /* this.shellWriter = new BufferedWriter(new OutputStreamWriter(System.out));
        this.shellReader = new BufferedReader(new InputStreamReader(System.in))*/; // Not used in sim, but defined
        // --- END SIMULATION ---

        System.out.println("PTY Session Ready.");
    }


    public String execute(String command) throws IOException {
        String trimmedCommand = command.trim();
        System.out.println("-> Executing from CWD (" + this.currentDirectory + "): " + trimmedCommand);

        // 1. Explicitly handle 'cd' to maintain CWD state in the Java layer
        if (trimmedCommand.startsWith("cd ")) {
            String targetPath = trimmedCommand.substring(3).trim();

            // Resolve the path relative to the *current* directory
            File targetFile = Paths.get(this.currentDirectory, targetPath).toFile();

            try {
                if (targetPath.equals("..")) {
                    // Handle 'cd ..' correctly
                    File parentDir = new File(this.currentDirectory).getParentFile();
                    if (parentDir != null) {
                        this.currentDirectory = parentDir.getCanonicalPath() + File.separator;
                        return "[SUCCESS: CWD Update] Moved up to: " + this.currentDirectory;
                    }
                } else if (targetFile.isDirectory()) {
                    // Update the state to the canonical path for consistency
                    this.currentDirectory = targetFile.getCanonicalPath() + File.separator;
                    return "[SUCCESS: CWD Update] Directory changed to: " + this.currentDirectory;
                } else {
                    // If it's not a valid directory, execute the original 'cd' command in PTY
                    // to get the actual shell error message.
                    return executeInPty(trimmedCommand);
                }
            } catch (Exception e) {
                return "[ERROR: CWD Update] Failed to resolve path. Executing in PTY. Error: " + e.getMessage();
            }
        }

        // 2. Execute all other commands via the PTY stream
        return executeInPty(trimmedCommand);
    }

    /**
     * Writes the command to the PTY and reads the output until the marker is found.
     * This is the core, non-simulated logic for the CMD_EXEC tool.
     */
    private String executeInPty(String command) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String separator = osName.contains("win") ? " & " : "; ";

        String flushCommand = osName.contains("win") ? "echo." : "echo";
        String flushAndMarkerCommand = flushCommand + separator + "echo " + CMD_SUCCESS_MARKER;

        String fullCommand = command + separator + flushAndMarkerCommand;

        // CRLF for Windows CMD, LF for POSIX shells
        String lineEnding = osName.contains("win") ? "\r\n" : "\n";

        shellWriter.write(fullCommand + lineEnding);
        shellWriter.flush();

        return readShellOutput(command);
    }
    /*private String readShellOutput() {
        return "";
    }*/

    /**
     * Reads the shell output until the command marker is encountered or a timeout occurs.
     */
    private String readShellOutput(String originalCommand) throws IOException {
        StringBuilder output = new StringBuilder();
        long startTime = System.currentTimeMillis();
        boolean markerFound = false;

        char[] buffer = new char[1024];
        int charsRead;

        while (System.currentTimeMillis() - startTime < READ_TIMEOUT_MS) {

            // If process is dead and no data pending, stop
            if (!ptyProcess.isAlive() && !shellReader.ready()) {
                break;
            }

            try {
                if (shellReader.ready()) {
                    charsRead = shellReader.read(buffer, 0, buffer.length);
                    if (charsRead > 0) {
                        String chunk = new String(buffer, 0, charsRead);
                        output.append(chunk);

                        // IMPORTANT: only treat the marker as "found" when it appears as its own line,
                        // not just inside the echoed command line.
                        String soFar = output.toString();
                        if (soFar.contains("\r\n" + CMD_SUCCESS_MARKER) ||
                                soFar.contains("\n" + CMD_SUCCESS_MARKER)  ||
                                soFar.startsWith(CMD_SUCCESS_MARKER)) {
                            markerFound = true;
                            break;
                        }

                    } else if (charsRead == -1) {
                        // EOF
                        break;
                    }
                } else {
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            } catch (InterruptedException ignored) {}
        }

        String all = output.toString();

        if (!markerFound) {
            if (!ptyProcess.isAlive()) {
                System.err.println("PTY Process terminated unexpectedly.");
                return "[CMD_EXEC FAILURE] PTY Process terminated unexpectedly. Partial Output:\n" +
                        all.trim();
            }
            System.err.println("PTY read loop timed out after " + READ_TIMEOUT_MS + "ms.");
            return "[CMD_EXEC WARNING] Command timed out after " + READ_TIMEOUT_MS +
                    "ms. Partial Output:\n" + all.trim();
        }

        // 1. Remove everything from the LAST marker onward
        int markerIndex = all.lastIndexOf(CMD_SUCCESS_MARKER);
        if (markerIndex != -1) {
            all = all.substring(0, markerIndex);
        }

        // Normalize newlines
        all = all.replace("\r", "");

        // 2. Strip the echoed command line (Windows: "C:\Users\manda>dir /b /ad & echo. & echo ___CMD_END___")
        int cmdEchoIndex = all.indexOf(">" + originalCommand);
        if (cmdEchoIndex != -1) {
            int newlineAfterCmd = all.indexOf("\n", cmdEchoIndex);
            if (newlineAfterCmd != -1) {
                all = all.substring(newlineAfterCmd + 1);
            } else {
                all = "";
            }
        }

        all = all.trim();

        if (all.isEmpty()) {
            return "[CMD_EXEC SUCCESS] Output: (No text output from command)";
        }

        return "[CMD_EXEC SUCCESS] Output:\n" + all;

    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void close() {
        // try {
        //    if (ptyProcess != null) ptyProcess.destroy();
        // } catch (Exception ignored) {}
        System.out.println("PTY Session closed.");
    }
}

