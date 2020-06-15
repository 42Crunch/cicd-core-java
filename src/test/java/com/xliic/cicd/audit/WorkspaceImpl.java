package com.xliic.cicd.audit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.xliic.common.Workspace;

class WorkspaceImpl implements Workspace {

    private File workspace;

    File getDirectory() {
        return workspace;
    }

    WorkspaceImpl(String subfolder) {
        File resources = new File("src/test/resources");
        this.workspace = new File(resources, subfolder).getAbsoluteFile();
    }

    @Override
    public String read(String filename) throws IOException, InterruptedException {
        File file = new File(filename);
        if (!file.isAbsolute()) {
            file = new File(workspace, filename);
        }
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Override
    public boolean exists(String filename) throws IOException, InterruptedException {
        File file = new File(filename);
        if (!file.isAbsolute()) {
            file = new File(workspace, filename);
        }
        return file.exists();
    }

    @Override
    public String absolutize(String filename) throws IOException, InterruptedException {
        File file = new File(filename);
        if (!file.isAbsolute()) {
            file = new File(workspace, filename);
        }
        return file.getCanonicalPath();
    }
}