package com.redar.si7.service;

import com.redar.si7.domain.HostsAccess;

import java.util.List;

public interface DomainBlockerService {

    List<HostsAccess> getAllDomains();

    List<HostsAccess> blockDomain(String domain);

    void removeRuleForDomain(String domain);

}
