package com.redar.si7.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandExecutor {

    public static String executeCommand(final String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();

        // -- Linux --

        // Run a shell command
        processBuilder.command("bash", "-c", command);

        // Run a shell script
        //processBuilder.command("path/to/hello.sh");

        // -- Windows --

        // Run a command
        //processBuilder.command("cmd.exe", "/c", "dir C:\\Users\\mkyong");

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
                System.out.println("[!] Command '" + command + "' returned success!");
            } else {
                System.out.println("[!] Command '" + command + "' returned failure!");
            }
            if (!output.toString().isEmpty()) {
                System.out.println(output);
            }

            return output.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return "";
    }
}
