package com.redar.si7.service;

import com.redar.si7.domain.AccessModificator;
import com.redar.si7.domain.HostsAccess;
import com.redar.si7.utils.CommandExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FirewallAccessService implements DomainBlockerService {

    @Value("${firewall.rule.name}")
    private String firewallRuleName;

    @Override
    public List<HostsAccess> getAllDomains() {
        final String correctLineMatcher = "RemoteIP:";
        final List<HostsAccess> allIps = new ArrayList<>();
        final String activeRule = CommandExecutor.executeCommand("netsh advfirewall firewall show rule name=\"" + firewallRuleName + "\"");

        if (activeRule.contains("\n")) {
            if (activeRule.indexOf(correctLineMatcher) != activeRule.lastIndexOf(correctLineMatcher)) {
                throw new IllegalArgumentException("[!] There is multiple firewall rules with the name '" + firewallRuleName + "', crashing service to prevent any future system issues!");
            }

            Stream.of(activeRule.split("\n")).forEach(line -> {
                if (line.startsWith(correctLineMatcher)) {
                    Stream.of(line.split(correctLineMatcher)[1].trim().split(","))
                            .forEach(ip -> allIps.add(new HostsAccess(ip.split("/")[0], AccessModificator.BLOCKED)));
                }
            });
        }

        return allIps;
    }

    @Override
    public List<HostsAccess> blockDomain(String domain) {
        if (createRuleIfAbsent(domain)) {
            return Arrays.asList(new HostsAccess(domain, AccessModificator.BLOCKED));
        }

        final List<HostsAccess> updatedIps = getAllDomains();
        updatedIps.add(new HostsAccess(domain, AccessModificator.BLOCKED));

        final List<String> ips = updatedIps.stream().map(HostsAccess::getDomain).collect(Collectors.toList());
        final String allIpsCombined = StringUtils.collectionToCommaDelimitedString(ips);

        final String result = CommandExecutor.executeCommand("netsh advfirewall firewall set rule name=\"" + firewallRuleName + "\" dir=out new remoteip=" + allIpsCombined);
        if (result.contains("Updated") || result.contains("Ok.")) {
            return updatedIps;
        }

        return new ArrayList<>();
    }

    @Override
    public void removeRuleForDomain(String domain) {
        final List<HostsAccess> updatedIps = getAllDomains();

        if (!updatedIps.removeIf(ha -> domain.equals(ha.getDomain()))) {
            return;
        }

        if (updatedIps.size() > 0) {
            final List<String> ips = updatedIps.stream().map(HostsAccess::getDomain).collect(Collectors.toList());
            final String allIpsCombined = StringUtils.collectionToCommaDelimitedString(ips);

            CommandExecutor.executeCommand("netsh advfirewall firewall set rule name=\"" + firewallRuleName + "\" dir=out new remoteip=" + allIpsCombined);
        } else {
            CommandExecutor.executeCommand("netsh advfirewall firewall delete rule name=\"" + firewallRuleName + "\"");
        }
    }

    private boolean createRuleIfAbsent(final String ipToAppendIfAbsent) {
        final String activeRule = CommandExecutor.executeCommand("netsh advfirewall firewall show rule name=\"" + firewallRuleName + "\"");

        if (!activeRule.contains("\n")) {
            return CommandExecutor.executeCommand("netsh advfirewall firewall add rule name=\"" + firewallRuleName + "\" dir=out interface=any action=block remoteip=" + ipToAppendIfAbsent).equals("Ok.");
        }

        return false;
    }
}
