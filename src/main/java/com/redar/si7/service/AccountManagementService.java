package com.redar.si7.service;

import com.redar.si7.domain.Account;
import com.redar.si7.domain.MessagePopup;
import com.redar.si7.utils.Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccountManagementService {

    @Value("${users.file.path}")
    private String usersFilePath;

    @Value("${encryption.key}")
    private String encryptionKey;

    @Value("${user.data.separator.hex}")
    private int dataSeparator;

    @Value("${charset.name}")
    private String charsetName;

    private final Encryptor encryptor;

    public MessagePopup register(final Account account) {
        if (!account.getUsername().matches("\\w{4,20}")) {
            return MessagePopup.from("Rules Violated", "Username length should be between 4 and 20 letters!\nWithout any special characters.");
        } else if (!canRegister(account)) {
            return MessagePopup.from("Username Unavailable", "Username '" + account.getUsername() + "' is already being user by someone else!");
        }

        final String encryptedText = convertToEncrypted(account);

        try (FileWriter fw = new FileWriter(usersFilePath, true)) {
            fw.append(encryptedText).append("\n");
        } catch (IOException e) {
            return MessagePopup.from("Resource Access Violation", "Failed to register you in our secured database at the moment!\nCheck for permissions.");
        }

        return MessagePopup.success("Your credentials are now securely encrypted on our side!\nRest easily now.");
    }

    public boolean login(final Account account) {
        final String encryptedAccount = convertToEncrypted(account);
        try (FileReader fr = new FileReader(usersFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            Optional<String> foundAccount = br.lines().filter(encryptedAccount::equals).findAny();
            if (foundAccount.isPresent()) {
                return true;
            }

        } catch (IOException ignored) {
        }

        return false;
    }

    public List<Account> getAllDecryptedAccountsExcept(final String exceptUser) {
        try (FileReader fr = new FileReader(usersFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            return br.lines().map(a -> {
                try {
                    final String[] combination = encryptor.decryptWithKey(a, encryptionKey).split(new String(new byte[]{(byte) dataSeparator}, Charset.forName(charsetName)));
                    if (!exceptUser.equals(combination[0])) {
                        return new Account(combination[0], combination[1]);
                    }
                } catch (Exception ignored) {
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean canRegister(final Account newAccount) {
        final String separator = new String(new byte[]{(byte) (char) dataSeparator}, Charset.forName(charsetName));
        final String searchRegex = "^" + newAccount.getUsername() + separator + "(.+)";

        try (FileReader fr = new FileReader(usersFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            Optional<String> foundAccount = br.lines().filter(a -> {
                try {
                    return encryptor.decryptWithKey(a, encryptionKey).matches(searchRegex);
                } catch (Exception ex) {
                    return false;
                }
            }).findAny();

            if (foundAccount.isPresent()) {
                return false;
            }
        } catch (IOException ignored) {
        }

        return true;
    }

    private String convertToEncrypted(final Account account) {
        final String separator = new String(new byte[]{(byte) (char) dataSeparator}, Charset.forName(charsetName));
        final String combinedText = account.combineText(separator);

        return encryptor.encryptWithKey(combinedText, encryptionKey);
    }

    public void deleteAccountByUsername(final String username) {
        final String separator = new String(new byte[]{(byte) (char) dataSeparator}, Charset.forName(charsetName));
        final String searchRegex = "^" + username + separator + "(.+)";
        final StringBuilder updatedFile = new StringBuilder();

        try (FileReader fr = new FileReader(usersFilePath)) {
            BufferedReader br = new BufferedReader(fr);

            br.lines().filter(a -> {
                try {
                    return !encryptor.decryptWithKey(a, encryptionKey).matches(searchRegex);
                } catch (Exception ex) {
                    return false;
                }
            }).forEachOrdered(a -> updatedFile.append(a).append("\n"));
            br.close();
        } catch (IOException ignored) {
        }

        try (FileWriter fw = new FileWriter(usersFilePath)) {
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(updatedFile.toString());
            bw.flush();
        } catch (IOException ignored) {
        }
    }

}