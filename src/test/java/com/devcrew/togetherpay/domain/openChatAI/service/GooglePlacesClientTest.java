package com.devcrew.togetherpay.domain.openChatAI.service;

import com.devcrew.togetherpay.domain.openChatAI.dto.request.OriginType;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.RecommendationCriteria;
import com.devcrew.togetherpay.domain.openChatAI.dto.request.TransportMode;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePhotoMedia;
import com.devcrew.togetherpay.domain.openChatAI.google.GooglePlace;
import com.devcrew.togetherpay.global.error.ErrorCode;
import com.devcrew.togetherpay.global.error.exception.BusinessException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GooglePlacesClientTest {

    private HttpServer server;
    private GooglePlacesClient client;
    private CapturedRequest capturedRequest;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        client = new GooglePlacesClient(WebClient.builder());
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "placesBaseUrl", "http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void searchTextSendsRequiredHeadersAndRequestBody() {
        server.createContext("/v1/places:searchText", exchange -> {
            capturedRequest = CapturedRequest.from(exchange);
            respondJson(exchange, 200, """
                    {
                      "places": [
                        {
                          "id": "place-1",
                          "displayName": {"text": "라멘집", "languageCode": "ko"},
                          "formattedAddress": "Tokyo",
                          "location": {"latitude": 35.6938, "longitude": 139.7034},
                          "rating": 4.4,
                          "userRatingCount": 120,
                          "googleMapsUri": "https://maps.google.com/place-1",
                          "primaryType": "restaurant"
                        }
                      ]
                    }
                    """);
        });

        List<GooglePlace> result = client.searchText(criteria(), "restaurant", "맛집");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("place-1");
        assertThat(result.get(0).displayName().text()).isEqualTo("라멘집");

        assertThat(capturedRequest.method()).isEqualTo("POST");
        assertThat(capturedRequest.path()).isEqualTo("/v1/places:searchText");
        assertThat(capturedRequest.apiKey()).isEqualTo("test-api-key");
        assertThat(capturedRequest.fieldMask())
                .contains("places.id")
                .contains("places.regularOpeningHours")
                .contains("places.primaryTypeDisplayName");
        assertThat(capturedRequest.body())
                .contains("\"textQuery\":\"라멘 도쿄 신주쿠\"")
                .contains("\"includedType\":\"restaurant\"")
                .contains("\"pageSize\":10")
                .contains("\"latitude\":35.6938")
                .contains("\"radius\":1200.0");
    }

    @Test
    void getPlaceDetailConvertsGoogleErrorToBusinessException() {
        server.createContext("/v1/places/place-1", exchange -> {
            capturedRequest = CapturedRequest.from(exchange);
            respondJson(exchange, 500, """
                    {"error": {"message": "google failed"}}
                    """);
        });

        assertThatThrownBy(() -> client.getPlaceDetail("place-1"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_FAILED));

        assertThat(capturedRequest.method()).isEqualTo("GET");
        assertThat(capturedRequest.path()).isEqualTo("/v1/places/place-1");
        assertThat(capturedRequest.apiKey()).isEqualTo("test-api-key");
        assertThat(capturedRequest.fieldMask())
                .contains("websiteUri")
                .contains("nationalPhoneNumber")
                .contains("photos");
    }

    @Test
    void getPhotoMediaSendsSizeAndNoRedirectQueryParams() {
        server.createContext("/v1/places/place-1/photos/photo-1/media", exchange -> {
            capturedRequest = CapturedRequest.from(exchange);
            respondJson(exchange, 200, """
                    {"photoUri": "https://example.com/photo.jpg"}
                    """);
        });

        GooglePhotoMedia result = client.getPhotoMedia("place-1", "photo-1", 600, 400);

        assertThat(result.photoUri()).isEqualTo("https://example.com/photo.jpg");
        assertThat(capturedRequest.method()).isEqualTo("GET");
        assertThat(capturedRequest.path()).isEqualTo("/v1/places/place-1/photos/photo-1/media");
        assertThat(capturedRequest.query())
                .contains("maxWidthPx=600")
                .contains("maxHeightPx=400")
                .contains("skipHttpRedirect=true");
        assertThat(capturedRequest.apiKey()).isEqualTo("test-api-key");
    }

    @Test
    void throwsInternalServerErrorWhenApiKeyIsBlank() {
        ReflectionTestUtils.setField(client, "apiKey", " ");

        assertThatThrownBy(() -> client.searchText(criteria(), "restaurant", "맛집"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private RecommendationCriteria criteria() {
        return new RecommendationCriteria(
                OriginType.MANUAL_TEXT,
                35.6938,
                139.7034,
                "도쿄 신주쿠",
                "라멘",
                null,
                3000,
                TransportMode.WALK,
                1200,
                20,
                OffsetDateTime.parse("2026-06-10T19:00:00+09:00")
        );
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record CapturedRequest(
            String method,
            String path,
            String query,
            String apiKey,
            String fieldMask,
            String body
    ) {
        private static CapturedRequest from(HttpExchange exchange) throws IOException {
            return new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getQuery(),
                    exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"),
                    exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
            );
        }
    }
}
