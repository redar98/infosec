package com.redar.si7.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account implements OneLiner {

    private String username;

    private String password;

    @Override
    public String combineText(final String separator) {
        return username + separator + password;
    }
}
