package com.kohang.fsi251notifier.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("fsi251")
@Data
@NoArgsConstructor
public class FSI251Data {
	
	@Id
	private String id;
	private String buildingName;
	private String clientName;
	private String certNo;
	private String certDate;
	private String fileName;

	public FSI251Data(String certNo, String certDate, String fileName) {
		this.certNo = certNo;
		this.certDate = certDate;
		this.fileName = fileName;
	}

}
