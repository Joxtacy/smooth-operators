const express = require('express');
const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(express.json());

// Fixed: Use LRU cache instead of unbounded global array
class LRUCache {
  constructor(maxSize = 100) {
    this.maxSize = maxSize;
    this.cache = new Map();
  }

  set(key, value) {
    if (this.cache.has(key)) {
      this.cache.delete(key);
    }
    this.cache.set(key, value);
    
    if (this.cache.size > this.maxSize) {
      const firstKey = this.cache.keys().next().value;
      this.cache.delete(firstKey);
    }
  }

  size() {
    return this.cache.size;
  }
}

const requestCache = new LRUCache(100);

// Fixed: Reusable EventEmitter with proper cleanup
const EventEmitter = require('events');
const emitter = new EventEmitter();
emitter.setMaxListeners(20); // Prevent memory leak warnings

// Fixed: Track intervals for proper cleanup
const activeIntervals = new Map();
const activeTimeouts = new Map();

// Routes
app.get('/', (req, res) => {
  res.json({ message: 'Smooth Operators API is running!' });
});

app.get('/health', (req, res) => {
  const memoryUsage = process.memoryUsage();
  res.json({ 
    status: 'healthy', 
    timestamp: new Date().toISOString(),
    memory: {
      rss: `${Math.round(memoryUsage.rss / 1024 / 1024)}MB`,
      heapUsed: `${Math.round(memoryUsage.heapUsed / 1024 / 1024)}MB`,
      heapTotal: `${Math.round(memoryUsage.heapTotal / 1024 / 1024)}MB`
    },
    cache: {
      size: requestCache.size(),
      activeIntervals: activeIntervals.size,
      activeTimeouts: activeTimeouts.size
    }
  });
});

// Fixed route that prevents memory leaks
app.get('/slow', async (req, res) => {
  const requestId = Date.now() + Math.random();
  
  try {
    // Fixed: Use LRU cache with size limit and minimal data
    requestCache.set(requestId, {
      timestamp: new Date().toISOString(),
      url: req.url,
      ip: req.ip
      // Removed: userAgent, headers, body to reduce memory usage
    });

    // Fixed: Use one-time event listener with proper cleanup
    const handler = () => {
      console.log(`Request ${requestId} processed`);
    };
    emitter.once('request', handler); // Use once() instead of on()

    // Fixed: Create interval with automatic cleanup
    let processingCount = 0;
    const interval = setInterval(() => {
      console.log(`Processing request ${requestId}... (${++processingCount}/20)`);
      if (processingCount >= 20) {
        clearInterval(interval);
        activeIntervals.delete(requestId);
      }
    }, 100);
    activeIntervals.set(requestId, interval);

    // Fixed: Avoid large objects in closure
    const processData = () => {
      // Process data without capturing large objects
      const timeoutId = setTimeout(() => {
        console.log(`Data processed for request ${requestId}`);
        activeTimeouts.delete(requestId);
      }, 1000); // Reduced timeout
      activeTimeouts.set(requestId, timeoutId);
    };

    processData();

    // Simulate slow operation
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Emit event to trigger handler
    emitter.emit('request');

    res.json({ 
      message: 'Slow operation completed',
      requestId: requestId,
      cacheSize: requestCache.size(),
      activeIntervals: activeIntervals.size,
      activeTimeouts: activeTimeouts.size
    });
  } catch (error) {
    console.error(`Error processing request ${requestId}:`, error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Fixed route with proper resource management
app.get('/data', async (req, res) => {
  const requestId = Date.now() + Math.random();
  
  try {
    // Fixed: Create connections without storing them globally
    const connectionCount = 10;
    const connectionSummary = [];
    
    for (let i = 0; i < connectionCount; i++) {
      // Simulate connection processing without storing large objects
      const connection = {
        id: i,
        created: new Date().toISOString(),
        status: 'active'
        // Removed: large data array
      };
      connectionSummary.push(connection);
      
      // Process connection immediately and release reference
      // connection = null; // This would be done automatically by scope
    }

    // Store minimal summary in cache
    requestCache.set(requestId, {
      timestamp: new Date().toISOString(),
      type: 'data_request',
      connectionCount: connectionCount
    });

    res.json({ 
      message: 'Data retrieved',
      requestId: requestId,
      connections: connectionSummary.length,
      cacheSize: requestCache.size()
    });
  } catch (error) {
    console.error(`Error in data route:`, error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Add cleanup endpoint for monitoring/debugging
app.get('/cleanup', (req, res) => {
  const cleaned = {
    intervals: 0,
    timeouts: 0
  };

  // Clean up any orphaned intervals
  activeIntervals.forEach((interval, id) => {
    clearInterval(interval);
    cleaned.intervals++;
  });
  activeIntervals.clear();

  // Clean up any orphaned timeouts
  activeTimeouts.forEach((timeout, id) => {
    clearTimeout(timeout);
    cleaned.timeouts++;
  });
  activeTimeouts.clear();

  if (global.gc) {
    global.gc();
  }

  res.json({
    message: 'Cleanup completed',
    cleaned: cleaned,
    memoryUsage: process.memoryUsage()
  });
});

// Proper graceful shutdown handler
const gracefulShutdown = () => {
  console.log('Received shutdown signal, cleaning up...');
  
  // Clean up intervals
  activeIntervals.forEach((interval) => {
    clearInterval(interval);
  });
  activeIntervals.clear();

  // Clean up timeouts
  activeTimeouts.forEach((timeout) => {
    clearTimeout(timeout);
  });
  activeTimeouts.clear();

  // Remove all event listeners
  emitter.removeAllListeners();

  console.log('Cleanup completed, shutting down.');
  process.exit(0);
};

process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);

// Add uncaught exception handler
process.on('uncaughtException', (error) => {
  console.error('Uncaught Exception:', error);
  gracefulShutdown();
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  gracefulShutdown();
});

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
  console.log('Memory usage:', process.memoryUsage());
});