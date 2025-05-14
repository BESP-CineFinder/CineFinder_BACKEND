package com.cinefinder.theater.data;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "theater")
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchTheater {

	@Id
	@Field(name = "id")
	private String id;

	@Field(type = FieldType.Keyword)
	private String brand;

	@Field(type = FieldType.Keyword)
	private String theaterId;

	@Field(type = FieldType.Object, name = "location")
	private GeoPoint location;

}
