package com.kohang.fsi251notifier.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("fsi251")
public class FSI251Data {
	
	@Id
	private String Id;
	private String buildingName;
	private String clientName;
	private String certNo;
	private String certDate;
	private String fileName;
	
	public FSI251Data() {
		
	}
	
	public FSI251Data(String certNo, String certDate, String fileName) {
		this.certNo = certNo;
		this.certDate = certDate;
		this.fileName = fileName;
	}
	
	public String getBuildingName() {
		return buildingName;
	}
	public void setBuildingName(String buildingName) {
		this.buildingName = buildingName;
	}
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public String getCertNo() {
		return certNo;
	}
	public void setCertNo(String certNo) {
		this.certNo = certNo;
	}
	public String getCertDate() {
		return certDate;
	}
	public void setCertDate(String certDate) {
		this.certDate = certDate;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getId() {
		return Id;
	}
	public void setId(String id) {
		Id = id;
	}
	@Override
	public String toString() {
		return "FSI251Data [buildingName=" + buildingName + ", clientName=" + clientName + ", certNo=" + certNo
				+ ", certDate=" + certDate + ", fileName=" + fileName + "]";
	}
	
}
