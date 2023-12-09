package com.kohang.fsi251notifier.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("exception")
@Data
@NoArgsConstructor
public class ExceptionData {

    @Id
    private String id;
    private FSI251Data fsi251Data;
    private String remark;
    private Boolean resolved;

    public ExceptionData(FSI251Data fsi251Data, String remark) {
        this.fsi251Data = fsi251Data;
        this.remark = remark;
        this.resolved = false;
    }

    public ExceptionData(FSI251Data fsi251Data, String remark, Boolean resolved) {
        this.fsi251Data = fsi251Data;
        this.remark = remark;
        this.resolved = resolved;
    }

}
