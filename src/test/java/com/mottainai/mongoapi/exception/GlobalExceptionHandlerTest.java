package com.mottainai.mongoapi.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private WebRequest webRequest;

    @Test
    void handleDuplicateKey_mapsToConflict() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/rag/documents");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleDuplicateKey(new DuplicateKeyException("E11000 duplicate key"), webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().path()).isEqualTo("/api/v1/rag/documents");
    }
}
