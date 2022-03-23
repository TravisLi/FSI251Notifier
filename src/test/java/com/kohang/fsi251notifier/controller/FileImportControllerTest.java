package com.kohang.fsi251notifier.controller;

import com.kohang.fsi251notifier.azure.AzureFileAccesser;
import com.kohang.fsi251notifier.azure.CloudFileCopier;
import com.kohang.fsi251notifier.azure.OneDriveFileAccesser;
import com.kohang.fsi251notifier.util.TestUtil;
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
public class FileImportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@SpyBean
	private CloudFileCopier copier;

	@Test
	@WithMockUser("admin")
	public void testImport() throws Exception {
		
		this.mockMvc.perform(get("/import")).andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Import job has been submitted, please go to fsi 251 page to check later")));

		verify(copier, times(1)).copyAllOneDriveCertsToAzureSrcDrive();

	}
	
	@Test
	public void testAuthFail() throws Exception {
		
		this.mockMvc.perform(get("/import")).andDo(print())
		.andExpectAll(status().is3xxRedirection(),redirectedUrlPattern("http://*/login"));
		
	}
	
}

