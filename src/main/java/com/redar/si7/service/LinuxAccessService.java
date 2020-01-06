package com.redar.si7.service;

import com.redar.si7.domain.AccessModificator;
import com.redar.si7.domain.HostsAccess;
import com.redar.si7.utils.CommandExecutor;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LinuxAccessService implements DomainBlockerService {

    @Value("${iptables.file.path}")
    private String iptablesFilePath;

    @Override
    public List<HostsAccess> getAllDomains() {
        try {
            createPopulatedIptablesFile();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        final List<HostsAccess> blockedDomains = new ArrayList<>();

        try (FileReader fr = new FileReader(iptablesFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            String line;
            boolean outputSectionFlag = false;
            while ((line = br.readLine()) != null) {

                if (!line.trim().startsWith("#")) {
                    if (line.startsWith(":OUTPUT ACCEPT")) {
                        outputSectionFlag = true;
                    } else if (line.matches("^:(FORWARD|INPUT) (.)+") && outputSectionFlag) {
                        outputSectionFlag = false;
                    }
                }
                if (outputSectionFlag && line.contains(" -m string --string \"")) {
                    blockedDomains.add(new HostsAccess(line.trim().split("\"")[1], AccessModificator.BLOCKED));
                }
            }
            br.close();
        } catch (IOException ignored) {
        }

        return blockedDomains;
    }

    @Override
    public List<HostsAccess> blockDomain(final String domain) {
        try {
            createPopulatedIptablesFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Integer[] ipSections = new Integer[3];
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
                        blockedDomains.add(new HostsAccess(domain, AccessModificator.BLOCKED));
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

        if (ipSections[2] == null) {
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

    @Override
    public void removeRuleForDomain(final String domain) {
        final StringBuilder updatedFile = new StringBuilder();
        try (FileReader fr = new FileReader(iptablesFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains(" -m string --string \"" + domain + "\"")) {
                    updatedFile.append(line).append("\n");
                }
            }
            br.close();
        } catch (IOException ignored) {
        }

        try (FileWriter fw = new FileWriter(iptablesFilePath)) {
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(updatedFile.toString());
            bw.flush();
        } catch (IOException ignored) {
        }

        CommandExecutor.executeCommand("iptables-restore < " + iptablesFilePath);
    }

    private void createPopulatedIptablesFile() throws IOException {
        File iptableFile = new File(iptablesFilePath);
        if (!iptableFile.exists()) {
            iptableFile.createNewFile();
        }

        iptableFile.setWritable(true, false);

        CommandExecutor.executeCommand("iptables-save > " + iptablesFilePath);
    }
}
