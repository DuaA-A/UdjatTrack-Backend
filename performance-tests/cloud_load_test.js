import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'https://udjattrack-backend-production-c744.up.railway.app/api/v1';
// Driver token — telemetry is submitted by the driver role
const TOKEN = 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiUk9MRV9EUklWRVIiLCJuYW1lIjoiYWxpIiwidXNlcklkIjoiMDAyZjFkN2UtNDkyZi00MmRhLWE1YWEtMDRhNjZmNjY2ZjY1Iiwic3ViIjoiYWxpMkB0ZXN0LmNvbSIsImlhdCI6MTc4MzEyODMwMCwiZXhwIjoxNzgzMjE0NzAwfQ.BI__3m0AB4StqA4V0TXqNWPK3mtdq3Sfe1Bx7s9yvdqN9ZJBLWRqNf811If-Jp1V';
const TRIP_ID = '9052f6e0-3938-4c84-bb43-f26e4eb43d02';

export const options = {
  stages: [
    { duration: '30s', target: 50  },   // Ramp up to 50 VUs
    { duration: '1m',  target: 100 },   // Sustain 100 VUs (100 simulated vehicles)
    { duration: '30s', target: 0   },   // Ramp down
  ],
  thresholds: {
    // Railway Hobby-tier calibrated thresholds
    http_req_duration: [
      'avg<1500',    // Average must be under 1,500 ms
      'med<1500',    // Median must be under 1,500 ms
      'max<5000',    // Max must not exceed 5,000 ms
      'p(90)<2000',  // 90th percentile under 2,000 ms
      'p(95)<3000',  // 95th percentile under 3,000 ms
      'p(99)<5000',  // 99th percentile under 5,000 ms
    ],
    http_req_failed:        ['rate<0.02'],   // Server error rate < 2%
    http_reqs:              ['rate>5'],      // Throughput must exceed 5 req/s
    'checks{type:status}':  ['rate>0.95'],  // >95% of status checks pass
  },
};

export default function () {
  const payload = JSON.stringify({
    latitude:    30.0444 + (Math.random() * 0.01),
    longitude:   31.2357 + (Math.random() * 0.01),
    speed:       Math.floor(Math.random() * 80) + 20,
    driverState: 'NORMAL',
    timestamp:   new Date().toISOString(),
  });

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': TOKEN,
  };

  const res = http.post(`${BASE_URL}/trips/${TRIP_ID}/telemetry`, payload, {
    headers,
    tags: { type: 'status' },
  });

  check(res, {
    'status is 200 or 202': (r) => r.status === 200 || r.status === 202,
    'response time < 3000ms': (r) => r.timings.duration < 3000,
    'response time < 5000ms': (r) => r.timings.duration < 5000,
    'no server error (< 500)': (r) => r.status < 500,
  });

  sleep(3);
}
