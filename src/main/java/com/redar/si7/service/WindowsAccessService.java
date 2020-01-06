package com.redar.si7.service;

import com.redar.si7.domain.AccessModificator;
import com.redar.si7.domain.HostsAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class WindowsAccessService implements DomainBlockerService {

    @Value("${hosts.file.path}")
    private String hostsFilePath;

    @Value("${localhost.domain}")
    private String localhostDomain;

    private final FirewallAccessService firewallAccessService;

    public static final String URL_ADDRESS_REGEX = "^(http:\\/\\/|https:\\/\\/)?(www.)?((\\w+)\\.\\w*)*.[a-z]{1,3}.?([a-z]+)?$";

    public static final String IP_ADDRESS_REGEX = "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";

    @Override
    public List<HostsAccess> getAllDomains() {
        try (FileReader fr = new FileReader(hostsFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            List<HostsAccess> allDomains = br.lines()
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

            try {
                allDomains.addAll(firewallAccessService.getAllDomains());
            } catch (Exception ignored) {
            }
            return allDomains;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public List<HostsAccess> blockDomain(final String domain) {
        if (domain.matches(IP_ADDRESS_REGEX)) {
            firewallAccessService.blockDomain(domain);
            return getAllDomains();
        }

        try (FileWriter fw = new FileWriter(hostsFilePath, true)) {
            fw.append("\n").append(localhostDomain).append("\t\t").append(domain);
        } catch (IOException e) {
            return null;
        }

        return getAllDomains();
    }

    @Override
    public void removeRuleForDomain(final String domain) {
        if (domain.matches(IP_ADDRESS_REGEX)) {
            firewallAccessService.removeRuleForDomain(domain);
            return;
        }

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

            bw.write(updatedFile.substring(0, updatedFile.lastIndexOf("\n")));
            bw.flush();
        } catch (IOException ignored) {
        }
    }

//    ipconfig /flushdns
//    ping www.example.com -n 1
//    (nbtstat -R)
//    ipconfig /displaydns | more
}
