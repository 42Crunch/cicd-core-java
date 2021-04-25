/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.xliic.cicd.audit.JsonParser.Bundled;
import com.xliic.cicd.audit.client.Client;
import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.client.RemoteApiMap;
import com.xliic.cicd.audit.config.model.Mapping;
import com.xliic.cicd.audit.model.api.Api;
import com.xliic.cicd.audit.model.api.ApiCollection;
import com.xliic.cicd.audit.model.api.ApiCollections;
import com.xliic.cicd.audit.model.api.ErrorMessage;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.cicd.audit.model.api.TechnicalCollection;
import com.xliic.common.Workspace;
import com.xliic.openapi.bundler.BundlingException;
import com.xliic.openapi.bundler.ReferenceResolutionFailure;

public class DiscoveryAuditor {
    private String collectionId;
    private Logger logger;
    private Client client;
    private Workspace workspace;

    public DiscoveryAuditor(Workspace workspace, Client client, Logger logger) {
        this.client = client;
        this.logger = logger;
        this.workspace = workspace;
    }

    RemoteApiMap audit(Workspace workspace, OpenApiFinder finder, String repoName, String branchName, String source,
            String[] search, Mapping mapping) throws IOException, InterruptedException, AuditException {
        DiscoveredOpenApiFiles discovered = discoverOpenApiFiles(workspace, finder, search, mapping);

        // collect list of successfully detected OpenAPI files
        ArrayList<URI> openApiFilenames = new ArrayList<>();
        for (Entry<URI, Maybe<Boolean>> openapi : discovered.entrySet()) {
            if (openapi.getValue().isOk()) {
                openApiFilenames.add(openapi.getKey());
            }
        }

        collectionId = createOrFindCollectionId(Util.makeTechnicalCollectionName(repoName, branchName), source);

        RemoteApiMap remoteApis = uploadFilesToCollection(openApiFilenames, workspace, collectionId);

        // add files which failed to parse to the list of errors
        for (Entry<URI, Maybe<Boolean>> openapi : discovered.entrySet()) {
            if (openapi.getValue().isError()) {
                remoteApis.put(openapi.getKey(), new Maybe<>(openapi.getValue().getError()));
            }
        }

        return remoteApis;
    }

    private DiscoveredOpenApiFiles discoverOpenApiFiles(Workspace workspace, OpenApiFinder finder, String[] search,
            Mapping mapping) throws IOException, InterruptedException, AuditException {
        DiscoveredOpenApiFiles discovered = new DiscoveredOpenApiFiles();

        List<URI> files = findOpenapiFiles(workspace, finder, search);
        String filenames = files.stream().map(file -> workspace.relativize(file).getPath())
                .collect(Collectors.joining(","));
        logger.info(String.format("Files matching search criteria: %s", filenames));
        for (URI file : files) {
            if (!mapping.containsKey(workspace.relativize(file).getPath())) {
                Maybe<Boolean> openapi = Util.isOpenApiFile(file, workspace);
                // put discovered OpenAPI files onto the list of discovered ones
                // also add there parsing errors
                if (openapi.isOk() && openapi.getResult() == true || openapi.isError()) {
                    discovered.put(file, openapi);
                }
            }
        }

        String discoveredFilenames = discovered.keySet().stream().map(file -> workspace.relativize(file).getPath())
                .collect(Collectors.joining(","));
        logger.info(String.format("Discovered OpenAPI files: %s", discoveredFilenames));

        return discovered;
    }

    private RemoteApiMap uploadFilesToCollection(ArrayList<URI> files, Workspace workspace, String collectionId)
            throws IOException, AuditException {
        RemoteApiMap uploaded = new RemoteApiMap();

        Maybe<ApiCollection> apis = client.listApis(collectionId);
        if (apis.isError()) {
            throw new AuditException("Unable to list collection: " + apis.getError().getMessage());

        }

        List<ApiAction> actions = createApiActions(apis.getResult(), files);
        for (ApiAction action : actions) {
            switch (action.action) {
            case ApiAction.DELETE:
                deleteApi(action.apiId);
                break;
            case ApiAction.CREATE:
                uploaded.put(action.file, createApi(action.file));
                break;
            case ApiAction.UPDATE:
                uploaded.put(action.file, updateApi(action.file, action.apiId));
                break;
            }
        }

        return uploaded;
    }

    private void deleteApi(String apiId) throws IOException, AuditException {
        logger.info(String.format("Removing api from collection, because it's no longer present: %s", apiId));
        Maybe<String> deleted = client.deleteApi(apiId);
        if (deleted.isError()) {
            throw new AuditException("Unable to delete api: " + deleted.getError().getMessage());
        }
    }

    private Maybe<RemoteApi> createApi(URI file) throws IOException {
        String relative = workspace.relativize(file).getPath();
        logger.info(String.format("Creating new API for: %s", relative));
        try {
            Bundled bundled = JsonParser.bundle(file, workspace);
            String title = "No Title";
            JsonNode root = bundled.document.root.node;
            if (root.get("info") != null && root.get("info").get("title") != null) {
                title = root.get("info").get("title").asText();
            }
            String apiName = Util.makeName(title);
            Maybe<RemoteApi> api = client.createTechnicalApi(collectionId, relative, apiName, bundled.json);
            if (api.isOk()) {
                api.getResult().setMapping(bundled.mapping);
            }
            return api;
        } catch (AuditException e) {
            // in case of parsing error during bundling, do not stop audit
            return new Maybe<RemoteApi>(new ErrorMessage(e));
        } catch (BundlingException e) {
            for (ReferenceResolutionFailure failure : e.getFailures()) {
                logger.error(String.format("Failed to resolve reference in %s at %s: %s", failure.sourceFile,
                        failure.sourcePointer, failure.message));
            }
            return new Maybe<RemoteApi>(new ErrorMessage(e));
        }
    }

    private Maybe<RemoteApi> updateApi(URI file, String apiId) throws IOException {
        String relative = workspace.relativize(file).getPath();
        logger.info(String.format("Updating existing API %s for: %s", apiId, relative));
        try {
            Bundled bundled = JsonParser.bundle(file, workspace);
            Maybe<RemoteApi> api = client.updateApi(apiId, bundled.json);
            if (api.isOk()) {
                api.getResult().setMapping(bundled.mapping);
            }
            return api;
        } catch (AuditException e) {
            // in case of parsing error during bundling, do not stop audit
            return new Maybe<RemoteApi>(new ErrorMessage(e));
        } catch (BundlingException e) {
            for (ReferenceResolutionFailure failure : e.getFailures()) {
                logger.error(String.format("Failed to resolve reference in %s at %s: %s", failure.sourceFile,
                        failure.sourcePointer, failure.message));
            }
            return new Maybe<RemoteApi>(new ErrorMessage(e));
        }

    }

    private List<ApiAction> createApiActions(ApiCollection result, ArrayList<URI> filenames) {
        List<ApiAction> actions = new ArrayList<>();

        HashMap<URI, String> fileToId = new HashMap<>();
        HashSet<URI> localFilenames = new HashSet<>();
        HashSet<URI> remoteFilenames = new HashSet<>();

        for (Api api : result.list) {
            URI filename = workspace.resolve(api.desc.technicalName);
            fileToId.put(filename, api.desc.id);
            remoteFilenames.add(filename);
        }

        for (URI filename : filenames) {
            localFilenames.add(filename);
            if (fileToId.containsKey(filename)) {
                actions.add(new ApiAction(ApiAction.UPDATE, filename, fileToId.get(filename)));
            } else {
                actions.add(new ApiAction(ApiAction.CREATE, filename));
            }

        }

        // for every remote api which can't be found amongst
        // the local filenames, issue "delete" action
        for (URI filename : remoteFilenames) {
            if (!localFilenames.contains(filename)) {
                actions.add(new ApiAction(ApiAction.DELETE, filename, fileToId.get(filename)));
            }

        }

        return actions;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private String createOrFindCollectionId(String collectionName, String source) throws AuditException, IOException {

        // check existing collections to see if collection with collectionName already
        // exists
        Maybe<TechnicalCollection> collection = client.readTechnicalCollection(collectionName);
        if (!collection.isError()) {
            return collection.getResult().id;
        } else {
            if (collection.getError().getHttpStatus() == 404) {
                Maybe<ApiCollections.ApiCollection> cc = client.createTechnicalCollection(collectionName, source);
                if (cc.isError()) {
                    throw new AuditException("Unable to create collection: " + collection.getError().getMessage());
                }
                return cc.getResult().desc.id;
            }
            throw new AuditException("Unable to list collection: " + collection.getError().getMessage());
        }
    }

    private List<URI> findOpenapiFiles(Workspace workspace, OpenApiFinder finder, String[] search)
            throws IOException, InterruptedException, AuditException {
        finder.setPatterns(search);
        List<URI> openApiFiles = finder.find();
        return openApiFiles;
    }

    class ApiAction {
        final static int CREATE = 1;
        final static int UPDATE = 2;
        final static int DELETE = 3;

        final int action;
        final URI file;
        final String apiId;

        ApiAction(int action, URI file, String apiId) {
            this.action = action;
            this.file = file;
            this.apiId = apiId;
        }

        ApiAction(int action, URI file) {
            this.action = action;
            this.file = file;
            this.apiId = null;
        }
    }
}
