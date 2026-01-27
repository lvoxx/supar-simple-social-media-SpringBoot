package io.github.lvoxx.warehouse.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StorageLocation entity - Quản lý vị trí lưu trữ cụ thể trong kho
 */
@Table("storage_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageLocation {
    
    @Id
    private Long id;
    
    @Column("zone_id")
    private Long zoneId;
    
    @Column("location_code")
    private String locationCode; // Mã vị trí (VD: A-01-02-03)
    
    @Column("aisle")
    private String aisle; // Lối đi
    
    @Column("rack")
    private String rack; // Giá kệ
    
    @Column("shelf")
    private String shelf; // Tầng kệ
    
    @Column("bin")
    private String bin; // Ngăn
    
    @Column("location_type")
    private String locationType; // PALLET, SHELF, FLOOR, BULK
    
    @Column("max_weight")
    private Double maxWeight;
    
    @Column("max_volume")
    private Double maxVolume;
    
    @Column("volume_unit")
    private String volumeUnit;
    
    @Column("current_weight")
    private Double currentWeight;
    
    @Column("current_volume")
    private Double currentVolume;
    
    @Column("width")
    private Double width;
    
    @Column("height")
    private Double height;
    
    @Column("depth")
    private Double depth;
    
    @Column("dimension_unit")
    private String dimensionUnit;
    
    @Column("status")
    private String status; // AVAILABLE, OCCUPIED, RESERVED, BLOCKED, MAINTENANCE
    
    @Column("is_pickable")
    private Boolean isPickable;
    
    @Column("is_replenishable")
    private Boolean isReplenishable;
    
    @Column("priority_level")
    private Integer priorityLevel; // Mức độ ưu tiên lấy hàng
    
    @Column("barcode")
    private String barcode;
    
    @Column("qr_code")
    private String qrCode;
    
    @Column("description")
    private String description;
    
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    @Column("created_by")
    private String createdBy;
    
    @Column("updated_by")
    private String updatedBy;
}