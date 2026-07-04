import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'https://udjattrack-backend-production-c744.up.railway.app/api/v1';
const TOKEN = 'Bearer eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiUk9MRV9GTEVFVF9NQU5BR0VSIiwibmFtZSI6IkZsZWV0IEEiLCJ1c2VySWQiOiI3MjJmNzcwMS0xMjMwLTQxMjktYTJlMy1mZDNkY2U4OWFmYmQiLCJzdWIiOiJhbGkuZXNzYW0uYWJkZWxoYWxlZW0uMjBAZ21haWwuY29tIiwiaWF0IjoxNzgzMTA4MDA1LCJleHAiOjE3ODMxOTQ0MDV9.9IkGqjJEVxvzhB4It-BN3wTdeUUWaChgYKIezIONyp6rpRFfcAzPPgd_DGzBAHYL';
const FLEET_ID = '722f7701-1230-4129-a2e3-fd3dce89afbd';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // Ramp up to 20 users
    { duration: '1m', target: 100 },  // Stress: 100 concurrent dashboard viewers
    { duration: '30s', target: 0 },   // Ramp down
  ],
};

export default function () {
  const headers = { 'Authorization': TOKEN };
  const start = Date.now();
  
  const res = http.get(`${BASE_URL}/dashboard/${FLEET_ID}/fleet-status`, { headers });
  
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(2);
}
