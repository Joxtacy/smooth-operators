# Kong Route Configuration Fixes

## Issues Identified

Based on Kong analytics from the last 24 hours, several critical issues were found:

### 1. High 404 Error Rate (43%)
- Missing `/healthcheck` route for monitoring
- Inconsistent route paths causing mismatches

### 2. Performance Problems
- Fibonacci endpoint causing 504 timeouts (57% failure rate)
- No rate limiting configured
- No circuit breaker protection

## Recommended Kong Configuration

### Add Health Check Route
```bash
# Add health check route
curl -X POST http://kong-admin:8001/routes \
  -d name=health-check \
  -d paths[]=/healthcheck \
  -d paths[]=/health \
  -d paths[]=/api/health \
  -d service.id=<smooth-operator-service-id> \
  -d methods[]=GET,OPTIONS
```

### Add Rate Limiting Plugin
```bash
# Add rate limiting to fibonacci endpoint
curl -X POST http://kong-admin:8001/plugins \
  -d name=rate-limiting \
  -d route.id=<fibonacci-route-id> \
  -d config.second=5 \
  -d config.minute=100 \
  -d config.hour=1000
```

### Add Request Timeout Plugin
```bash
# Add timeout protection
curl -X POST http://kong-admin:8001/plugins \
  -d name=request-timeout \
  -d route.id=<fibonacci-route-id> \
  -d config.timeout=6000  # 6 second timeout
```

### Add Circuit Breaker Plugin
```bash
# Add circuit breaker for reliability
curl -X POST http://kong-admin:8001/plugins \
  -d name=circuit-breaker \
  -d service.id=<smooth-operator-service-id> \
  -d config.failure_threshold=5 \
  -d config.recovery_timeout=30 \
  -d config.success_threshold=3
```

### Update Service Configuration
```bash
# Update service timeouts
curl -X PATCH http://kong-admin:8001/services/<service-id> \
  -d connect_timeout=5000 \
  -d write_timeout=10000 \
  -d read_timeout=10000 \
  -d retries=3
```

## Expected Improvements

1. **Reduce 504 timeouts** from 11% to <1%
2. **Improve fibonacci performance** from 2.5s average to <100ms
3. **Add proper health monitoring** with `/healthcheck` endpoint
4. **Reduce error rate** from 57% to <5%
5. **Add rate limiting protection** against abuse
