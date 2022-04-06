package com.kohang.fsi251notifier.azure;

import com.azure.ai.formrecognizer.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.models.AnalyzedDocument;
import com.azure.core.credential.AzureKeyCredential;
import com.kohang.fsi251notifier.model.ExceptionData;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final FSI251Repository fsi251Repo;
    private final ExceptionRepository exceptionRepo;
    private final AzureFileAccesser accesser;

    @Autowired
    public FSI251Recognizer(@Value("#{systemProperties['azure.recognition.key']!=null && systemProperties['azure.recognition.key']!='' ? systemProperties['azure.recognition.key'] : systemEnvironment['azure_recognition_key']}") String key,
                            @Value("#{systemProperties['azure.recognition.endpoint']!=null && systemProperties['azure.recognition.endpoint']!='' ? systemProperties['azure.recognition.endpoint'] : systemEnvironment['azure_recognition_endpoint']}") String endpoint,
                            AzureFileAccesser fa,
                            FSI251Repository f,
                            ExceptionRepository e) {
        this.client = new DocumentAnalysisClientBuilder()
                .credential(new AzureKeyCredential(key.strip()))
                .endpoint(endpoint.strip())
                .buildClient();
        this.accesser = fa;
        this.fsi251Repo = f;
        this.exceptionRepo = e;
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

                        if (pages.size() > 1) {
                            subFileName = fileName.replace(Util.PDF_EXTENSION, "-" + pageNo++ + Util.PDF_EXTENSION);
                            logger.info("Rename each page to " + subFileName);
                        }

                        InputStream subIs = new ByteArrayInputStream(subOs.toByteArray());

                        FSI251Data data = this.analyzeDocument(subFileName, subIs, subOs.size());

                        //if the document does not contain cert no and cert date, will skip the check
                        if (data.getCertNo() != null && data.getCertDate() != null) {

                            FSI251Data dataInDb = fsi251Repo.findByCertNo(data.getCertNo());

                            if (dataInDb == null) {
                                logger.info("Save the record to DB");
                                try {
                                    //added for date check, to make sure mongo query work
                                    Util.convertDateStrToLocalDate(data.getCertDate());
                                } catch (Exception e) {
                                    String errorMsg = data.getCertDate() + " cannot format into dd/mm/yyyy format";
                                    logger.error(errorMsg);
                                    e.printStackTrace();
                                    ExceptionData exception = new ExceptionData(data,errorMsg);
                                    exceptionRepo.save(exception);
                                    //remove the date to prevent fail during notification email send
                                    data.setCertDate("");
                                }
                                fsi251Repo.save(data);
                                //reset it for file accesser to consume
                                subIs.reset();
                                accesser.uploadToProcessedFolder(subFileName, subOs.size(), subIs);
                                resultList.add(data);
                            } else {
                                logger.warn(String.format("Cert No %s: already exist", data.getCertNo()));
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Pdf pages splitting error");
                        e.printStackTrace();
                    } finally {
                        if (sub != null) {
                            sub.close();
                        }
                    }

                }


            } catch (IOException e) {
                logger.error("PDF loading error");
                e.printStackTrace();
            } finally {
                if (main != null) {
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
                    .forEach(documentFieldMap -> documentFieldMap.forEach((key, documentField) -> {

                        switch (key) {
                            case BUILDING_KEY -> data.setBuildingName(documentField.getContent());
                            case CLIENT_KEY -> data.setClientName(documentField.getContent());
                            case CERT_NO_KEY -> data.setCertNo(documentField.getContent());
                            case CERT_DATE_KEY -> data.setCertDate(documentField.getContent());
                        }
                    }));

        } catch (Exception e) {
            logger.error("Document recognition error");
            e.printStackTrace();
        }

        logger.info(data.toString());

        return data;

    }


}
