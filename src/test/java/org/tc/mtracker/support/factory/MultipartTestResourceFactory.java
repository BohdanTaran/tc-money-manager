package org.tc.mtracker.support.factory;

import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

public final class MultipartTestResourceFactory {

    private MultipartTestResourceFactory() {
    }

    public static ByteArrayResource resource(String filename, String content) {
        return resource(filename, content.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteArrayResource resource(String filename, byte[] content) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
