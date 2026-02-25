package com.kohang.fsi251notifier.controller;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest
@AutoConfigureMockMvc
class FSI251ControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	@Test
	@WithMockUser("admin")
	void testGetFullList() throws Exception {
		
		this.mockMvc.perform(get("/fsi251")).andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("admin")))
		.andExpect(content().string(containsString(TestUtil.SAMPLE_CERT_NO)));
		
	}
	
	@Test
	void testAuthFail() throws Exception {
		
		this.mockMvc.perform(get("/fsi251")).andDo(print())
		.andExpectAll(status().is3xxRedirection(),redirectedUrlPattern("http://*/login"));
		
	}
	
}