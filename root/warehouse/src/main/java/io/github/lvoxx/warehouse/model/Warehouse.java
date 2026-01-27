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
 * Warehouse entity - Quản lý thông tin kho hàng
 */
@Table("warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
    
    @Id
    private Long id;
    
    @Column("warehouse_code")
    private String warehouseCode;
    
    @Column("warehouse_name")
    private String warehouseName;
    
    @Column("address")
    private String address;
    
    @Column("city")
    private String city;
    
    @Column("province")
    private String province;
    
    @Column("country")
    private String country;
    
    @Column("postal_code")
    private String postalCode;
    
    @Column("phone")
    private String phone;
    
    @Column("email")
    private String email;
    
    @Column("manager_name")
    private String managerName;
    
    @Column("capacity")
    private Double capacity; // Diện tích hoặc sức chứa
    
    @Column("capacity_unit")
    private String capacityUnit; // m2, m3, pallet, etc.
    
    @Column("warehouse_type")
    private String warehouseType; // STORAGE, DISTRIBUTION, COLD_STORAGE, etc.
    
    @Column("status")
    private String status; // ACTIVE, INACTIVE, MAINTENANCE
    
    @Column("latitude")
    private Double latitude;
    
    @Column("longitude")
    private Double longitude;
    
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
