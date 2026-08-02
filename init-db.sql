-- Initialize database schema for Distributed API Security & Quantitative Intelligence Platform

-- Devices table: tracks registered mesh nodes
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_ip VARCHAR(45) NOT NULL UNIQUE,
    private_ip VARCHAR(45),
    last_heartbeat TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE', -- IDLE, BUSY, OFFLINE
    current_load INTEGER DEFAULT 0,
    region VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_region ON devices(region);
CREATE INDEX idx_devices_last_heartbeat ON devices(last_heartbeat);

-- API Requests table: logs all API requests processed by devices
CREATE TABLE IF NOT EXISTS api_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_url VARCHAR(2048) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_headers JSONB,
    request_body TEXT,
    response_body TEXT,
    status_code INTEGER,
    latency_ms INTEGER,
    device_id UUID REFERENCES devices(id),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    payload_hash VARCHAR(64)
);

CREATE INDEX idx_api_requests_device ON api_requests(device_id);
CREATE INDEX idx_api_requests_timestamp ON api_requests(timestamp);
CREATE INDEX idx_api_requests_target_url ON api_requests(target_url);

-- Test Results table: stores security test outcomes
CREATE TABLE IF NOT EXISTS test_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_type VARCHAR(50) NOT NULL, -- IDOR, BOLA, XSS, SQLi, etc.
    endpoint VARCHAR(2048) NOT NULL,
    vulnerability_found BOOLEAN DEFAULT FALSE,
    severity VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL
    details JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    device_id UUID REFERENCES devices(id)
);

CREATE INDEX idx_test_results_type ON test_results(test_type);
CREATE INDEX idx_test_results_severity ON test_results(severity);
CREATE INDEX idx_test_results_vulnerability ON test_results(vulnerability_found);

-- Extracted Data table: stores data extracted from APIs (product info, prices, etc.)
CREATE TABLE IF NOT EXISTS extracted_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_domain VARCHAR(255) NOT NULL,
    product_id VARCHAR(100),
    product_name VARCHAR(500),
    price DECIMAL(12, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    inventory_level INTEGER,
    shipping_date DATE,
    extracted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    raw_data JSONB
);

CREATE INDEX idx_extracted_data_source ON extracted_data(source_domain);
CREATE INDEX idx_extracted_data_product ON extracted_data(product_id);
CREATE INDEX idx_extracted_data_extracted_at ON extracted_data(extracted_at);

-- Trading Signals table: stores generated trading signals
CREATE TABLE IF NOT EXISTS trading_signals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(20) NOT NULL,
    action VARCHAR(10) NOT NULL, -- BUY, SELL, HOLD
    confidence DECIMAL(5, 2),
    price_target DECIMAL(12, 2),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    executed BOOLEAN DEFAULT FALSE,
    execution_price DECIMAL(12, 2),
    executed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_trading_signals_symbol ON trading_signals(symbol);
CREATE INDEX idx_trading_signals_action ON trading_signals(action);
CREATE INDEX idx_trading_signals_executed ON trading_signals(executed);
CREATE INDEX idx_trading_signals_generated_at ON trading_signals(generated_at);

-- Audit Logs table: immutable audit trail with optional blockchain anchoring
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id VARCHAR(100) UNIQUE NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_resource VARCHAR(500),
    result_hash VARCHAR(64),
    blockchain_tx_id VARCHAR(100),
    metadata JSONB
);

CREATE INDEX idx_audit_logs_entry_id ON audit_logs(entry_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_blockchain_tx ON audit_logs(blockchain_tx_id);

-- Job Queue table: manages distributed job assignments
CREATE TABLE IF NOT EXISTS job_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL, -- SECURITY_TEST, DATA_EXTRACTION, QUANT_ANALYSIS
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, ASSIGNED, COMPLETED, FAILED
    assigned_device_id UUID REFERENCES devices(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_job_queue_status ON job_queue(status);
CREATE INDEX idx_job_queue_type ON job_queue(job_type);
CREATE INDEX idx_job_queue_assigned ON job_queue(assigned_device_id);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for devices table
CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for testing
INSERT INTO devices (public_ip, private_ip, status, current_load, region) VALUES
    ('192.168.1.100', '10.0.0.10', 'IDLE', 0, 'us-east-1'),
    ('192.168.1.101', '10.0.0.11', 'IDLE', 0, 'us-west-2'),
    ('192.168.1.102', '10.0.0.12', 'BUSY', 5, 'eu-west-1');
