package com.terubase.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class TeruBaseOpenAiClientTest {

    @Test
    void sendsApiKeyOnlyAsAuthorizationHeaderAndDoesNotLogIt() throws Exception {
        String apiKey = "request-only-secret";
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = """
                    {"choices":[{"message":{"content":"[]"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        Logger logger = Logger.getLogger(TeruBaseOpenAiClient.class.getName());
        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            TeruBaseProperties properties = new TeruBaseProperties();
            properties.setOpenAiChatCompletionsUrl("http://localhost:" + server.getAddress().getPort() + "/chat");
            TeruBaseOpenAiClient client = new TeruBaseOpenAiClient(new ObjectMapper(), properties, executorService);

            String result = client.generateInsertSql(new MockDataRequest(
                    1,
                    apiKey,
                    "Customer(id)",
                    null,
                    null,
                    false
            ));

            assertThat(result).isEqualTo("[]");
            assertThat(authorization).hasValue("Bearer " + apiKey);
            assertThat(messages).noneMatch(message -> message.contains(apiKey));
        } finally {
            logger.removeHandler(handler);
            server.stop(0);
        }
    }
}
