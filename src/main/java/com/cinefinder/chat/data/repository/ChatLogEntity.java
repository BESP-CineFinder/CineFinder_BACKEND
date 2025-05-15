package com.cinefinder.chat.data.repository;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "chat-logs")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatLogEntity {
    @Id
    private String id;
    private String senderId;
    private String message;

    @Field(type = FieldType.Date, format = DateFormat.strict_date_hour_minute_second)
    private LocalDateTime timestamp;
}
