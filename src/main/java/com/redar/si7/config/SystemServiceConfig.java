package com.redar.si7.config;

import com.redar.si7.service.AccountManagementService;
import com.redar.si7.service.DomainBlockerService;
import com.redar.si7.service.LinuxAccessService;
import com.redar.si7.service.WindowsAccessService;
import com.redar.si7.utils.Encryptor;
import com.redar.si7.utils.OSValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemServiceConfig {

    @Bean
    public AccountManagementService accountManagementService(Encryptor encryptor,
                                                             @Value("${windows.users.file}") String windowsFilePath,
                                                             @Value("${unix.users.file}") String unixFilePath) {
        final String usersFilePath;
        if (OSValidator.isWindows()) {
            usersFilePath = windowsFilePath;
        } else if (OSValidator.isUnix()) {
            usersFilePath = unixFilePath;
        } else {
            throw new IllegalArgumentException("Operating System '" + OSValidator.getOS() + "' is not supported by this application.");
        }

        return new AccountManagementService(encryptor, usersFilePath);
    }

    @Bean
    public DomainBlockerService domainBlockerService() {
        if (OSValidator.isWindows()) {
            return new WindowsAccessService();
        } else if (OSValidator.isUnix()) {
            return new LinuxAccessService();
        } else {
            throw new IllegalArgumentException("Operating System '" + OSValidator.getOS() + "' is not supported by this application.");
        }
    }

}
