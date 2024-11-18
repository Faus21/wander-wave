package com.dama.wanderwave.user;

import java.util.List;


public record BlackList(List<String> userIds) {

    public boolean addUser (String userId) {
        return userIds.add(userId);
    }

    public boolean removeUser (String userId) {
        return userIds.remove(userId);
    }
}