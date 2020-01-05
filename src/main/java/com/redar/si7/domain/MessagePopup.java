package com.redar.si7.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MessagePopup {

    private String title;

    private String message;

    public static MessagePopup success(String message) {
        return new MessagePopup("Success", message);
    }

    public static MessagePopup fail(String message) {
        return new MessagePopup("Fail", message);
    }

    public static MessagePopup from(String title, String message) {
        return new MessagePopup(title, message);
    }

}
