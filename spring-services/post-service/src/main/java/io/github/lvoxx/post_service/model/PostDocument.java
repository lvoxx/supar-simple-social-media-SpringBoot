package io.github.lvoxx.post_service.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Elasticsearch document for posts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "posts")
public class PostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long postId;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    @Field(type = FieldType.Keyword)
    private String visibility;

    @Field(type = FieldType.Boolean)
    private Boolean commentsEnabled;

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(type = FieldType.Long)
    private Long likeCount;

    @Field(type = FieldType.Long)
    private Long commentCount;

    @Field(type = FieldType.Long)
    private Long shareCount;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Nested)
    private List<MediaInfo> media;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaInfo {
        @Field(type = FieldType.Keyword)
        private String mediaUrl;

        @Field(type = FieldType.Keyword)
        private String mediaType;

        @Field(type = FieldType.Integer)
        private Integer displayOrder;
    }
}