package com.kohang.fsi251notifier.azure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.kohang.fsi251notifier.repository.ExceptionRepository;
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
	private AzureFileAccesser accesser;

	@Mock
	private FSI251Repository fsi251Repo;

	@Mock
	private ExceptionRepository exceptionRepo;

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${azure_recognition_key}")
	private String azureKey;
	@Value("${azure_recognition_endpoint}")
	private String azureEndPoint;

	private final List<String> fileList = List.of(TestUtil.SAMPLE_FILE, TestUtil.SAMPLE_FILE_1, TestUtil.SAMPLE_FILE_2, TestUtil.SAMPLE_FILE_EXCEPTION);
	
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

			ByteArrayOutputStream sampleFileExpBaos = new ByteArrayOutputStream();
			File sampleFileExp = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_EXCEPTION).getFile();
			sampleFileExpBaos.writeBytes(Files.readAllBytes(sampleFileExp.toPath()));

			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE)).thenReturn(sampleFileBaos);
			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_1)).thenReturn(sampleFile1Baos);
			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_2)).thenReturn(sampleFile2Baos);
			when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_EXCEPTION)).thenReturn(sampleFileExpBaos);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	@DisplayName("Repository contain certs")
	@Order(1)
	public void testRunForRepositoryContainsTheCerts() {
		
		when(accesser.getSrcFiles()).thenReturn(fileList);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO)).thenReturn(TestUtil.getFSI251Data(0));
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_1)).thenReturn(TestUtil.getFSI251Data(1));
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_2)).thenReturn(TestUtil.getFSI251Data(2));
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_EXCEPTION)).thenReturn(TestUtil.getFSI251Data(3));

		//when
		FSI251Recognizer fsi251Recognizer = new FSI251Recognizer(azureKey,azureEndPoint, accesser, fsi251Repo, exceptionRepo);
				
		List<FSI251Data> dataList = fsi251Recognizer.run();

		//then
		verify(fsi251Repo, times(0)).save(any());
		
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(0).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(1).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(2).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(3).getFileName());

		assertEquals(0, dataList.size());

	}
	
	@Test
	@DisplayName("Repository does not contain certs")
	@Order(2)
	public void testRunForRepositoryDoesNotContainTheCerts() {

		when(accesser.getSrcFiles()).thenReturn(fileList);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO)).thenReturn(null);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_1)).thenReturn(null);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_2)).thenReturn(null);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_EXCEPTION)).thenReturn(null);
		//when
		FSI251Recognizer fsi251Recognizer = new FSI251Recognizer(azureKey,azureEndPoint, accesser, fsi251Repo, exceptionRepo);
						
		List<FSI251Data> dataList = fsi251Recognizer.run();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> ac = ArgumentCaptor.forClass(List.class);
		
		//then
		verify(fsi251Repo, times(4)).save(any());
		verify(exceptionRepo, times(1)).save(any());

		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(0).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(1).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(2).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(3).getFileName());

		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO)).toList().size());
		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_1)).toList().size());
		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_2)).toList().size());
		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_EXCEPTION)).toList().size());

	}
	
	

}
