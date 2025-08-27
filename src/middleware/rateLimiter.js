const rateLimit = require('express-rate-limit');
const logger = require('../utils/logger');

/**
 * Rate limiting configuration for different endpoint types
 */
const createRateLimiter = (options = {}) => {
  const defaultOptions = {
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // limit each IP to 100 requests per windowMs
    message: {
      error: 'Too many requests from this IP',
      retryAfter: '15 minutes'
    },
    standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
    legacyHeaders: false, // Disable the `X-RateLimit-*` headers
    handler: (req, res) => {
      logger.warn('Rate limit exceeded', {
        ip: req.ip,
        method: req.method,
        url: req.url,
        userAgent: req.get('User-Agent'),
        timestamp: new Date().toISOString()
      });
      
      res.status(429).json({
        error: 'Too many requests from this IP, please try again later.',
        retryAfter: Math.round(options.windowMs / 1000) + ' seconds'
      });
    }
  };

  return rateLimit({ ...defaultOptions, ...options });
};

// Different rate limiters for different use cases
const rateLimiters = {
  // General API rate limiter
  general: createRateLimiter(),
  
  // Stricter rate limiter for auth endpoints
  auth: createRateLimiter({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 10, // limit each IP to 10 requests per windowMs
    message: {
      error: 'Too many authentication attempts',
      retryAfter: '15 minutes'
    }
  }),
  
  // Very strict rate limiter for password reset
  passwordReset: createRateLimiter({
    windowMs: 60 * 60 * 1000, // 1 hour
    max: 3, // limit each IP to 3 requests per hour
    message: {
      error: 'Too many password reset attempts',
      retryAfter: '1 hour'
    }
  })
};

module.exports = {
  createRateLimiter,
  ...rateLimiters
};