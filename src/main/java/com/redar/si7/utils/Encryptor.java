package com.redar.si7.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.stream.Stream;

@Component
public class Encryptor {
    
    @Value("${charset.name}")
    private String charsetName;

    @Value("${encryption.shifting}")
    private int encryptionShifting;

    @Value("${encryption.randomizer}")
    private int encryptionRandomizer;

    private final int internalRandomizer = 0x8A;

    public String encryptWithKey(final String input, final String key) {
        final String firstPass = shiftTimes(input, encryptionShifting);
        final String secondPass = convertToHex(firstPass);
        final String thirdPass = addRandomizer(secondPass, key, 1);
        final String fourthPass = addEncryptionKey(thirdPass, key, false);

        return fourthPass;
    }

    public String decryptWithKey(final String input, final String key) {
        final String firstPass = addEncryptionKey(input, key, true);
        final String secondPass = addRandomizer(firstPass, key, -1);
        final String thirdPass = convertToLetters(secondPass);
        final String fourthPass = shiftTimes(thirdPass, encryptionShifting * -1);

        return fourthPass;
    }

    private String shiftTimes(String input, final int times) {
        if (times > 0) {
            for (int i = 0; i < times; i++) {
                input = input.charAt(input.length() - 1) + input.substring(0, input.length() - 1);
            }
        } else {
            for (int i = 0; i < Math.abs(times); i++) {
                input = input.substring(1) + input.charAt(0);
            }
        }

        return input;
    }

    private String convertToHex(final String input) {
        final StringBuilder hexedInput = new StringBuilder();

        for (byte charByte : input.getBytes(Charset.forName(charsetName))) {
            final String hex = doubleHex(Integer.toHexString(charByte));
            hexedInput.append(hex);
        }

        return hexedInput.toString();
    }

    private String convertToLetters(String input) {
        StringBuilder converted = new StringBuilder();
        for (int i = 0; i < input.length(); i += 2) {
            converted.append(new String(new byte[]{(byte) Integer.parseInt(input.substring(i, i + 2), 16)}, Charset.forName(charsetName)));
        }
        return converted.toString();
    }


    private String addRandomizer(final String input, final String key, final int multiplier) {
        final StringBuilder randomizedInput = new StringBuilder();
        int keySum = key.codePoints().sum();
        keySum = (keySum % (Integer.parseInt(String.valueOf(keySum).substring(0, 1)) * (int) Math.pow(10, (String.valueOf(keySum).length() - 1)))) + key.codePointAt(0);

        for (int i = 0; i < input.length(); i += 2) {
            final String hex = input.substring(i, i + 2);
            String randomizedChar = Integer.toHexString((byte) (Integer.parseInt(hex, 16) + ((internalRandomizer + i * 2 + keySum) * multiplier)));
            randomizedChar = doubleHex(randomizedChar.replace("ff", ""));

            randomizedInput.append(randomizedChar);
        }

        return randomizedInput.toString();
    }

    private String addEncryptionKey(final String input, final String key, final boolean reverse) {
        final StringBuilder newInput = new StringBuilder();
        final byte[] keyBytes = key.getBytes(Charset.forName(charsetName));
        int keyIndex = 0;

        for (int i = 0; i < input.length(); i += 2) {
            final String hex = input.substring(i, i + 2);
            final String hexKey = doubleHex(Integer.toHexString(keyBytes[keyIndex]));

            String resultHex;
            if (reverse) {
                resultHex = Integer.toHexString((byte) (Integer.parseInt(hex, 16) - Integer.parseInt(hexKey, 16)));
            } else {
                resultHex = Integer.toHexString((byte) (Integer.parseInt(hex, 16) + Integer.parseInt(hexKey, 16)));
            }
            newInput.append(doubleHex(resultHex));

            if (++keyIndex >= keyBytes.length) {
                keyIndex = 0;
            }
        }
        return newInput.toString();
    }

    private static String doubleHex(final String fullHex) {
        final String result = fullHex.replace("ff", "");

        if (result.length() > 2) {
            throw new IllegalArgumentException("The character '" + (char) Integer.parseInt(fullHex, 16) + "' needs more than 2 hexadecimal ending letters! Drowning...");
        }

        return "00".substring(result.length()) + result;
    }

}
