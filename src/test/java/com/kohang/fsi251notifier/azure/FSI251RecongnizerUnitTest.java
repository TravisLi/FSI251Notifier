package com.kohang.fsi251notifier.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest(classes = {FSI251RecongnizerUnitTest.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class FSI251RecongnizerUnitTest {

	@Mock
	private FileAccesser accesser;

	@Mock
	private FSI251Repository repository;

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${azure_key}")
	private String azureKey;
	@Value("${azure_endpoint}")
	private String azureEndPoint;

	private List<String> fileList = List.of(TestUtil.SAMPLE_FILE, TestUtil.SAMPLE_FILE_1, TestUtil.SAMPLE_FILE_2);
	
	@BeforeEach
	public void init() {
		
		try {

			ByteArrayOutputStream sampleFileBaos = new ByteArrayOutputStream();
			File sampleFile = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getFile();
			sampleFileBaos.writeBytes(Files.readAllBytes(sampleFile.toPath()));

			ByteArrayOutputStream sampleFile1Baos = new ByteArrayOutputStream();
			File sampleFile1 = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_1).getFile();
			sampleFile1Baos.writeBytes(Files.readAllBytes(sampleFile1.toPath()));

			ByteArrayOutputStream sampleFile2Baos = new ByteArrayOutputStream();
			File sampleFile2 = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_2).getFile();
			sampleFile2Baos.writeBytes(Files.readAllBytes(sampleFile2.toPath()));

			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE)).thenReturn(sampleFileBaos);
			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_1)).thenReturn(sampleFile1Baos);
			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_2)).thenReturn(sampleFile2Baos);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	@DisplayName("Repository contain certs")
	@Order(1)
	public void testRunForRepositoryContainsTheCerts() {
		
		when(accesser.getSrcFiles()).thenReturn(fileList);
		when(repository.findByCertNo(TestUtil.SAMPLE_CERT_NO)).thenReturn(TestUtil.getFSI251Data(0));
		when(repository.findByCertNo(TestUtil.SAMPLE_CERT_NO_1)).thenReturn(TestUtil.getFSI251Data(1));
		when(repository.findByCertNo(TestUtil.SAMPLE_CERT_NO_2)).thenReturn(TestUtil.getFSI251Data(2));
		
		//when
		FSI251Recognizer fsi251Recognizer = new FSI251Recognizer(azureKey,azureEndPoint, accesser, repository);
				
		List<FSI251Data> dataList = fsi251Recognizer.run();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> ac = ArgumentCaptor.forClass(List.class);
		
		//then
		verify(repository, times(0)).save(any());
		
		verify(accesser).copyAndDeleteFiles(ac.capture());
		
		assertTrue(dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO)).toList().size()==1);
		assertTrue(dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_1)).toList().size()==1);
		assertTrue(dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_2)).toList().size()==1);
		
		assertEquals(ac.getValue(), fileList);
	}
	
	@Test
	@DisplayName("Repository does not contain certs")
	@Order(2)
	public void testRunForRepositoryDoesNotContainTheCerts() {

		when(accesser.getSrcFiles()).thenReturn(fileList);
		when(repository.findByCertNo(TestUtil.SAMPLE_CERT_NO)).thenReturn(null);
		when(repository.findByCertNo(TestUtil.SAMPLE_CERT_NO_1)).thenReturn(null);
		when(repository.findByCertNo(TestUtil.SAMPLE_CERT_NO_2)).thenReturn(null);
		
		//when
		FSI251Recognizer fsi251Recognizer = new FSI251Recognizer(azureKey,azureEndPoint, accesser, repository);
						
		List<FSI251Data> dataList = fsi251Recognizer.run();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> ac = ArgumentCaptor.forClass(List.class);
		
		//then
		verify(repository, times(3)).save(any());
		
		verify(accesser).copyAndDeleteFiles(ac.capture());
		
		assertTrue(dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO)).toList().size()==1);
		assertTrue(dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_1)).toList().size()==1);
		assertTrue(dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_2)).toList().size()==1);
		
		assertEquals(ac.getValue(), fileList);
	}
	
	

}
