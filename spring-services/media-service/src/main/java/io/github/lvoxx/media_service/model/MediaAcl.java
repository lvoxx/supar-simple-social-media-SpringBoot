package io.github.lvoxx.media_service.model;

import java.time.Instant;
import java.util.UUID;

import javax.management.relation.RelationType;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("media_acl")
public class MediaAcl {

    @Id
    private UUID id;

    @Column("media_id")
    private UUID mediaId;

    @Column("allowed_user_id")
    private String allowedUserId;

    @Column("relation_type")
    private RelationType relationType;

    @Column("created_at")
    private Instant createdAt;
}