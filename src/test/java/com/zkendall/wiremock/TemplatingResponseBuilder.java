package com.zkendall.wiremock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplatingResponseBuilder extends ResponseTransformer {

    /**
     * Matches ${key:value}
     */
    public static final String TEMPLATE = "\\$\\{(.*?):(.*?)\\}";
    private static Pattern pattern = Pattern.compile(TEMPLATE);

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files) {
        final String requestBody = request.getBodyAsString();
        String responseBody = null;

        if (Strings.isNullOrEmpty(requestBody)) {
            return responseDefinition;
        }

        final Map<String, String> definitions = extractDefinitions(requestBody);

        if (definitions.isEmpty()) {
            return responseDefinition;
        }


        if (responseDefinition.getBody() != null) {
            String rawBody = new String(responseDefinition.getBody());
            responseBody = applyTemplate(definitions, rawBody);
        }

        if (!Strings.isNullOrEmpty(responseDefinition.getBodyFileName())) {
            BinaryFile responseBodyFile = files.getBinaryFileNamed(responseDefinition.getBodyFileName());
            String rawBody = new String(responseBodyFile.readContents());
            responseBody = applyTemplate(definitions, rawBody);
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
                .withBody(responseBody)
                .build();
    }

    /**
     * Returns map of key-value pairs as extracted from the groups of TEMPLATE: ${key:value}
     */
    public Map<String, String> extractDefinitions(String document) {
        Matcher matcher = pattern.matcher(document);
        Map results = new HashMap();
        while (matcher.find()) {
            results.put(matcher.group(1), matcher.group(2));
        }
        return results;
    }

    /**
     * Takes a map of key-value pairs, and replaces the values in the target where ${key}
     */
    public String applyTemplate(Map<String, String> definitions, String target) {
        for (Map.Entry<String, String> pair : definitions.entrySet()) {
            target = target.replace("${" + pair.getKey() + "}", pair.getValue());
        }
        return target;
    }

    @Override
    public String name() {
        return "templatedResponseBuilder";
    }
}
