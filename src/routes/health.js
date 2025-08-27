const express = require('express');
const router = express.Router();
const logger = require('../utils/logger');

/**
 * Health check endpoint
 * GET /health
 */
router.get('/', (req, res) => {
  const healthCheck = {
    uptime: process.uptime(),
    message: 'Service is running smoothly',
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'development',
    version: process.env.npm_package_version || '1.1.0'
  };

  try {
    logger.info('Health check requested', {
      ip: req.ip,
      timestamp: healthCheck.timestamp
    });
    
    res.status(200).json(healthCheck);
  } catch (error) {
    logger.error('Health check failed', { error: error.message });
    
    healthCheck.message = 'Service experiencing issues';
    res.status(503).json(healthCheck);
  }
});

/**
 * Detailed health check endpoint
 * GET /health/detailed
 */
router.get('/detailed', (req, res) => {
  const detailedHealth = {
    status: 'healthy',
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'development',
    version: process.env.npm_package_version || '1.1.0',
    memory: process.memoryUsage(),
    pid: process.pid,
    node_version: process.version,
    platform: process.platform
  };

  try {
    logger.info('Detailed health check requested', {
      ip: req.ip,
      timestamp: detailedHealth.timestamp
    });
    
    res.status(200).json(detailedHealth);
  } catch (error) {
    logger.error('Detailed health check failed', { error: error.message });
    
    detailedHealth.status = 'unhealthy';
    detailedHealth.error = error.message;
    res.status(503).json(detailedHealth);
  }
});

module.exports = router;