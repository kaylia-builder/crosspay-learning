-- CrossPay Learning Project - Database Initialization
-- 初始化表结构 + 测试数据

CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    country VARCHAR(50) DEFAULT 'Kenya',
    currency VARCHAR(10) DEFAULT 'USD',
    fee_rate DECIMAL(5,4) DEFAULT 0.0300,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    merchant_order_no VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    channel VARCHAR(50),
    channel_order_no VARCHAR(100),
    fail_reason VARCHAR(500),
    callback_received_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(30) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    fee_amount DECIMAL(12,2) NOT NULL,
    net_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    settlement_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS settlement_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    payment_order_id BIGINT NOT NULL,
    FOREIGN KEY (settlement_id) REFERENCES settlements(id),
    FOREIGN KEY (payment_order_id) REFERENCES payment_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'OPERATOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员 (密码: admin123)
INSERT INTO admin_users (username, password_hash, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN')
ON DUPLICATE KEY UPDATE username=username;

-- 插入测试商户 (密码: merchant123)
INSERT INTO merchants (merchant_no, name, email, password_hash, country, currency, fee_rate)
VALUES ('M20240801001', 'AfricaShop Demo', 'demo@africashop.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Kenya', 'USD', 0.0300)
ON DUPLICATE KEY UPDATE merchant_no=merchant_no;
