import { useState, useEffect, useCallback, useRef } from 'react';
import webSocketService from '../services/WebSocketService';

export const useWebSocket = () => {
  const [connected, setConnected] = useState(false);
  const [connectionState, setConnectionState] = useState('DISCONNECTED');
  const [error, setError] = useState(null);
  const connectAttempted = useRef(false);

  useEffect(() => {
    if (!connectAttempted.current) {
      connectAttempted.current = true;
      connect();
    }

    return () => {
      disconnect();
    };
  }, []);

  const connect = useCallback(async () => {
    try {
      setError(null);
      setConnectionState('CONNECTING');
      
      await webSocketService.connect();
      
      setConnected(true);
      setConnectionState('CONNECTED');
      console.log('WebSocket connected successfully');
      
    } catch (error) {
      console.error('Failed to connect WebSocket:', error);
      setError(error.message);
      setConnected(false);
      setConnectionState('DISCONNECTED');
    }
  }, []);

  const disconnect = useCallback(() => {
    webSocketService.disconnect();
    setConnected(false);
    setConnectionState('DISCONNECTED');
  }, []);

  const reconnect = useCallback(() => {
    disconnect();
    setTimeout(connect, 1000);
  }, [connect, disconnect]);

  return {
    connected,
    connectionState,
    error,
    connect,
    disconnect,
    reconnect,
    isConnected: webSocketService.isConnected.bind(webSocketService)
  };
};

export const usePriceUpdates = () => {
  const [prices, setPrices] = useState({});
  const [lastUpdate, setLastUpdate] = useState(null);
  const subscriptionRef = useRef(null);

  useEffect(() => {
    const handlePriceUpdate = (priceData) => {
      setPrices(prev => ({
        ...prev,
        [priceData.productId]: priceData
      }));
      setLastUpdate(new Date());
    };

    if (webSocketService.isConnected()) {
      subscriptionRef.current = webSocketService.subscribeToPrices(handlePriceUpdate);
    }

    return () => {
      if (subscriptionRef.current) {
        webSocketService.unsubscribe('prices');
      }
    };
  }, []);

  return {
    prices,
    lastUpdate,
    getPriceForSymbol: (symbol) => prices[symbol] || null
  };
};

export const useTradeUpdates = () => {
  const [trades, setTrades] = useState([]);
  const [lastTrade, setLastTrade] = useState(null);
  const subscriptionRef = useRef(null);
  const maxTrades = 100;

  useEffect(() => {
    const handleTradeUpdate = (tradeData) => {
      setTrades(prev => {
        const newTrades = [tradeData, ...prev];
        return newTrades.slice(0, maxTrades);
      });
      setLastTrade(tradeData);
    };

    if (webSocketService.isConnected()) {
      subscriptionRef.current = webSocketService.subscribeToTrades(handleTradeUpdate);
    }

    return () => {
      if (subscriptionRef.current) {
        webSocketService.unsubscribe('trades');
      }
    };
  }, []);

  return {
    trades,
    lastTrade,
    getTradesForSymbol: (symbol) => trades.filter(trade => trade.productId === symbol)
  };
};

export const useSymbolData = (symbol) => {
  const [symbolData, setSymbolData] = useState(null);
  const [symbolTrades, setSymbolTrades] = useState([]);
  const subscriptionRef = useRef(null);

  useEffect(() => {
    if (!symbol) return;

    const handleSymbolUpdate = (data) => {
      if (data.productId) {
        // Price update
        setSymbolData(data);
      } else if (data.price && data.size) {
        // Trade update
        setSymbolTrades(prev => {
          const newTrades = [data, ...prev];
          return newTrades.slice(0, 50); // Keep last 50 trades
        });
      }
    };

    if (webSocketService.isConnected()) {
      subscriptionRef.current = webSocketService.subscribeToSymbol(symbol, handleSymbolUpdate);
    }

    return () => {
      if (subscriptionRef.current) {
        webSocketService.unsubscribe(`symbol-${symbol}`);
      }
    };
  }, [symbol]);

  return {
    symbolData,
    symbolTrades,
    isSubscribed: subscriptionRef.current !== null
  };
};

export const useSystemStatus = () => {
  const [systemStatus, setSystemStatus] = useState({});
  const [lastStatusUpdate, setLastStatusUpdate] = useState(null);
  const subscriptionRef = useRef(null);

  useEffect(() => {
    const handleStatusUpdate = (statusData) => {
      setSystemStatus(statusData);
      setLastStatusUpdate(new Date());
    };

    if (webSocketService.isConnected()) {
      subscriptionRef.current = webSocketService.subscribeToSystemStatus(handleStatusUpdate);
    }

    return () => {
      if (subscriptionRef.current) {
        webSocketService.unsubscribe('system');
      }
    };
  }, []);

  return {
    systemStatus,
    lastStatusUpdate,
    isHealthy: systemStatus.status === 'UP'
  };
};
