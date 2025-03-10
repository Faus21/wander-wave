package com.dama.wanderwave.user;

import java.util.Set;


public record BlackList(Set<String> userIds) {
    public boolean addUser (String userId) {
        return userIds.add(userId);
    }

    public boolean removeUser (String userId) {
        return userIds.remove(userId);
    }
}