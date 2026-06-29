package com.juege.oshrelease.config;

import com.juege.oshrelease.model.AppUser;

public final class RequestContext {

    private static final ThreadLocal<AppUser> CURRENT_USER = new ThreadLocal<AppUser>();

    private RequestContext() {
    }

    public static void setCurrentUser(AppUser user) {
        CURRENT_USER.set(user);
    }

    public static AppUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}

