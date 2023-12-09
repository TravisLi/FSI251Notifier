package com.kohang.fsi251notifier.azure;


import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzedDocument;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.kohang.fsi251notifier.model.ExceptionData;
import com.kohang.fsi251notifier.model.FSI251Data;
import com.kohang.fsi251notifier.repository.ExceptionRepository;
import com.kohang.fsi251notifier.repository.FSI251Repository;
import com.kohang.fsi251notifier.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class FSI251Recognizer {

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
        log.info("run start");

        List<FSI251Data> resultList = new LinkedList<>();
        List<String> srcFiles = accesser.getSrcFiles();

        for (String fileName : srcFiles) {

            List<FSI251Data> mainPageResultList = processFile(fileName);
            resultList.addAll(mainPageResultList);

        }

        return resultList;
    }

    private List<FSI251Data> processFile(String fileName) {
        log.info("Working on file: " + fileName);

        List<FSI251Data> resultList = new LinkedList<>();

        try (ByteArrayOutputStream os = accesser.getSrcFileByteArrayOutputStream(fileName)) {

            List<FSI251Data> mainPageResultList = processMainPdf(fileName, os);
            resultList.addAll(mainPageResultList);

            //do it one by one rather than in whole list
            accesser.copyAndDeleteFile(fileName);

        } catch (IOException e) {
            log.error("Error occurs while working with Byte Array Output Stream", e);
        }

        return resultList;
    }

    private List<FSI251Data> processMainPdf(String fileName, ByteArrayOutputStream os) {

        List<FSI251Data> resultList = new LinkedList<>();

        try (InputStream is = new ByteArrayInputStream(os.toByteArray())) {

            try(PDDocument main = PDDocument.load(is)){
                log.info("Splitting the Pdf");

                // Splitting the pages into multiple PDFs
                List<PDDocument> pages = new Splitter().split(main);
                List<FSI251Data> subPageResultList = processPdfSubPages(fileName, pages);
                resultList.addAll(subPageResultList);

            } catch (IOException e) {
                log.error("PDF loading error", e);
            }

        } catch (IOException e) {
            log.error("Input stream loading error", e);
        }
        return resultList;
    }

    private List<FSI251Data> processPdfSubPages(String fileName, List<PDDocument> pages) throws IOException {
        log.info("Processing Pdf sub pages");

        List<FSI251Data> resultList = new LinkedList<>();
        int pageNo = 1;

        for (PDDocument sub : pages) {

            try (sub) {

                try (ByteArrayOutputStream subOs = new ByteArrayOutputStream()) {

                    sub.save(subOs);

                    String subFileName = fileName;

                    if (pages.size() > 1) {
                        subFileName = fileName.replace(Util.PDF_EXTENSION, "-" + pageNo++ + Util.PDF_EXTENSION);
                        log.info("Rename each page to {}", subFileName);
                    }

                    Optional<FSI251Data> fsi251Data = processPdfSubPage(subOs, subFileName);
                    fsi251Data.ifPresent(resultList::add);

                } catch (IOException e) {
                    log.error("Pdf pages splitting error", e);
                }

            }

        }

        return resultList;
    }

    private Optional<FSI251Data> processPdfSubPage(ByteArrayOutputStream subOs, String subFileName) {
        try (InputStream subIs = new ByteArrayInputStream(subOs.toByteArray())) {
            FSI251Data data = this.analyzeDocument(subFileName, BinaryData.fromBytes(subOs.toByteArray()));

            //if the document does not contain cert no and cert date, will skip the check
            if (data.getCertNo() != null && data.getCertDate() != null) {

                FSI251Data dataInDb = fsi251Repo.findByCertNo(data.getCertNo());

                if (dataInDb == null) {
                    log.info("Save the record to DB");
                    dataDateValidation(data);
                    fsi251Repo.save(data);
                    //reset it for file accesser to consume
                    subIs.reset();
                    accesser.uploadToProcessedFolder(subFileName, subOs.size(), subIs);
                    return Optional.of(data);
                } else {
                    log.warn(String.format("Cert No %s: already exist", data.getCertNo()));
                }
            }

            //stop for 2 seconds between each page
            //prevent from over the threshold of document recognizer free tier limit
            Thread.sleep(2000);
        } catch (IOException e) {
            log.error("Error occurs while working with Byte Array Input Stream", e);
        } catch (InterruptedException e) {
            log.error("Sleeping of thread is interrupted", e);
        }
        return Optional.empty();
    }

    private void dataDateValidation(FSI251Data data) {
        try {
            //added for date check, to make sure mongo query work
            Util.convertDateStrToLocalDate(data.getCertDate());
        } catch (Exception e) {
            String errorMsg = data.getCertDate() + " cannot format into dd/mm/yyyy format";
            log.error(errorMsg,e);
            exceptionRepo.save(new ExceptionData(data,errorMsg));
            //remove the date to prevent fail during notification email send
            data.setCertDate("");
        }
    }

    private FSI251Data analyzeDocument(String fileName, BinaryData bd) {

        log.info("Recognizing in Azure");

        FSI251Data data = new FSI251Data();
        data.setFileName(fileName);

        try {

            client.beginAnalyzeDocument(MODEL_ID, bd)
                    .getFinalResult()
                    .getDocuments().stream()
                    .map(AnalyzedDocument::getFields)
                    .forEach(documentFieldMap -> documentFieldMap.forEach((key, documentField) -> {

                        switch (key) {
                            case BUILDING_KEY -> data.setBuildingName(documentField.getContent());
                            case CLIENT_KEY -> data.setClientName(documentField.getContent());
                            case CERT_NO_KEY -> data.setCertNo(documentField.getContent());
                            case CERT_DATE_KEY -> data.setCertDate(documentField.getContent());
                            default -> log.warn("No such field {} exists", key);
                        }
                    }));

        } catch (Exception e) {
            log.error("Document recognition error", e);
        }

        log.info(data.toString());

        return data;

    }

}
