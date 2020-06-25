package com.xliic.cicd.audit;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonParser {

    private static ObjectMapper getMapper(boolean yaml) {
        ObjectMapper mapper;
        if (yaml) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static <T> T parse(byte[] json, Class<T> contentClass) throws IOException {
        return getMapper(false).readValue(json, contentClass);
    }

    public static <T> T parse(String json, Class<T> contentClass) throws IOException {
        return getMapper(false).readValue(json, contentClass);
    }

    public static <T> T parse(String data, Class<T> contentClass, boolean isYaml) throws IOException {
        return getMapper(isYaml).readValue(data, contentClass);
    }

    public static <T> T parse(InputStream json, Class<T> contentClass) throws IOException {
        return getMapper(false).readValue(json, contentClass);
    }
}