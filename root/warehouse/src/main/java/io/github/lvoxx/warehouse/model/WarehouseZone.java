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
 * WarehouseZone entity - Quản lý các khu vực trong kho
 */
@Table("warehouse_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseZone {

    @Id
    private Long id;

    @Column("warehouse_id")
    private Long warehouseId;

    @Column("zone_code")
    private String zoneCode;

    @Column("zone_name")
    private String zoneName;

    @Column("zone_type")
    private String zoneType; // RECEIVING, STORAGE, PICKING, PACKING, SHIPPING, COLD_STORAGE

    @Column("floor_level")
    private Integer floorLevel;

    @Column("temperature_min")
    private Double temperatureMin;

    @Column("temperature_max")
    private Double temperatureMax;

    @Column("humidity_min")
    private Double humidityMin;

    @Column("humidity_max")
    private Double humidityMax;

    @Column("area")
    private Double area; // Diện tích khu vực

    @Column("area_unit")
    private String areaUnit;

    @Column("max_weight_capacity")
    private Double maxWeightCapacity;

    @Column("weight_unit")
    private String weightUnit;

    @Column("status")
    private String status; // ACTIVE, INACTIVE, FULL, MAINTENANCE

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