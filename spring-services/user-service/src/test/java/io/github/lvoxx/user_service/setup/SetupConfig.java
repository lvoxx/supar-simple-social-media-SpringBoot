package io.github.lvoxx.user_service.setup;

import lombok.AllArgsConstructor;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public abstract class SetupConfig {

    @AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public abstract class Postgres {
        public static final String CONTAINER_IMAGE = "postgres:16-alpine";
        public static final String DATABASE_NAME = "testdb";
        public static final String USERNAME = "test";
        public static final String PASSWORD = "test";
    }
}
