package com.mottainai.mongoapi.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.mottainai.mongoapi.document.Metric;

public interface MetricRepository extends MongoRepository<Metric, String>, MetricRepositoryCustom {
}
