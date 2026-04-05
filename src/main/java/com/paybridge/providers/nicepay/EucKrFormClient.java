package com.paybridge.providers.nicepay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paybridge.support.error.ErrorCode;
import com.paybridge.support.error.PayBridgeException;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EucKrFormClient {

    private static final Charset REQUEST_CHARSET = Charset.forName("EUC-KR");

    private final ObjectMapper objectMapper;

    public EucKrFormClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> postForm(URI uri, Map<String, String> formData, Duration connectTimeout, Duration readTimeout) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=EUC-KR")
                    .header("Accept", "application/json, text/plain, */*")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(encodeForm(formData)))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            Charset responseCharset = extractResponseCharset(response).orElse(StandardCharsets.UTF_8);
            String body = new String(response.body(), responseCharset);
            return parseResponseBody(body, responseCharset);
        } catch (IOException ex) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "Failed to call NicePay over the network.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "NicePay call was interrupted.");
        }
    }

    byte[] encodeForm(Map<String, String> formData) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(entry.getKey(), REQUEST_CHARSET));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), REQUEST_CHARSET));
            first = false;
        }
        return builder.toString().getBytes(REQUEST_CHARSET);
    }

    Map<String, String> parseResponseBody(String body, Charset responseCharset) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            return parseJsonBody(trimmed);
        }
        return parseKeyValueBody(trimmed, responseCharset);
    }

    private Map<String, String> parseJsonBody(String body) {
        try {
            Map<String, Object> raw = objectMapper.readValue(body, new TypeReference<>() { });
            Map<String, String> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> result.put(key, value == null ? null : String.valueOf(value)));
            return result;
        } catch (IOException ex) {
            throw new PayBridgeException(HttpStatus.BAD_GATEWAY, ErrorCode.PROVIDER_ERROR, "NicePay returned JSON that could not be parsed.");
        }
    }

    private Map<String, String> parseKeyValueBody(String body, Charset responseCharset) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0], responseCharset);
            String value = parts.length > 1 ? urlDecode(parts[1], responseCharset) : "";
            result.put(key, value);
        }
        return result;
    }

    private String urlDecode(String raw, Charset responseCharset) {
        return URLDecoder.decode(raw, responseCharset);
    }

    private Optional<Charset> extractResponseCharset(HttpResponse<byte[]> response) {
        return response.headers()
                .firstValue("Content-Type")
                .flatMap(contentType -> {
                    String[] tokens = contentType.split(";");
                    for (String token : tokens) {
                        String trimmed = token.trim();
                        if (trimmed.toLowerCase().startsWith("charset=")) {
                            return Optional.of(Charset.forName(trimmed.substring("charset=".length())));
                        }
                    }
                    return Optional.empty();
                });
    }
}
