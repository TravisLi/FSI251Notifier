package com.kohang.fsi251notifier.mongo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.lang.NonNull;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private final String dbUser;
    private final String dbPass;
    private final String dbHost;

    public MongoConfig(@Value("${db.user}")String dbUser,
                       @Value("${db.password}")String dbPass,
                       @Value("${db.host}")String dbHost){
        this.dbUser=dbUser.strip();
        this.dbPass=dbPass.strip();
        this.dbHost=dbHost.strip();
    }

    public MongoClient mongoClient(){

        String template = "mongodb://%s:%s@%s:27017";
        String cs = String.format(template,dbUser,dbPass,dbHost);
        ConnectionString connectionString = new ConnectionString(cs);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return this.createMongoClient(mongoClientSettings);
    }

    @Override
    @NonNull
    protected String getDatabaseName() {
        return "fsi251";
    }

}
