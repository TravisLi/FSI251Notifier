package com.kohang.fsi251notifier.azure;

import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;
import com.azure.storage.file.share.models.CopyStatusType;
import com.azure.storage.file.share.models.ShareFileCopyInfo;
import com.azure.storage.file.share.models.ShareFileUploadInfo;
import com.kohang.fsi251notifier.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AzureFileAccesser {

	private static final String SHARE_FILE_NAME = "fsi251file";
	private static final String SOURCE_DIR = "source";
	private static final String PROCESSED_DIR = "processed";
	private static final long MAX_SIZE = 10240;
	
	private final ShareDirectoryClient sourceDirClient;
	private final ShareDirectoryClient processedDirClient;

	public AzureFileAccesser(@Value("#{systemProperties['azure.storage']!=null && systemProperties['azure.storage']!=''? systemProperties['azure.storage'] : systemEnvironment['azure_storage']}")String storageConnectionStr) {

		ShareClient shareClient = new ShareServiceClientBuilder().connectionString(storageConnectionStr.strip()).buildClient().getShareClient(SHARE_FILE_NAME);
		this.sourceDirClient = shareClient.getDirectoryClient(SOURCE_DIR);
		
		//create the folder if the folder does not exist
		if(Boolean.FALSE.equals(this.sourceDirClient.exists())) {
			log.info("Source directory is missing, create a new one");
			this.sourceDirClient.create();
		}
		
		this.processedDirClient = shareClient.getDirectoryClient(PROCESSED_DIR);
		
		//create the folder if the folder does not exist
		if(Boolean.FALSE.equals(this.processedDirClient.exists())) {
			log.info("Processed directory is missing, create a new one");
			this.processedDirClient.create();
		}
		
	}
		
	public List<String> getSrcFiles(){

		log.info("Getting all files from src folder");

		List<String> resultList = new ArrayList<>();
				
		sourceDirClient.listFilesAndDirectories().forEach(item->{
			
			if(item.getName().contains(Util.PDF_EXTENSION)) {
				
				ShareFileClient c = sourceDirClient.getFileClient(item.getName());
				log.info(c.getFileUrl());
				resultList.add(item.getName());
				
			}
						
		});
		
		return resultList;
		
	}

	public List<String> getProcessedFiles(){

		log.info("Getting all files from processed folder");

		List<String> resultList = new ArrayList<>();

		processedDirClient.listFilesAndDirectories().forEach(item->{

			if(item.getName().contains(Util.PDF_EXTENSION)) {

				ShareFileClient c = processedDirClient.getFileClient(item.getName());
				log.info(c.getFileUrl());
				resultList.add(item.getName());

			}

		});

		return resultList;

	}

	public String getProcessedFileUrl(String fileName){
		
		ShareFileClient pc = processedDirClient.getFileClient(fileName);
		return pc.getFileUrl();
		
	}

	public void deleteAllFilesInSrcFolder(){

		getSrcFiles().forEach(name->{
			log.info("Deleting file:" + name);
			ShareFileClient sc = sourceDirClient.getFileClient(name);
			sc.delete();

		});
	}

	public void deleteAllFilesInProcessedFolder(){

		getProcessedFiles().forEach(name->{
			log.info("Deleting file:" + name);
			ShareFileClient sc = processedDirClient.getFileClient(name);
			sc.delete();

		});
	}
	
	public void copyAndDeleteFiles(List<String> nameList) {
		
		nameList.forEach(this::copyAndDeleteFile);
		
	}

	public void copyAndDeleteFile(String filename) {

		ShareFileClient sc = sourceDirClient.getFileClient(filename);
		ShareFileClient pc = processedDirClient.createFile(filename, MAX_SIZE);

		SyncPoller<ShareFileCopyInfo, Void> poller = pc.beginCopy(sc.getFileUrl(), null, Duration.ofSeconds(10));

		final PollResponse<ShareFileCopyInfo> pollResponse = poller.poll();
		final ShareFileCopyInfo value = pollResponse.getValue();

		if (value.getCopyStatus().equals(CopyStatusType.SUCCESS)) {
			log.info(sc.getFileUrl() + " copy completed. Delete will be executed");
			sc.delete();
		}

	}
		
	public ByteArrayOutputStream getSrcFileByteArrayOutputStream(String name) {

		return getFileByteArrayOutputStream(sourceDirClient, name);
		
	}
	
	public ByteArrayOutputStream getProcessedFileByteArrayOutputStream(String name) {

		return getFileByteArrayOutputStream(processedDirClient, name);
		
	}

	private ByteArrayOutputStream getFileByteArrayOutputStream(ShareDirectoryClient client, String name) {

		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		client.getFileClient(name).download(stream);

		return stream;

	}

	public void uploadToSrcFolder(String fileName, long maxSize, InputStream is) {

		uploadToFolder(this.sourceDirClient,fileName,maxSize,is);

	}

	public void uploadToProcessedFolder(String fileName, long maxSize, InputStream is) {

		uploadToFolder(this.processedDirClient,fileName,maxSize,is);

	}

	private void uploadToFolder(ShareDirectoryClient client, String fileName, long maxSize, InputStream is) {

		if(is!=null&&!fileName.isEmpty()&&maxSize>0) {
			ShareFileClient sc = client.createFile(fileName, maxSize);
			ShareFileUploadInfo response = sc.upload(is,maxSize,null);
			log.info("ETag of uploaded file: {}", response.getETag());
		}

	}

	public String uploadToSrcFolder(File file) {

		try {
			return uploadToFolder(this.sourceDirClient,file);
		} catch (IOException e) {
			log.error("Upload to Source folder error", e);
		}

		return "";

	}

	public String uploadToProcessedFolder(File file) {

		try {
			return uploadToFolder(this.processedDirClient,file);
		} catch (IOException e) {
			log.error("Upload to Processed folder error",e);
		}

		return "";

	}

	private String uploadToFolder(ShareDirectoryClient client, File file) throws IOException {

		if(file!=null&&file.length()>0) {
			ShareFileClient pc = client.createFile(file.getName(), file.length());
			InputStream uploadData = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
			ShareFileUploadInfo response = pc.upload(uploadData, file.length(),null);
			return response.getETag();
		}

		return "";
	}

}
