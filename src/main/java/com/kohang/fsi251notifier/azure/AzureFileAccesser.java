package com.kohang.fsi251notifier.azure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.azure.storage.blob.models.ParallelTransferOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.file.share.ShareClient;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;
import com.azure.storage.file.share.models.CopyStatusType;
import com.azure.storage.file.share.models.ShareFileCopyInfo;
import com.azure.storage.file.share.models.ShareFileUploadInfo;

@Component
public class AzureFileAccesser {

	private static final Logger logger = LoggerFactory.getLogger(AzureFileAccesser.class);
	
	private static final String SHARE_FILE_NAME = "fsi251file";
	private static final String SOURCE_DIR = "source";
	private static final String PROCESSED_DIR = "processed";
	private static final long MAX_SIZE = 10240;
	
	private final ShareDirectoryClient sourceDirClient;
	private final ShareDirectoryClient processDirClient;
	
	public AzureFileAccesser(@Value("${azure_storage}")String storageConnectionStr) {
		
		ShareClient shareClient = new ShareServiceClientBuilder().connectionString(storageConnectionStr).buildClient().getShareClient(SHARE_FILE_NAME);	
		this.sourceDirClient = shareClient.getDirectoryClient(SOURCE_DIR);
		
		//create the folder if the folder does not exist
		if(!this.sourceDirClient.exists()) {
			logger.info("Source directory is missing, create a new one");
			this.sourceDirClient.create();
		}
		
		this.processDirClient = shareClient.getDirectoryClient(PROCESSED_DIR);
		
		//create the folder if the folder does not exist
		if(!this.processDirClient.exists()) {
			logger.info("Processed directory is missing, create a new one");
			this.processDirClient.create();
		}
		
	}
		
	public List<String> getSrcFiles(){

		logger.info("Getting all files is src folder");

		List<String> resultList = new ArrayList<>();
				
		sourceDirClient.listFilesAndDirectories().forEach(item->{
			
			if(item.getName().contains(".pdf")) {
				
				ShareFileClient c = sourceDirClient.getFileClient(item.getName());
				logger.info(c.getFileUrl());
				resultList.add(item.getName());
				
			}
						
		});
		
		return resultList;
		
	}
	
	public String getProcessedFileUrl(String fileName){
		
		ShareFileClient pc = processDirClient.getFileClient(fileName);
		return pc.getFileUrl();
		
	}

	public void deleteAllFilesInSrcFolder(){

		getSrcFiles().forEach(name->{

			ShareFileClient sc = sourceDirClient.getFileClient(name);
			sc.delete();

		});
	}
	
	public void copyAndDeleteFiles(List<String> nameList) {
		
		nameList.forEach(name->{
			
			ShareFileClient sc = sourceDirClient.getFileClient(name);
			ShareFileClient pc = processDirClient.createFile(name, MAX_SIZE);
			
			SyncPoller<ShareFileCopyInfo, Void> poller = pc.beginCopy(sc.getFileUrl(), null, Duration.ofSeconds(10));

			final PollResponse<ShareFileCopyInfo> pollResponse = poller.poll();
			final ShareFileCopyInfo value = pollResponse.getValue();
			
			if(value.getCopyStatus().equals(CopyStatusType.SUCCESS)) {
				logger.info(sc.getFileUrl() + " copy completed. Delete will be executed");
				sc.delete();
			}
						
			
		});
		
	}
		
	public ByteArrayOutputStream getSrcFileByteArrayOutputStream(String name) {
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		sourceDirClient.getFileClient(name).download(stream);
		
		return stream;
		
	}
	
	public ByteArrayOutputStream getProcessedFileByteArrayOutputStream(String name) {
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		processDirClient.getFileClient(name).download(stream);
		
		return stream;
		
	}
	
	
	public String uploadToSrcFolder(File file) {
		
		if(file!=null&&file.length()>0) {
			
			ShareFileClient sc = sourceDirClient.createFile(file.getName(), file.length());
			
			InputStream uploadData;
			try {
				uploadData = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
				ShareFileUploadInfo response = sc.uploadRange(uploadData, file.length());
				return response.getETag();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		return "";
		
	}

	public List<String> uploadToSrcFolder(List<File> fileList){

		List<String> fileNameList = new LinkedList<>();
		fileList.forEach(f->{
			String result = uploadToSrcFolder(f);
			if(!result.isEmpty()){
				fileNameList.add(f.getName());
			}
			if(f.exists()){
				f.delete();
			}
		});

		return fileNameList;
	}
	
	public String uploadToProcessFolder(File file) {
		
		ShareFileClient pc = processDirClient.createFile(file.getName(), file.length());

		try {
			InputStream uploadData = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
			ShareFileUploadInfo response = pc.uploadRange(uploadData, file.length());
			return response.getETag();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
		
	}


	
}
