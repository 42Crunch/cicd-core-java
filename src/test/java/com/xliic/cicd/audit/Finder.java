package com.xliic.cicd.audit;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

class Finder implements OpenApiFinder {
    private WorkspaceImpl workspace;
    private String[] patterns;

    public Finder(WorkspaceImpl workspace) {
        this.workspace = workspace;
    }

    public void setPatterns(String[] patterns) {
        this.patterns = patterns;
    }

    public List<URI> find() {

        ArrayList<String> includes = new ArrayList<String>();
        ArrayList<String> excludes = new ArrayList<String>();

        for (String pattern : patterns) {
            if (pattern.startsWith("!")) {
                excludes.add(pattern.substring(1));
            } else {
                includes.add(pattern);
            }
        }

        FileSet fs = new FileSet();
        fs.setDir(workspace.getDirectory());
        fs.setProject(new Project());

        for (String include : includes) {
            fs.createInclude().setName(include);
        }

        for (String exclude : excludes) {
            fs.createExclude().setName(exclude);
        }

        DirectoryScanner ds = fs.getDirectoryScanner(new org.apache.tools.ant.Project());

        ArrayList<URI> result = new ArrayList<>();
        for (String filename : ds.getIncludedFiles()) {
            result.add(workspace.resolve(filename));
        }

        return result;

    }

}