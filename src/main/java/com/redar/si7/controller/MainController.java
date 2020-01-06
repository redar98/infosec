package com.redar.si7.controller;

import com.redar.si7.domain.Account;
import com.redar.si7.domain.HostsAccess;
import com.redar.si7.domain.MessagePopup;
import com.redar.si7.service.AccountManagementService;
import com.redar.si7.service.DomainBlockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AccountManagementService accountManagementService;

    private final DomainBlockerService domainBlockerService;

    @GetMapping("/")
    public String getRegister(Model model) {
        model.addAttribute("account", new Account());
        return "register";
    }

    @PostMapping("/")
    public String register(@ModelAttribute Account account, Model model) {
        MessagePopup result = accountManagementService.register(account);

        model.addAttribute("messagePopup", result);
        return result.getTitle().equals("Success") ? "login" : "register";
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

    @GetMapping("/websites")
    public String getHosts(Model model) {
        model.addAttribute("hostsList", domainBlockerService.getAllDomains());

        return "websites";
    }

    @PostMapping("/websites")
    public String blockHosts(@RequestParam String domain, Model model) {
        if (!domain.matches("^(http:\\/\\/|https:\\/\\/)?(www.)?((\\w+)\\.\\w*)*.[a-z]{1,3}.?([a-z]+)?$")) {
            model.addAttribute("messagePopup", MessagePopup.from("What?$#", "Enter a valid website domain!"));
            model.addAttribute("hostsList", domainBlockerService.getAllDomains());
            return "websites";
        }

        List<HostsAccess> blockedDomains = domainBlockerService.blockDomain(domain);

        if (blockedDomains == null) {
            model.addAttribute("messagePopup", MessagePopup.fail("Globally unique error occurred! Sorry :("));
            model.addAttribute("hostsList", new ArrayList<>());
        } else {
            model.addAttribute("hostsList", blockedDomains);
        }

        return "websites";
    }

    @PostMapping("/delete/host")
    public String deleteFromHosts(@RequestParam String domain, Model model) {
        domainBlockerService.removeRuleForDomain(domain);
        model.addAttribute("hostsList", domainBlockerService.getAllDomains());

        return "redirect:/websites";
    }

}
