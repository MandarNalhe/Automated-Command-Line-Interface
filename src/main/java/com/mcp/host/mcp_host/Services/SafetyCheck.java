package com.mcp.host.mcp_host.Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;

public class SafetyCheck {
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            Pattern.compile("\\brm\\s+-rf\\b"),                         // rm -rf
            Pattern.compile("(?i)\\bdel\\b.*\\s+/f\\b"),                         // Windows delete
            Pattern.compile("\\brmdir\\b"),                             // remove dir
            Pattern.compile("\\bshutdown\\b"),                          // shutdown
            Pattern.compile("\\breboot\\b"),                            // reboot
            Pattern.compile("\\bmkfs\\b"),                              // format disk (Linux)
            Pattern.compile("\\bformat\\b"),                            // format disk (Windows)
            Pattern.compile("\\bkill\\s+-9\\b"),                        // force kill
            Pattern.compile("/dev/sd[a-z]"),                            // disk devices
            Pattern.compile("C:/Windows", Pattern.CASE_INSENSITIVE)     // system folder
    );

    public static boolean isDangerous(String command) {
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(command).find()) {
                return true;
            }
        }
        return false;
    }
    public static String runInSandbox(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", "aucli-test", "bash", "-c", "\""+command+"\""
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder output = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        process.waitFor();

        return output.toString();
    }
}
