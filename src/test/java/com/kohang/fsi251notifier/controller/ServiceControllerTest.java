package com.kohang.fsi251notifier.controller;

import com.kohang.fsi251notifier.azure.CloudFileCopier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ServiceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser("admin")
	public void testImport() throws Exception {
		
		this.mockMvc.perform(get("/start/import")).andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("File import started")));

	}

	@Test
	@WithMockUser("admin")
	public void testEmailSend() throws Exception {

		this.mockMvc.perform(get("/start/email")).andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Email send started")));

	}

	@Test
	@WithMockUser("admin")
	public void testRecognize() throws Exception {

		this.mockMvc.perform(get("/start/recognition")).andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Recognition started")));

	}
	
	@Test
	public void testAuthFail() throws Exception {

		this.mockMvc.perform(get("/start/import")).andDo(print())
				.andExpectAll(status().is3xxRedirection(),redirectedUrlPattern("http://*/login"));

		this.mockMvc.perform(get("/start/email")).andDo(print())
				.andExpectAll(status().is3xxRedirection(),redirectedUrlPattern("http://*/login"));

		this.mockMvc.perform(get("/start/recognition")).andDo(print())
				.andExpectAll(status().is3xxRedirection(),redirectedUrlPattern("http://*/login"));
		
	}
	
}

