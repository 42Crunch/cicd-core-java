package com.xliic.cicd.audit;

import java.io.File;
import java.net.URI;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.xliic.common.Workspace;

import org.yaml.snakeyaml.util.UriEncoder;

class WorkspaceImpl implements Workspace {

    private URI workspace;
    private File directory;

    File getDirectory() {
        return directory;
    }

    WorkspaceImpl(String subfolder) throws IOException {
        this.workspace = new File("src/test/resources", subfolder).getCanonicalFile().toURI();
        this.directory = new File("src/test/resources", subfolder);
    }

    @Override
    public String read(URI uri) throws IOException, InterruptedException {
        File file = new File(uri.getPath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Override
    public boolean exists(URI uri) throws IOException, InterruptedException {
        File file = new File(uri);
        return file.exists();
    }

    @Override
    public URI resolve(String filename) {
        ArrayList<String> encoded = new ArrayList<>();
        for (Path segment : Paths.get(filename)) {
            encoded.add(UriEncoder.encode(segment.toString()));
        }
        return workspace.resolve(String.join("/", encoded));
    }

    @Override
    public URI relativize(URI uri) {
        return workspace.relativize(uri);
    }

}