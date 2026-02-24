package io.github.lvoxx.media_service.model;

import java.util.UUID;

import org.mapstruct.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("media_original")
public class MediaOriginal {

    @Id
    @Column("media_id")
    private UUID mediaId;

    @Column("cloudinary_public_id")
    private String cloudinaryPublicId;

    @Column("secure_url")
    private String secureUrl;

    @Column("resource_type")
    private String resourceType;

    @Column("signature")
    private String signature;
}