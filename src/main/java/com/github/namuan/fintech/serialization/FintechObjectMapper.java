package com.github.namuan.fintech.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class FintechObjectMapper {
    private FintechObjectMapper() {}

    public static ObjectMapper create() {
        return new ObjectMapper();
    }
}
