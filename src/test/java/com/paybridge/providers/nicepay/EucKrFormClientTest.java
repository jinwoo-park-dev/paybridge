package com.paybridge.providers.nicepay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EucKrFormClientTest {

    private HttpServer server;
    private EucKrFormClient client;
    private volatile String lastRequestBody;

    @BeforeEach
    void setUp() throws IOException {
        client = new EucKrFormClient(new ObjectMapper());
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postsEucKrFormAndParsesUtf8KvResponse() throws Exception {
        server.createContext("/approve", new KvHandler(Charset.forName("EUC-KR"), StandardCharsets.UTF_8));
        Map<String, String> response = client.postForm(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/approve"),
                Map.of("GoodsName", "테스트상품", "Amt", "1004"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );

        assertThat(lastRequestBody).contains("GoodsName=%C5%D7%BD%BA%C6%AE%BB%F3%C7%B0");
        assertThat(response.get("ResultCode")).isEqualTo("3001");
        assertThat(response.get("ResultMsg")).isEqualTo("정상처리되었습니다.");
    }

    @Test
    void parsesJsonResponseWhenProviderReturnsJson() throws Exception {
        server.createContext("/json", exchange -> {
            byte[] body = "{\"ResultCode\":\"3001\",\"ResultMsg\":\"정상처리되었습니다.\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        Map<String, String> response = client.postForm(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/json"),
                Map.of("MID", "nictest04m"),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );

        assertThat(response.get("ResultCode")).isEqualTo("3001");
        assertThat(response.get("ResultMsg")).isEqualTo("정상처리되었습니다.");
    }

    private final class KvHandler implements HttpHandler {
        private final Charset requestCharset;
        private final Charset responseCharset;

        private KvHandler(Charset requestCharset, Charset responseCharset) {
            this.requestCharset = requestCharset;
            this.responseCharset = responseCharset;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            lastRequestBody = new String(requestBytes, requestCharset);
            String body = "ResultCode=3001&ResultMsg=" + java.net.URLEncoder.encode("정상처리되었습니다.", responseCharset);
            byte[] responseBytes = body.getBytes(responseCharset);
            exchange.getResponseHeaders().set("Content-Type", "application/x-www-form-urlencoded; charset=" + responseCharset.name());
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }
    }
}
