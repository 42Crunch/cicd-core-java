package com.xliic.cicd.audit;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xliic.common.Workspace;
import com.xliic.openapi.bundler.Bundler;
import com.xliic.openapi.bundler.Document;
import com.xliic.openapi.bundler.Mapping;
import com.xliic.openapi.bundler.Parser;
import com.xliic.openapi.bundler.Serializer;

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

    public static Bundled bundle(String filename, Workspace workspace) throws AuditException {
        try {
            Parser parser = new Parser(workspace);
            Serializer serializer = new Serializer();
            Bundler bundler = new Bundler(serializer);
            Document document = parser.parse(workspace.absolutize(filename));
            Mapping mapping = bundler.bundle(document);
            return new Bundled(serializer.serialize(document), mapping);
        } catch (Exception e) {
            throw new AuditException(String.format("Failed to parse file: %s %s", filename, e), e);
        }
    }

    public static class Bundled {
        public final String json;
        public final Mapping mapping;

        public Bundled(String json, Mapping mapping) {
            this.json = json;
            this.mapping = mapping;
        }
    }
}