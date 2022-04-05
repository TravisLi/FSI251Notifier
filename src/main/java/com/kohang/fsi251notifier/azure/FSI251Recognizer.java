package com.kohang.fsi251notifier.azure;

import com.azure.ai.formrecognizer.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.models.AnalyzedDocument;
import com.azure.core.credential.AzureKeyCredential;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.Util;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
    public FSI251Recognizer(@Value("#{systemProperties['azure.recognition.key']!=null && systemProperties['azure.recognition.key']!='' ? systemProperties['azure.recognition.key'] : systemEnvironment['azure_recognition_key']}") String key,
                            @Value("#{systemProperties['azure.recognition.endpoint']!=null && systemProperties['azure.recognition.endpoint']!='' ? systemProperties['azure.recognition.endpoint'] : systemEnvironment['azure_recognition_endpoint']}") String endpoint,
                            AzureFileAccesser fa,
                            FSI251Repository r) {
        this.client = new DocumentAnalysisClientBuilder()
                .credential(new AzureKeyCredential(key.strip()))
                .endpoint(endpoint.strip())
                .buildClient();
        this.accesser = fa;
        this.repository = r;
    }

    //save cert file to DB, run once a day
    //TODO: create an exception list for those cert cannot be inserted to DB, in addition with UI to rescue the file
    @Transactional
    public List<FSI251Data> run() {
        logger.info("run start");

        List<FSI251Data> resultList = new ArrayList<>();
        List<String> srcFiles = accesser.getSrcFiles();

        for (String fileName : srcFiles) {

            logger.info("Working on file: " + fileName);

            ByteArrayOutputStream os = accesser.getSrcFileByteArrayOutputStream(fileName);

            InputStream is = new ByteArrayInputStream(os.toByteArray());

            PDDocument main = null;

            try {
                main = PDDocument.load(is);

				logger.info("Splitting the Pdf");

                // Splitting the pages into multiple PDFs
                List<PDDocument> pages = new Splitter().split(main);

                int pageNo = 1;

                for (PDDocument sub : pages) {

                    ByteArrayOutputStream subOs = new ByteArrayOutputStream();

                    try {

                        sub.save(subOs);

						String subFileName = fileName;

						if(pages.size()>1){
							subFileName = fileName.replace(Util.PDF_EXTENSION, "-" + pageNo++ + Util.PDF_EXTENSION);
							logger.info("Rename each page to " + subFileName);
						}

                        InputStream subIs = new ByteArrayInputStream(subOs.toByteArray());

                        FSI251Data data = this.analyzeDocument(subFileName,subIs,subOs.size());

						//if the document does not contain cert no and cert date, will skip the check
						if (data.getCertNo() != null && data.getCertDate() != null) {

							try {
								//added for date check, to make sure mongo qurey work
								Util.convertDateStrToLocalDate(data.getCertDate());

								FSI251Data dataInDb = repository.findByCertNo(data.getCertNo());

								if (dataInDb == null) {
									logger.info("Save the record to DB");
									repository.save(data);
                                    //reset it for file accesser to consume
                                    subIs.reset();
                                    accesser.uploadToProcessedFolder(subFileName,subOs.size(),subIs);
									resultList.add(data);
								} else {
									logger.warn(String.format("Cert No %s: already exist", data.getCertNo()));
								}

							} catch (Exception e) {
								logger.error(data.getCertDate() + " cannot format into dd/mm/yyyy format");
								e.printStackTrace();
							}

						}

                    } catch (IOException e) {
                        logger.error("Pdf pages splitting error");
                        e.printStackTrace();
                    }finally {
                        if(sub!=null) {
                            sub.close();
                        }
                    }

                }


            } catch (IOException e) {
                logger.error("PDF loading error");
                e.printStackTrace();
            }finally{
                if(main!=null){
                    try {
                        main.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //do it one by one rather than in whole list
            accesser.copyAndDeleteFile(fileName);

        }

        return resultList;
    }

    private FSI251Data analyzeDocument(String fileName, InputStream is, long maxSize) {

        logger.info("Recognizing in Azure");

        FSI251Data data = new FSI251Data();
        data.setFileName(fileName);

        try {

            client.beginAnalyzeDocument(MODEL_ID, is, maxSize)
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

        } catch (Exception e) {
            logger.error("Document recognition error");
            e.printStackTrace();
        }

        logger.info(data.toString());

        return data;

    }


}
