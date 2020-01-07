package com.redar.si7.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

public class CommandExecutor {

    public static String executeCommand(final String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();

        if (OSValidator.isUnix()) {
            // Linux - Run a shell command
            processBuilder.command("bash", "-c", command);
        } else if (OSValidator.isWindows()) {
            // Windows - Run a command
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            throw new IllegalArgumentException("Operating System '" + OSValidator.getOS() + "' is not supported by this application.");
        }

        // Run a shell script
        //processBuilder.command("path/to/hello.sh");

        // Run a bat file
        //processBuilder.command("C:\\Users\\mkyong\\hello.bat");

        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println(LocalDateTime.now() + "  INFO --- [  CommandExecutor] Command '" + command + "' returned success!");
            } else {
                System.out.println(LocalDateTime.now() + "  INFO --- [  CommandExecutor] Command '" + command + "' returned failure! (" + exitVal + ")");
            }
            if (!output.toString().isEmpty()) {
                System.out.println(output.toString().trim());
            }

            return output.toString().trim();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return "";
    }
}
