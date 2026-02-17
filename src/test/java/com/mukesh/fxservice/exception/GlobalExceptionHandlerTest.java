package com.mukesh.fxservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void restClientMappedToBadGateway() throws Exception {
        mockMvc.perform(get("/throw/rest-client").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway());
    }

    @Test
    void methodNotAllowedMappedTo405() throws Exception {
        mockMvc.perform(get("/throw/method-not-allowed").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @RestController
    static class TestController {
        @GetMapping("/throw/rest-client")
        public void restClient() {
            throw new RestClientException("downstream error");
        }

        @GetMapping("/throw/method-not-allowed")
        public void methodNotAllowed() throws org.springframework.web.HttpRequestMethodNotSupportedException {
            throw new org.springframework.web.HttpRequestMethodNotSupportedException("POST");
        }
    }
}
