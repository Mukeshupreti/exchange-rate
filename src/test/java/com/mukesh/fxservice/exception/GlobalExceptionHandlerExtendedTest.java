package com.mukesh.fxservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandlerExtendedTest.TestController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerExtendedTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/validate").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runtimeException_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/throw/runtime").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @RestController
    static class TestController {

        @GetMapping("/validate")
        public void validate(@RequestParam("param") String param) {
            // just a simple endpoint that requires a param
        }

        @GetMapping("/throw/runtime")
        public void runtime() {
            throw new RuntimeException("boom");
        }
    }
}

