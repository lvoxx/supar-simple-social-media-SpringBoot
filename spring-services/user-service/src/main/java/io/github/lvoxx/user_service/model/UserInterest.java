package io.github.lvoxx.user_service.model;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.common_core.model.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table("user_interests")
public class UserInterest extends BaseEntity {

    @Column("user_id")
    private Long userId;

    @Column("interest_category")
    private String interestCategory;

    @Column("interest_name")
    private String interestName;

    @Column("weight")
    @Builder.Default
    private Double weight = 1.0;
}