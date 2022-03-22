package com.kohang.fsi251notifier.azure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.azure.ai.formrecognizer.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.models.AnalyzedDocument;
import com.azure.core.credential.AzureKeyCredential;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;

@Component
public class FSI251Recognizer {

	private static final Logger logger = LoggerFactory.getLogger(FSI251Recognizer.class);
	private static final String MODEL_ID = "FSI251";
	private static final String BUILDING_KEY = "buildingName";
	private static final String CLIENT_KEY = "clientName";
	private static final String CERT_NO_KEY = "certNo";
	private static final String CERT_DATE_KEY = "certDate";

	private final DocumentAnalysisClient client;
	private final FSI251Repository repository;
	private final AzureFileAccesser accesser;

	@Autowired
	public FSI251Recognizer(@Value("${azure_recognition_key}")String key, @Value("${azure_recognition_endpoint}")String endpoint, AzureFileAccesser fa, FSI251Repository r) {
		this.client = new DocumentAnalysisClientBuilder()
				.credential(new AzureKeyCredential(key))
				.endpoint(endpoint)
				.buildClient();
		this.accesser = fa;
		this.repository = r;
	}

	//save cert file to DB, run once a day
	@Transactional
	public List<FSI251Data> run(){
		logger.info("run start");

		List<FSI251Data> resultList = new ArrayList<>();
		List<String> srcFiles = accesser.getSrcFiles();

		for(String fileName : srcFiles) {
			
			logger.info("Working on file: " + fileName);
			
			FSI251Data data = analyzeDocument(fileName);

			//if the document does not contain cert no and cert date, will skip the check
			if(data.getCertNo()!=null&&data.getCertDate()!=null) {

				FSI251Data dataInDb = repository.findByCertNo(data.getCertNo());

				if (dataInDb == null) {
					logger.info("Save the record to DB");
					repository.save(data);
				} else {
					logger.warn(String.format("Cert No %s: already exist", data.getCertNo()));
				}

				resultList.add(data);
			}

		}

		accesser.copyAndDeleteFiles(srcFiles);

		return resultList;
	}

	private FSI251Data analyzeDocument(String fileName) {

		logger.info("Recognizing in Azure");

		ByteArrayOutputStream os = accesser.getSrcFileByteArrayOutputStream(fileName);

		InputStream is = new ByteArrayInputStream(os.toByteArray());

		FSI251Data data = new FSI251Data();
		data.setFileName(fileName);

		client.beginAnalyzeDocument(MODEL_ID, is, os.size())
		.getFinalResult()
		.getDocuments().stream()
		.map(AnalyzedDocument::getFields)
		.forEach(documentFieldMap -> {

			documentFieldMap.forEach((key, documentField) -> {

				switch (key) {
					case BUILDING_KEY -> data.setBuildingName(documentField.getContent());
					case CLIENT_KEY -> data.setClientName(documentField.getContent());
					case CERT_NO_KEY -> data.setCertNo(documentField.getContent());
					case CERT_DATE_KEY -> data.setCertDate(documentField.getContent());
				}
			});

		});

		logger.info(data.toString());
		return data;

	}


}
