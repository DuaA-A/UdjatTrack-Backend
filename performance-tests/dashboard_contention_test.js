import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'https://udjattrack-backend-production-c744.up.railway.app/api/v1';
const TOKEN    = 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiUk9MRV9GTEVFVF9NQU5BR0VSIiwibmFtZSI6IkZsZWV0IEEiLCJ1c2VySWQiOiI3MjJmNzcwMS0xMjMwLTQxMjktYTJlMy1mZDNkY2U4OWFmYmQiLCJzdWIiOiJhbGkuZXNzYW0uYWJkZWxoYWxlZW0uMjBAZ21haWwuY29tIiwiaWF0IjoxNzgzMTI4MjYzLCJleHAiOjE3ODMyMTQ2NjN9.Djp8cMcAOmKkRzZXmOfOG5gnulyvFY5P2C-rwh0EdjjY5IUbL-NdbHzAekjVWtVh';
const FLEET_ID = '722f7701-1230-4129-a2e3-fd3dce89afbd';

export const options = {
  stages: [
    { duration: '30s', target: 20  },  // Ramp up to 20 concurrent fleet managers
    { duration: '1m',  target: 100 },  // Stress: 100 concurrent dashboard viewers
    { duration: '30s', target: 0   },  // Ramp down
  ],
  thresholds: {
    // Railway Hobby-tier calibrated thresholds for a read-heavy aggregation endpoint
    http_req_duration: [
      'avg<3000',    // Average under 3,000 ms
      'med<2500',    // Median under 2,500 ms
      'max<8000',    // Max must not exceed 8,000 ms
      'p(90)<5000',  // 90th percentile under 5,000 ms
      'p(95)<6000',  // 95th percentile under 6,000 ms
      'p(99)<7000',  // 99th percentile under 7,000 ms
    ],
    http_req_failed:       ['rate<0.01'],   // Error rate < 1%  (stricter — pure reads)
    http_reqs:             ['rate>1'],      // Throughput > 1 req/s
    'checks{type:status}': ['rate>0.99'],  // >99% status checks pass
  },
};

export default function () {
  const headers = { 'Authorization': TOKEN };

  const res = http.get(`${BASE_URL}/dashboard/${FLEET_ID}/fleet-status`, {
    headers,
    tags: { type: 'status' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 6000ms': (r) => r.timings.duration < 6000,
    'body not empty': (r) => r.body && r.body.length > 0,
    'no server error': (r) => r.status < 500,
  });

  sleep(2);
}
