package com.mukesh.fxservice.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/throw/bulkhead")
        public void bulkhead() {
            BulkheadFullException ex = mock(BulkheadFullException.class);
            throw ex;
        }

        @GetMapping("/throw/circuit")
        public void circuit() {
            CallNotPermittedException ex = mock(CallNotPermittedException.class);
            throw ex;
        }
    }

    @Test
    void bulkheadMappedTo429() throws Exception {
        mockMvc.perform(get("/throw/bulkhead").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void circuitMappedTo503() throws Exception {
        mockMvc.perform(get("/throw/circuit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());
    }
}
