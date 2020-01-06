package com.redar.si7.service;

import com.redar.si7.domain.AccessModificator;
import com.redar.si7.domain.HostsAccess;
import com.redar.si7.utils.CommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LinuxIptablesService {

    @Value("${iptables.file.path}")
    private String iptablesFilePath;


    public List<HostsAccess> blockDomain(final String domain) {
//        new ProcessBuilder().command("bash", "-c", "iptables -A OUTPUT -p tcp -m string --algo bm --string \"" + domain + "\" -j REJECT");

        File iptableFile = new File(iptablesFilePath);
        Integer[] ipSections = new Integer[3];
        if (!iptableFile.exists()) {
            try {
                createIptablesFile(iptableFile);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }

        List<HostsAccess> blockedDomains = new ArrayList<>();
        final StringBuilder fileContents = new StringBuilder();
        try (FileReader fr = new FileReader(iptablesFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                fileContents.append(line).append("\n");

                if (!line.trim().startsWith("#")) {
                    if (ipSections[0] == null && line.startsWith(":INPUT ACCEPT")) {
                        ipSections[0] = lineNumber;
                    } else if (ipSections[1] == null && line.startsWith(":FORWARD ACCEPT")) {
                        ipSections[1] = lineNumber;
                    } else if (ipSections[2] == null && line.startsWith(":OUTPUT ACCEPT")) {
                        fileContents.append("-A OUTPUT -p tcp -m string --string \"").append(domain).append("\" --algo bm --to 65535 -j REJECT --reject-with icmp-port-unreachable\n");
                        ipSections[2] = lineNumber;
                        ipSections[2] = lineNumber;
                    }
                }
                if (ipSections[2] != null && line.contains(" -m string --string \"")) {
                    blockedDomains.add(new HostsAccess(line.trim().split("\"")[1], AccessModificator.BLOCKED));
                }
                lineNumber++;
            }

            br.close();
        } catch (IOException ignored) {
        }

        if (ipSections[2] == null){
            return null;
        }

        try (FileWriter fw = new FileWriter(iptablesFilePath)) {
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(fileContents.toString());
            bw.flush();
        } catch (IOException ignored) {
        }

        CommandExecutor.executeCommand("iptables-restore < " + iptablesFilePath);

        return blockedDomains;
    }

    private void createIptablesFile(final File file) throws IOException {
        file.createNewFile();
        file.setWritable(true, false);

        CommandExecutor.executeCommand("iptables-save > " + iptablesFilePath);
//        Set<PosixFilePermission> perms = Files.readAttributes(file.toPath(), PosixFileAttributes.class).permissions();
//        perms.add(PosixFilePermission.OTHERS_WRITE);
//        Files.setPosixFilePermissions(file.toPath(), perms);
    }
}
