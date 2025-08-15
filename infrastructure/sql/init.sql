-- Crypto Trading Database Schema
-- Initialize database tables for the crypto trading system

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Crypto prices table for OHLCV data
CREATE TABLE crypto_prices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exchange VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    open_price DECIMAL(20, 8) NOT NULL,
    high_price DECIMAL(20, 8) NOT NULL,
    low_price DECIMAL(20, 8) NOT NULL,
    close_price DECIMAL(20, 8) NOT NULL,
    volume DECIMAL(20, 8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(exchange, symbol, timestamp)
);

-- Technical indicators table
CREATE TABLE technical_indicators (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    symbol VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    indicator_type VARCHAR(50) NOT NULL,
    indicator_value DECIMAL(20, 8) NOT NULL,
    period INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Trading strategies table
CREATE TABLE trading_strategies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    rules JSONB NOT NULL,
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Backtest results table
CREATE TABLE backtest_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    strategy_id UUID REFERENCES trading_strategies(id),
    symbol VARCHAR(20) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    initial_capital DECIMAL(20, 8) NOT NULL,
    final_capital DECIMAL(20, 8) NOT NULL,
    total_return DECIMAL(10, 4) NOT NULL,
    max_drawdown DECIMAL(10, 4) NOT NULL,
    sharpe_ratio DECIMAL(10, 4),
    total_trades INTEGER NOT NULL,
    winning_trades INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exchange VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    order_type VARCHAR(20) NOT NULL, -- MARKET, LIMIT, STOP_LOSS
    side VARCHAR(10) NOT NULL, -- BUY, SELL
    quantity DECIMAL(20, 8) NOT NULL,
    price DECIMAL(20, 8),
    status VARCHAR(20) NOT NULL, -- PENDING, FILLED, CANCELLED, REJECTED
    exchange_order_id VARCHAR(100),
    strategy_id UUID REFERENCES trading_strategies(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Portfolio positions table
CREATE TABLE portfolio_positions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    symbol VARCHAR(20) NOT NULL UNIQUE,
    quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    average_price DECIMAL(20, 8) NOT NULL DEFAULT 0,
    unrealized_pnl DECIMAL(20, 8) NOT NULL DEFAULT 0,
    realized_pnl DECIMAL(20, 8) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Risk metrics table
CREATE TABLE risk_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    portfolio_value DECIMAL(20, 8) NOT NULL,
    var_1d DECIMAL(20, 8), -- Value at Risk 1 day
    var_7d DECIMAL(20, 8), -- Value at Risk 7 days
    max_drawdown DECIMAL(10, 4),
    volatility DECIMAL(10, 4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_crypto_prices_symbol_timestamp ON crypto_prices(symbol, timestamp DESC);
CREATE INDEX idx_crypto_prices_exchange_symbol ON crypto_prices(exchange, symbol);
CREATE INDEX idx_technical_indicators_symbol_type ON technical_indicators(symbol, indicator_type, timestamp DESC);
CREATE INDEX idx_orders_symbol_status ON orders(symbol, status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_risk_metrics_timestamp ON risk_metrics(timestamp DESC);

-- Insert sample trading strategies
INSERT INTO trading_strategies (name, description, rules) VALUES 
('RSI Mean Reversion', 'Buy when RSI < 30, sell when RSI > 70', 
 '{"buy_conditions": [{"indicator": "RSI", "operator": "<", "value": 30}], "sell_conditions": [{"indicator": "RSI", "operator": ">", "value": 70}]}'),
('MACD Crossover', 'Buy on MACD bullish crossover, sell on bearish crossover',
 '{"buy_conditions": [{"indicator": "MACD_SIGNAL", "operator": "crossover", "value": "bullish"}], "sell_conditions": [{"indicator": "MACD_SIGNAL", "operator": "crossover", "value": "bearish"}]}');

-- Insert initial portfolio positions (starting with cash)
INSERT INTO portfolio_positions (symbol, quantity, average_price) VALUES 
('USDT', 10000.00, 1.00);

COMMIT;
