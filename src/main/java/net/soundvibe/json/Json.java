package net.soundvibe.json;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.zalando.jackson.datatype.money.MoneyModule;

import java.io.*;

public final class Json {

    private Json() {}

    private static final ObjectMapper JSON_MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModules(new MoneyModule(), new JavaTimeModule(), new ParameterNamesModule());
        return objectMapper;
    }

    public static <T> T parse(String json, Class<T> tClass) {
        try {
            return JSON_MAPPER.readValue(json, tClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String toString(Object object) {
        try {
            return JSON_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
