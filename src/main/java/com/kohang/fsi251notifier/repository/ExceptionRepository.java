package com.kohang.fsi251notifier.repository;

import com.kohang.fsi251notifier.model.ExceptionData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptionRepository extends MongoRepository<ExceptionData, String> {

    List<ExceptionData> findByResolved(Boolean resolved);

}
