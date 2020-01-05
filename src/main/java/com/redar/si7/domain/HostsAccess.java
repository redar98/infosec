package com.redar.si7.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostsAccess {

    private String domain;

    private AccessModificator access;

}
