package org.jmqtt.starter.api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfo {

    private String userId;

    private String userName;

    public UserInfo(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

}
