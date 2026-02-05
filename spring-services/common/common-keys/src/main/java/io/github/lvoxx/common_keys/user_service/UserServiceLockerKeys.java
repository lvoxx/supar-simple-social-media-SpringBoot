package io.github.lvoxx.common_keys.user_service;

import lombok.AllArgsConstructor;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public abstract class UserServiceLockerKeys {
    private static final String LOCK_KEY_PREFIX = "lock:user:";

    private static final String USER_CREATION_LOCK_KEY = LOCK_KEY_PREFIX + "creation:%s";
    private static final String USER_UPDATE_LOCK_KEY = LOCK_KEY_PREFIX + "update:%s";
    private static final String USER_DELETION_LOCK_KEY = LOCK_KEY_PREFIX + "deletion:%s";

    public static final String getUserCreationLockKey(String username) {
        return USER_CREATION_LOCK_KEY.formatted(username);
    }

    public static final String getUserUpdateLockKey(String username) {
        return USER_UPDATE_LOCK_KEY.formatted(username);
    }

    public static final String getUserDeletionLockKey(String username) {
        return USER_DELETION_LOCK_KEY.formatted(username);
    }

}
