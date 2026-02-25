package com.kohang.fsi251notifier.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.TestUtil;

@SpringBootTest(classes = {FSI251RecongnizerUnitTest.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class FSI251RecongnizerUnitTest {

	@Mock
	private AzureFileAccesser accesser;

	@Mock
	private FSI251Repository fsi251Repo;

	@Mock
	private ExceptionRepository exceptionRepo;

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${azure.recognition.key}")
	private String azureKey;
	@Value("${azure.recognition.endpoint}")
	private String azureEndPoint;

	private final List<String> fileList = List.of(TestUtil.SAMPLE_FILE, TestUtil.SAMPLE_FILE_1, TestUtil.SAMPLE_FILE_2, TestUtil.SAMPLE_FILE_EXCEPTION);
	
    @BeforeEach
    void init() throws Exception {

        ByteArrayOutputStream sampleFileBaos = new ByteArrayOutputStream();
        try (InputStream is = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE).getInputStream()) {
            sampleFileBaos.writeBytes(is.readAllBytes());
        }

        ByteArrayOutputStream sampleFile1Baos = new ByteArrayOutputStream();
        try (InputStream is = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_1).getInputStream()) {
            sampleFile1Baos.writeBytes(is.readAllBytes());
        }

        ByteArrayOutputStream sampleFile2Baos = new ByteArrayOutputStream();
        try (InputStream is = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_2).getInputStream()) {
            sampleFile2Baos.writeBytes(is.readAllBytes());
        }

        ByteArrayOutputStream sampleFileExpBaos = new ByteArrayOutputStream();
        try (InputStream is = resourceLoader.getResource("classpath:" + TestUtil.SAMPLE_FILE_EXCEPTION).getInputStream()) {
            sampleFileExpBaos.writeBytes(is.readAllBytes());
        }

        when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE)).thenReturn(sampleFileBaos);
        when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_1)).thenReturn(sampleFile1Baos);
        when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_2)).thenReturn(sampleFile2Baos);
        when(accesser.getSrcFileByteArrayOutputStream(TestUtil.SAMPLE_FILE_EXCEPTION)).thenReturn(sampleFileExpBaos);

        Thread.sleep(20000);

    }

	@Test
	@DisplayName("Repository contain certs")
	@Order(1)
	void testRunForRepositoryContainsTheCerts() {
		
		when(accesser.getSrcFiles()).thenReturn(fileList);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO)).thenReturn(TestUtil.getFSI251Data(0));
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_2)).thenReturn(TestUtil.getFSI251Data(2));
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_EXCEPTION)).thenReturn(TestUtil.getFSI251Data(3));

		//when
		FSI251Recognizer fsi251Recognizer = new FSI251Recognizer(azureKey, azureEndPoint, accesser, fsi251Repo, exceptionRepo);
				
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
	void testRunForRepositoryDoesNotContainTheCerts() {

		when(accesser.getSrcFiles()).thenReturn(fileList);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO)).thenReturn(null);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_2)).thenReturn(null);
		when(fsi251Repo.findByCertNo(TestUtil.SAMPLE_CERT_NO_EXCEPTION)).thenReturn(null);
		//when
		FSI251Recognizer fsi251Recognizer = new FSI251Recognizer(azureKey,azureEndPoint, accesser, fsi251Repo, exceptionRepo);
						
		List<FSI251Data> dataList = fsi251Recognizer.run();

		//then
		verify(fsi251Repo, times(3)).save(any());
		verify(exceptionRepo, times(1)).save(any());

		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(0).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(1).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(2).getFileName());
		verify(accesser, times(1)).copyAndDeleteFile(TestUtil.getFSI251Data(3).getFileName());

		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO)).toList().size());
		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_2)).toList().size());
		assertEquals(1, dataList.stream().filter(data -> data.getCertNo().equals(TestUtil.SAMPLE_CERT_NO_EXCEPTION)).toList().size());

	}

}
