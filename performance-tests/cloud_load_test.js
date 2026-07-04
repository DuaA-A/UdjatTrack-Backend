import http from 'k6/http';
import { check, sleep } from 'k6';

// REPLACE WITH YOUR DEPLOYED CLOUD URL AND A VALID AUTH TOKEN
const BASE_URL = 'https://udjattrack-backend-production-c744.up.railway.app/api/v1';
const TOKEN = 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiUk9MRV9GTEVFVF9NQU5BR0VSIiwibmFtZSI6IkZsZWV0IEEiLCJ1c2VySWQiOiI3MjJmNzcwMS0xMjMwLTQxMjktYTJlMy1mZDNkY2U4OWFmYmQiLCJzdWIiOiJhbGkuZXNzYW0uYWJkZWxoYWxlZW0uMjBAZ21haWwuY29tIiwiaWF0IjoxNzgzMTA4MDA1LCJleHAiOjE3ODMxOTQ0MDV9.9IkGqjJEVxvzhB4It-BN3wTdeUUWaChgYKIezIONyp6rpRFfcAzPPgd_DGzBAHYL'; 
const TRIP_ID = '9052f6e0-3938-4c84-bb43-f26e4eb43d02'; // Ensure this trip exists and is ONGOING

export const options = {
  stages: [
    { duration: '30s', target: 500 },  // Ramp up
    { duration: '1m', target: 1000 },  // 1000 active vehicles
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],   // Error rate must be less than 5%
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% of requests under 500ms
  },
};

export default function () {
  const payload = JSON.stringify({
    latitude: 30.0444 + (Math.random() * 0.01),
    longitude: 31.2357 + (Math.random() * 0.01),
    speed: Math.floor(Math.random() * 80) + 20,
    driverState: 'ALERT',
    timestamp: new Date().toISOString()
  });

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': TOKEN,
  };

  const res = http.post(`${BASE_URL}/telemetry/${TRIP_ID}`, payload, { headers });

  check(res, {
    'is status 202 or 200': (r) => r.status === 202 || r.status === 200,
  });

  sleep(3); // Simulate exact 1 request every 3 seconds
}
