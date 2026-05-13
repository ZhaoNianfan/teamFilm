package com.myself.teamfiles.module.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "teamfiles_files", createIndex = true)
@Setting(shards = 1, replicas = 0)
public class FileDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard") // TODO: switch to ik_max_word/ik_smart after installing IK plugin
    private String originalName;

    @Field(type = FieldType.Keyword)
    private String fileName;

    @Field(type = FieldType.Keyword)
    private String fileType;

    @Field(type = FieldType.Keyword)
    private String fileExtension;

    @Field(type = FieldType.Keyword)
    private String mimeType;

    @Field(type = FieldType.Long)
    private Long fileSize;

    @Field(type = FieldType.Keyword)
    private String storageSpace;

    @Field(type = FieldType.Long)
    private Long folderId;

    @Field(type = FieldType.Long)
    private Long uploadUserId;

    @Field(type = FieldType.Keyword)
    private String uploadUsername;

    @Field(type = FieldType.Keyword)
    private String md5;

    @Field(type = FieldType.Integer)
    private Integer downloadCount;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
