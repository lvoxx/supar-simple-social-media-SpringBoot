package io.github.lvoxx.user_service.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@Table("user_interests")
public class UserInterest implements Persistable<Long> {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("interest_category")
    private String interestCategory;
    
    @Column("interest_name")
    private String interestName;
    
    @Column("weight")
    private Double weight;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Transient
    @Default
    private boolean isNew = false;
    
    @Override
    public boolean isNew() {
        return isNew || id == null;
    }
    
    public UserInterest setAsNew() {
        this.isNew = true;
        return this;
    }
}