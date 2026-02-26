package io.github.lvoxx.media_service.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.media_service.utils.VariantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("media_variant")
public class MediaVariant {

    @Id
    private UUID id;

    @Column("media_id")
    private UUID mediaId;

    @Column("variant")
    private VariantType variant;

    @Column("cdn_url")
    private String cdnUrl;

    @Column("bytes")
    private Long bytes;
}