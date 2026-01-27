-- Warehouse Management System Database Schema
-- R2DBC Compatible PostgreSQL Script

-- Create warehouses table
CREATE TABLE IF NOT EXISTS warehouses (
    id BIGSERIAL PRIMARY KEY,
    warehouse_code VARCHAR(50) UNIQUE NOT NULL,
    warehouse_name VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    province VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    phone VARCHAR(50),
    email VARCHAR(255),
    manager_name VARCHAR(255),
    capacity NUMERIC(15, 2),
    capacity_unit VARCHAR(20),
    warehouse_type VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create warehouse zones table
CREATE TABLE IF NOT EXISTS warehouse_zones (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    zone_code VARCHAR(50) NOT NULL,
    zone_name VARCHAR(255) NOT NULL,
    zone_type VARCHAR(50),
    floor_level INTEGER,
    temperature_min NUMERIC(5, 2),
    temperature_max NUMERIC(5, 2),
    humidity_min NUMERIC(5, 2),
    humidity_max NUMERIC(5, 2),
    area NUMERIC(15, 2),
    area_unit VARCHAR(20),
    max_weight_capacity NUMERIC(15, 2),
    weight_unit VARCHAR(20),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT uk_zone_code UNIQUE (warehouse_id, zone_code)
);

-- Create storage locations table
CREATE TABLE IF NOT EXISTS storage_locations (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    location_code VARCHAR(100) UNIQUE NOT NULL,
    aisle VARCHAR(20),
    rack VARCHAR(20),
    shelf VARCHAR(20),
    bin VARCHAR(20),
    location_type VARCHAR(50),
    max_weight NUMERIC(15, 2),
    max_volume NUMERIC(15, 2),
    volume_unit VARCHAR(20),
    current_weight NUMERIC(15, 2) DEFAULT 0,
    current_volume NUMERIC(15, 2) DEFAULT 0,
    width NUMERIC(10, 2),
    height NUMERIC(10, 2),
    depth NUMERIC(10, 2),
    dimension_unit VARCHAR(20),
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    is_pickable BOOLEAN DEFAULT TRUE,
    is_replenishable BOOLEAN DEFAULT TRUE,
    priority_level INTEGER,
    barcode VARCHAR(255),
    qr_code VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_location_zone FOREIGN KEY (zone_id) REFERENCES warehouse_zones(id)
);

-- Create inventory table
CREATE TABLE IF NOT EXISTS inventory (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    quantity NUMERIC(15, 2) NOT NULL DEFAULT 0,
    available_quantity NUMERIC(15, 2) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(15, 2) NOT NULL DEFAULT 0,
    damaged_quantity NUMERIC(15, 2) NOT NULL DEFAULT 0,
    unit_of_measure VARCHAR(20),
    unit_cost NUMERIC(15, 2),
    total_cost NUMERIC(15, 2),
    currency VARCHAR(10),
    manufacture_date DATE,
    expiry_date DATE,
    received_date DATE,
    supplier_id BIGINT,
    purchase_order_number VARCHAR(100),
    lot_number VARCHAR(100),
    pallet_id VARCHAR(100),
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    quality_status VARCHAR(50),
    weight NUMERIC(15, 2),
    weight_unit VARCHAR(20),
    volume NUMERIC(15, 2),
    volume_unit VARCHAR(20),
    min_stock_level NUMERIC(15, 2),
    max_stock_level NUMERIC(15, 2),
    reorder_point NUMERIC(15, 2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) REFERENCES storage_locations(id),
    CONSTRAINT uk_inventory_unique UNIQUE (warehouse_id, location_id, product_id, batch_number, serial_number)
);

-- Create inventory transactions table
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_number VARCHAR(100) UNIQUE NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    from_location_id BIGINT,
    to_location_id BIGINT,
    quantity NUMERIC(15, 2) NOT NULL,
    unit_of_measure VARCHAR(20),
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    reference_number VARCHAR(100),
    reference_type VARCHAR(50),
    unit_cost NUMERIC(15, 2),
    total_cost NUMERIC(15, 2),
    currency VARCHAR(10),
    reason_code VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PENDING',
    transaction_date TIMESTAMP NOT NULL,
    completed_date TIMESTAMP,
    user_id VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT fk_transaction_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Create goods receipts table
CREATE TABLE IF NOT EXISTS goods_receipts (
    id BIGSERIAL PRIMARY KEY,
    receipt_number VARCHAR(100) UNIQUE NOT NULL,
    warehouse_id BIGINT NOT NULL,
    supplier_id BIGINT,
    purchase_order_number VARCHAR(100),
    receipt_date DATE NOT NULL,
    expected_date DATE,
    carrier_name VARCHAR(255),
    tracking_number VARCHAR(100),
    vehicle_number VARCHAR(50),
    driver_name VARCHAR(255),
    driver_phone VARCHAR(50),
    receipt_type VARCHAR(50),
    status VARCHAR(50) DEFAULT 'DRAFT',
    total_items INTEGER,
    total_quantity NUMERIC(15, 2),
    received_by VARCHAR(100),
    inspection_status VARCHAR(50),
    inspection_date TIMESTAMP,
    inspected_by VARCHAR(100),
    notes TEXT,
    attachment_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_receipt_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Create goods receipt items table
CREATE TABLE IF NOT EXISTS goods_receipt_items (
    id BIGSERIAL PRIMARY KEY,
    receipt_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    expected_quantity NUMERIC(15, 2) NOT NULL,
    received_quantity NUMERIC(15, 2) DEFAULT 0,
    accepted_quantity NUMERIC(15, 2) DEFAULT 0,
    rejected_quantity NUMERIC(15, 2) DEFAULT 0,
    unit_of_measure VARCHAR(20),
    location_id BIGINT,
    unit_cost NUMERIC(15, 2),
    total_cost NUMERIC(15, 2),
    currency VARCHAR(10),
    manufacture_date DATE,
    expiry_date DATE,
    lot_number VARCHAR(100),
    quality_status VARCHAR(50),
    rejection_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_receipt_item_receipt FOREIGN KEY (receipt_id) REFERENCES goods_receipts(id),
    CONSTRAINT fk_receipt_item_location FOREIGN KEY (location_id) REFERENCES storage_locations(id),
    CONSTRAINT uk_receipt_item_line UNIQUE (receipt_id, line_number)
);

-- Create goods issues table
CREATE TABLE IF NOT EXISTS goods_issues (
    id BIGSERIAL PRIMARY KEY,
    issue_number VARCHAR(100) UNIQUE NOT NULL,
    warehouse_id BIGINT NOT NULL,
    customer_id BIGINT,
    sales_order_number VARCHAR(100),
    issue_date DATE NOT NULL,
    requested_date DATE,
    ship_to_address TEXT,
    ship_to_city VARCHAR(100),
    ship_to_province VARCHAR(100),
    ship_to_country VARCHAR(100),
    ship_to_postal_code VARCHAR(20),
    carrier_name VARCHAR(255),
    tracking_number VARCHAR(100),
    vehicle_number VARCHAR(50),
    driver_name VARCHAR(255),
    driver_phone VARCHAR(50),
    issue_type VARCHAR(50),
    priority VARCHAR(50) DEFAULT 'NORMAL',
    status VARCHAR(50) DEFAULT 'DRAFT',
    total_items INTEGER,
    total_quantity NUMERIC(15, 2),
    picked_by VARCHAR(100),
    packed_by VARCHAR(100),
    shipped_by VARCHAR(100),
    picking_started_at TIMESTAMP,
    picking_completed_at TIMESTAMP,
    packing_started_at TIMESTAMP,
    packing_completed_at TIMESTAMP,
    shipped_at TIMESTAMP,
    notes TEXT,
    attachment_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_issue_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Create goods issue items table
CREATE TABLE IF NOT EXISTS goods_issue_items (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    requested_quantity NUMERIC(15, 2) NOT NULL,
    picked_quantity NUMERIC(15, 2) DEFAULT 0,
    shipped_quantity NUMERIC(15, 2) DEFAULT 0,
    unit_of_measure VARCHAR(20),
    from_location_id BIGINT,
    unit_price NUMERIC(15, 2),
    total_price NUMERIC(15, 2),
    currency VARCHAR(10),
    lot_number VARCHAR(100),
    picking_status VARCHAR(50),
    short_pick_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_issue_item_issue FOREIGN KEY (issue_id) REFERENCES goods_issues(id),
    CONSTRAINT fk_issue_item_location FOREIGN KEY (from_location_id) REFERENCES storage_locations(id),
    CONSTRAINT uk_issue_item_line UNIQUE (issue_id, line_number)
);

-- Create stock transfers table
CREATE TABLE IF NOT EXISTS stock_transfers (
    id BIGSERIAL PRIMARY KEY,
    transfer_number VARCHAR(100) UNIQUE NOT NULL,
    from_warehouse_id BIGINT NOT NULL,
    to_warehouse_id BIGINT NOT NULL,
    transfer_date DATE NOT NULL,
    expected_arrival_date DATE,
    actual_arrival_date DATE,
    transfer_type VARCHAR(50),
    status VARCHAR(50) DEFAULT 'DRAFT',
    priority VARCHAR(50) DEFAULT 'NORMAL',
    total_items INTEGER,
    total_quantity NUMERIC(15, 2),
    carrier_name VARCHAR(255),
    tracking_number VARCHAR(100),
    vehicle_number VARCHAR(50),
    requested_by VARCHAR(100),
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    shipped_by VARCHAR(100),
    shipped_at TIMESTAMP,
    received_by VARCHAR(100),
    received_at TIMESTAMP,
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_transfer_from_warehouse FOREIGN KEY (from_warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_transfer_to_warehouse FOREIGN KEY (to_warehouse_id) REFERENCES warehouses(id)
);

-- Create stock transfer items table
CREATE TABLE IF NOT EXISTS stock_transfer_items (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    batch_number VARCHAR(100),
    serial_number VARCHAR(100),
    from_location_id BIGINT,
    to_location_id BIGINT,
    requested_quantity NUMERIC(15, 2) NOT NULL,
    shipped_quantity NUMERIC(15, 2) DEFAULT 0,
    received_quantity NUMERIC(15, 2) DEFAULT 0,
    unit_of_measure VARCHAR(20),
    lot_number VARCHAR(100),
    status VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id) REFERENCES stock_transfers(id),
    CONSTRAINT fk_transfer_item_from_location FOREIGN KEY (from_location_id) REFERENCES storage_locations(id),
    CONSTRAINT fk_transfer_item_to_location FOREIGN KEY (to_location_id) REFERENCES storage_locations(id),
    CONSTRAINT uk_transfer_item_line UNIQUE (transfer_id, line_number)
);

-- Create indexes for better query performance
CREATE INDEX idx_warehouse_code ON warehouses(warehouse_code);
CREATE INDEX idx_warehouse_status ON warehouses(status);

CREATE INDEX idx_zone_warehouse ON warehouse_zones(warehouse_id);
CREATE INDEX idx_zone_status ON warehouse_zones(status);

CREATE INDEX idx_location_zone ON storage_locations(zone_id);
CREATE INDEX idx_location_code ON storage_locations(location_code);
CREATE INDEX idx_location_status ON storage_locations(status);

CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX idx_inventory_location ON inventory(location_id);
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_sku ON inventory(sku);
CREATE INDEX idx_inventory_status ON inventory(status);
CREATE INDEX idx_inventory_expiry ON inventory(expiry_date);

CREATE INDEX idx_transaction_warehouse ON inventory_transactions(warehouse_id);
CREATE INDEX idx_transaction_type ON inventory_transactions(transaction_type);
CREATE INDEX idx_transaction_date ON inventory_transactions(transaction_date);
CREATE INDEX idx_transaction_status ON inventory_transactions(status);

CREATE INDEX idx_receipt_warehouse ON goods_receipts(warehouse_id);
CREATE INDEX idx_receipt_number ON goods_receipts(receipt_number);
CREATE INDEX idx_receipt_status ON goods_receipts(status);
CREATE INDEX idx_receipt_date ON goods_receipts(receipt_date);

CREATE INDEX idx_receipt_item_receipt ON goods_receipt_items(receipt_id);
CREATE INDEX idx_receipt_item_product ON goods_receipt_items(product_id);

CREATE INDEX idx_issue_warehouse ON goods_issues(warehouse_id);
CREATE INDEX idx_issue_number ON goods_issues(issue_number);
CREATE INDEX idx_issue_status ON goods_issues(status);
CREATE INDEX idx_issue_date ON goods_issues(issue_date);

CREATE INDEX idx_issue_item_issue ON goods_issue_items(issue_id);
CREATE INDEX idx_issue_item_product ON goods_issue_items(product_id);

CREATE INDEX idx_transfer_from_warehouse ON stock_transfers(from_warehouse_id);
CREATE INDEX idx_transfer_to_warehouse ON stock_transfers(to_warehouse_id);
CREATE INDEX idx_transfer_status ON stock_transfers(status);
CREATE INDEX idx_transfer_date ON stock_transfers(transfer_date);

CREATE INDEX idx_transfer_item_transfer ON stock_transfer_items(transfer_id);
CREATE INDEX idx_transfer_item_product ON stock_transfer_items(product_id);