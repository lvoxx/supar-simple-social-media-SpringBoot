package io.github.lvoxx.common_keys.user_service;

import lombok.AllArgsConstructor;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class UserServiceCacheKeys {
    public static final String USERS_VALUE = "cache:users";

    public static final String PUT_ID = "#id";
    public static final String PUT_USERNAME = "#username";
    public static final String PUT_EMAIL = "#email";

    public static final String EVICT_ID = "#result.id";
    public static final String EVICT_USERNAME = "#result.username";
    public static final String EVICT_EMAIL = "#result.email";

    public static final boolean EVICT_ALL = true;
}
