package com.kohang.fsi251notifier.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    private final String dbUser;
    private final String dbPass;
    private final String dbHost;

    public MongoConfig(@Value("#{systemProperties['db.user']!=null && systemProperties['db.user']!='' ? systemProperties['db.user'] : systemEnvironment['db_user']}")String dbUser,
                       @Value("#{systemProperties['db.password']!=null && systemProperties['db.password']!='' ? systemProperties['db.password'] : systemEnvironment['db_password']}")String dbPass,
                       @Value("${db_host}")String dbHost){
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
    protected String getDatabaseName() {
        return "fsi251";
    }

}
