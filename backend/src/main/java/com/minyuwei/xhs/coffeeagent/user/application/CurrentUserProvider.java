package com.minyuwei.xhs.coffeeagent.user.application;

public class CurrentUserProvider {
    public static final String LOCAL_USER_ID = "local-user";

    public String currentUserId() {
        return LOCAL_USER_ID;
    }
}
