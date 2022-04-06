package com.kohang.fsi251notifier.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("exception")
public class ExceptionData {

    @org.springframework.data.annotation.Id
    private String Id;
    private FSI251Data fsi251Data;
    private String remark;
    private Boolean resolved;

    public ExceptionData(){

    }

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

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public FSI251Data getFsi251Data() {
        return fsi251Data;
    }

    public void setFsi251Data(FSI251Data fsi251Data) {
        this.fsi251Data = fsi251Data;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public String toString() {
        return "ExceptionData{" +
                "Id='" + Id + '\'' +
                ", fsi251Data=" + fsi251Data +
                ", remark='" + remark + '\'' +
                '}';
    }
}
