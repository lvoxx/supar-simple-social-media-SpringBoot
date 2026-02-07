package io.github.lvoxx.common_keys.user_service;

import lombok.AllArgsConstructor;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public abstract class UserPreferencesServiceLockerKeys {
    private static final String LOCK_KEY_PREFIX = "lock:preferences:";

    private static final String USER_PREFERENCES_UPDATE_LOCK_KEY = LOCK_KEY_PREFIX.concat("update:%s");

    public static final String getUserPreferencesUpdateLockKey(Long userId) {
        return USER_PREFERENCES_UPDATE_LOCK_KEY.formatted(userId);
    }

}
