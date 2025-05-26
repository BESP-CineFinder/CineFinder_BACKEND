package com.cinefinder.global.util.service;

import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Service
public class BadWordFilterService {

    @Value("${api.bad-word-filter.code.request-url}")
    private String filterRequestUrl;

    @Value("${api.bad-word-filter.code.service-key}")
    private String filterServiceKey;

    public String maskBadWords(String input) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", input);
            requestBody.put("mode", "FILTER");
            requestBody.put("callbackUrl", null);

            String jsonBody;
            try {
                jsonBody = objectMapper.writeValueAsString(requestBody);
            } catch (JsonProcessingException e) {
                throw new CustomException(ApiStatus._FILTERING_FAIL_REQUEST_JSON);
            }

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(filterRequestUrl))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", filterServiceKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new CustomException(ApiStatus._FILTERING_FAIL_API_COMMUNICATION);
            }

            try {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                return jsonNode.get("filtered").asText();
            } catch (Exception e) {
                throw new CustomException(ApiStatus._FILTERING_FAIL_RESPONSE_PARSING);
            }

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._FILTERING_FAIL_UNKNOWN);
        }
    }
}
