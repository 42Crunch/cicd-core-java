/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.net.URI;

import com.xliic.cicd.audit.model.OpenApiFile;
import com.xliic.cicd.audit.model.api.ErrorMessage;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.common.Workspace;

public class Util {
    static int MAX_NAME_LEN = 64;

    static Maybe<Boolean> isOpenApiFile(URI file, Workspace workspace) {
        try {
            OpenApiFile openApiFile = JsonParser.parse(workspace.read(file), OpenApiFile.class,
                    file.getPath().toLowerCase().endsWith(".yaml") || file.getPath().toLowerCase().endsWith(".yml"));
            return new Maybe<Boolean>(openApiFile.isOpenApi());
        } catch (Exception ex) {
            return new Maybe<Boolean>(new ErrorMessage(String.format("Filed to parse a file '%s': %s",
                    workspace.relativize(file).getPath(), ex.getMessage())));
        }
    }

    public static String makeName(String name) {
        String mangled = name.replaceAll("[^A-Za-z0-9_\\-\\.\\ ]", " ");
        if (mangled.length() > MAX_NAME_LEN) {
            return mangled.substring(0, MAX_NAME_LEN);
        }
        return mangled;
    }

    public static String makeTechnicalCollectionName(String repoName, String branchName) {
        // FIXME check for max name len and for prohibited chars
        return String.format("%s@@%s", repoName, branchName);
    }
}
