const logger = require('../utils/logger');

/**
 * Request logging middleware
 * Logs incoming requests with method, URL, IP, and response time
 */
const requestLogger = (req, res, next) => {
  const startTime = Date.now();
  const { method, url, ip } = req;
  const userAgent = req.get('User-Agent') || 'Unknown';

  // Log the incoming request
  logger.info('Incoming request', {
    method,
    url,
    ip,
    userAgent,
    timestamp: new Date().toISOString()
  });

  // Override res.json to capture response data
  const originalJson = res.json;
  res.json = function(data) {
    const duration = Date.now() - startTime;
    
    // Log the response
    logger.info('Request completed', {
      method,
      url,
      ip,
      statusCode: res.statusCode,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    });

    return originalJson.call(this, data);
  };

  // Handle cases where res.json is not called
  res.on('finish', () => {
    if (!res.headersSent) return;
    
    const duration = Date.now() - startTime;
    logger.info('Request finished', {
      method,
      url,
      ip,
      statusCode: res.statusCode,
      duration: `${duration}ms`,
      timestamp: new Date().toISOString()
    });
  });

  next();
};

module.exports = requestLogger;