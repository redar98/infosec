package com.redar.si7.service;

import com.redar.si7.domain.AccessModificator;
import com.redar.si7.domain.HostsAccess;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WindowsAccessService implements DomainBlockerService {

    @Value("${hosts.file.path}")
    private String hostsFilePath;

    @Value("${localhost.domain}")
    private String localhostDomain;

    @Override
    public List<HostsAccess> getAllDomains() {
        try (FileReader fr = new FileReader(hostsFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            return br.lines()
                    .filter(l -> !l.trim().startsWith("#"))
                    .map(l -> l.trim().replace("\t", " ").split(" "))
                    .map(a -> {
                        List<String> normalized = new ArrayList<>();
                        Stream.of(a).forEach(line -> {
                            if (!line.isEmpty()) normalized.add(line);
                        });
                        return normalized;
                    })
                    .filter(l -> l.size() == 2 && !l.get(1).equalsIgnoreCase("localhost"))
                    .map(a -> new HostsAccess(a.get(1), AccessModificator.BLOCKED))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public List<HostsAccess> blockDomain(final String domain) {
        try (FileWriter fw = new FileWriter(hostsFilePath, true)) {
            fw.append("\n").append(localhostDomain).append("\t\t").append(domain);
        } catch (IOException e) {
            return null;
        }

        return getAllDomains();
    }

    @Override
    public void removeRuleForDomain(final String domain) {
        final String searchRegex = ".*" + domain + "$";
        final StringBuilder updatedFile = new StringBuilder();

        try (FileReader fr = new FileReader(hostsFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            br.lines().filter(a -> !a.matches(searchRegex)).forEachOrdered(a -> updatedFile.append(a).append("\n"));
            br.close();
        } catch (IOException ignored) {
        }

        try (FileWriter fw = new FileWriter(hostsFilePath)) {
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(updatedFile.toString());
            bw.flush();
        } catch (IOException ignored) {
        }
    }

//    ipconfig /flushdns
//    ping www.example.com -n 1
//    (nbtstat -R)
//    ipconfig /displaydns | more
}