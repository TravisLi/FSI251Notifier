package com.kohang.fsi251notifier.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.kohang.fsi251notifier.model.FSI251Data;

@Repository
public interface FSI251Repository extends MongoRepository<FSI251Data, String> {

	public FSI251Data findByCertNo(String certNo);
		
	@Query("""
	{
	$and:[
		{$expr: {
			$gte: [
				{
					$dateFromString: {
						dateString: "$certDate", 
						format: "%d/%m/%Y"
					}
				},
				{
					$dateFromString: {
						dateString: "?0", 
						format: "%d/%m/%Y"}
				}
			]
		}},
		{$expr: {
			$lte: [
				{
					$dateFromString: {
					   dateString: "$certDate", 
					   format: "%d/%m/%Y"}
				},
				{
					$dateFromString: {
					   dateString: "?1", 
					   format: "%d/%m/%Y"}
				}
			]
		}}
	] 
	} """)  
	public List<FSI251Data> findByDateRange(String start, String end);
	
}
