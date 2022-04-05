package com.kohang.fsi251notifier.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser("admin")
    void testImport() throws Exception {

        this.mockMvc.perform(get("/start/import")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("File import started")));

    }

    @Test
    @WithMockUser("admin")
    void testEmailSend() throws Exception {

        this.mockMvc.perform(get("/start/email")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email send started")));

    }

    @Test
    @WithMockUser("admin")
    void testRecognize() throws Exception {

        this.mockMvc.perform(get("/start/recognize")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recognition started")));

    }

    @Test
    void testAuthFail() throws Exception {

        this.mockMvc.perform(get("/start/import")).andDo(print())
                .andExpectAll(status().is3xxRedirection(), redirectedUrlPattern("http://*/login"));

        this.mockMvc.perform(get("/start/email")).andDo(print())
                .andExpectAll(status().is3xxRedirection(), redirectedUrlPattern("http://*/login"));

        this.mockMvc.perform(get("/start/recognize")).andDo(print())
                .andExpectAll(status().is3xxRedirection(), redirectedUrlPattern("http://*/login"));

    }

}

