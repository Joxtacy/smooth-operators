# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2024-01-20

### Added
- **Winston Logger**: Comprehensive logging system with file and console outputs
  - Configurable log levels
  - Structured JSON logging
  - Separate error and combined log files
  - Console logging for development environments

- **Request Logging Middleware**: Automatic request/response logging
  - Logs incoming requests with method, URL, IP, and user agent
  - Tracks response times and status codes
  - Provides detailed monitoring capabilities

- **Rate Limiting Middleware**: Configurable rate limiting for API protection
  - General API rate limiter (100 requests per 15 minutes)
  - Strict auth endpoint limiter (10 requests per 15 minutes)
  - Very strict password reset limiter (3 requests per hour)
  - Detailed logging of rate limit violations

- **Health Check Routes**: Monitoring endpoints for service health
  - `/health` - Basic health check with uptime and status
  - `/health/detailed` - Comprehensive health metrics including memory usage, process info

- **Enhanced Package Configuration**:
  - Added winston and express-rate-limit dependencies
  - Version bump to 1.1.0
  - Added logs cleanup script
  - Enhanced package metadata with keywords

### Changed
- Updated project description to reflect enhanced security and logging features
- Improved project structure with better organized middleware

### Security
- Implemented rate limiting to prevent abuse
- Enhanced request monitoring and logging for security auditing

## [1.0.0] - 2024-01-05

### Added
- Initial project setup with Express.js
- Basic middleware configuration (CORS, Helmet, JSON parsing)
- JWT authentication support
- ESLint and Prettier configuration
- Jest testing framework setup
- Basic project structure with routes, middleware, and utils directories