package com.xliic.cicd.audit;

import java.io.File;
import java.util.ArrayList;

import com.xliic.cicd.audit.OpenApiFinder;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

class Finder implements OpenApiFinder {
    private File workspace;
    private String[] patterns;

    public Finder(File workspace) {
        this.workspace = workspace;
    }

    public void setPatterns(String[] patterns) {
        this.patterns = patterns;
    }

    public String[] find() {

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
        fs.setDir(workspace);
        fs.setProject(new Project());

        for (String include : includes) {
            fs.createInclude().setName(include);
        }

        for (String exclude : excludes) {
            fs.createExclude().setName(exclude);
        }

        DirectoryScanner ds = fs.getDirectoryScanner(new org.apache.tools.ant.Project());

        return ds.getIncludedFiles();

    }

}