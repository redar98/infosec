package com.redar.si7.controller;

import com.redar.si7.domain.Account;
import com.redar.si7.domain.HostsAccess;
import com.redar.si7.domain.MessagePopup;
import com.redar.si7.service.AccountManagementService;
import com.redar.si7.service.HostsAccessService;
import com.redar.si7.service.LinuxIptablesService;
import com.redar.si7.utils.OSValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import sun.security.krb5.internal.HostAddress;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AccountManagementService accountManagementService;

    private final HostsAccessService hostsAccessService;

    private final LinuxIptablesService linuxIptablesService;

    @GetMapping("/")
    public String getRegister(Model model) {
        model.addAttribute("account", new Account());
        return "register";
    }

    @PostMapping("/")
    public String register(@ModelAttribute Account account, Model model) {
        MessagePopup result = accountManagementService.register(account);

        model.addAttribute("messagePopup", result);
        return result.getTitle().equals("Success")? "redirect:/login": "register";
    }

    @GetMapping("/login")
    public String getLogin(Model model) {
        model.addAttribute("account", new Account());
        return "login";
    }

    @PostMapping("/login")
    public String login(HttpSession session, @ModelAttribute Account account, Model model) {
        boolean success = accountManagementService.login(account);

        if (success) {
            model.addAttribute("messagePopup", MessagePopup.success("You have just logged in '" + account.getUsername() + "'!"));
        } else {
            model.addAttribute("messagePopup", MessagePopup.fail("You have not registered before! Account not found."));
            model.addAttribute("account", new Account());
            return "login";
        }

        session.setAttribute("currentUser", account.getUsername());
        model.addAttribute("accountList", accountManagementService.getAllDecryptedAccountsExcept(session.getAttribute("currentUser").toString()));
        return "accounts";
    }

    @PostMapping("/delete")
    public String deleteAccount(HttpSession session, @RequestParam String username, Model model) {
        accountManagementService.deleteAccountByUsername(username);

        model.addAttribute("accountList", accountManagementService.getAllDecryptedAccountsExcept(session.getAttribute("currentUser").toString()));
        return "accounts";
    }

    @GetMapping("/hosts")
    public String getHosts(Model model) {
        model.addAttribute("hostsList", hostsAccessService.getAllDomainsFromHosts());

        return "hosts";
    }

    @PostMapping("/hosts")
    public String blockHosts(@RequestParam String domain, Model model) {
        if(domain.matches("^(http:\\/\\/|https:\\/\\/)?(www.)?((\\w+)\\.\\w*)*.[a-z]{1,3}.?([a-z]+)?$")) {
            if (OSValidator.isUnix()) {
                List<HostsAccess> blockedDomains = linuxIptablesService.blockDomain(domain);

                if (blockedDomains == null) {
                    model.addAttribute("messagePopup", MessagePopup.fail("Globally unique error occurred! Sorry."));
                    model.addAttribute("hostsList", new ArrayList<>());
                } else {
                    model.addAttribute("hostsList", blockedDomains);
                }
                return "hosts";
            } else if (OSValidator.isWindows()) {
                if (!hostsAccessService.blockDomain(domain)) {
                    model.addAttribute("messagePopup", MessagePopup.fail("Your system denied my request to edit 'hosts' file :("));
                }
            }
        } else {
            model.addAttribute("messagePopup", MessagePopup.from("What?$#", "Enter a valid website domain!"));
        }

        model.addAttribute("hostsList", hostsAccessService.getAllDomainsFromHosts());

        return "hosts";
    }

    @PostMapping("/delete/host")
    public String deleteFromHosts(@RequestParam String domain, Model model) {
        if (OSValidator.isUnix()) {

        } else if (OSValidator.isWindows()) {
            hostsAccessService.removeDomainFromHosts(domain);
        }

        model.addAttribute("hostsList", hostsAccessService.getAllDomainsFromHosts());
        return "redirect:/hosts";
    }

}
