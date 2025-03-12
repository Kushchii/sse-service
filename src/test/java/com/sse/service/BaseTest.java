package com.sse.service;

import org.instancio.Instancio;

abstract class BaseTest {

    protected static <T> T random(Class<T> type) {
        return Instancio.create(type);
    }
}
