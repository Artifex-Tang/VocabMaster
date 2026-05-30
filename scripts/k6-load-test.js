// k6 load test — VocabMaster API
// Target: 500 concurrent users, P95 < 500ms
//
// Usage:
//   k6 run scripts/k6-load-test.js
//   k6 run --vus 500 --duration 60s scripts/k6-load-test.js
//
// Prerequisites:
//   - Backend running at http://localhost:8080
//   - Seed data loaded

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── Config ──
const BASE = __ENV.API_BASE || 'http://localhost:8080/api/v1';

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // ramp-up
    { duration: '1m',  target: 200 },  // hold
    { duration: '30s', target: 500 },  // peak
    { duration: '1m',  target: 500 },  // sustain
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],   // P95 < 500ms
    http_req_failed: ['rate<0.05'],     // <5% failures
    checks: ['rate>0.95'],             // >95% checks pass
  },
};

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency');

// ── Test data ──
const TEST_USER = {
  email: `k6-test-${Date.now()}@loadtest.com`,
  password: 'Test1234!@#',
  nickname: 'k6 Tester',
};

// ── Setup: register a batch of test users ──
export function setup() {
  const tokens = [];
  // Pre-register 10 users for the test pool
  for (let i = 0; i < 10; i++) {
    const email = `k6-${i}-${Date.now()}@loadtest.com`;
    const res = http.post(
      `${BASE}/auth/register`,
      JSON.stringify({ email, password: 'Test1234!@#', nickname: `k6-${i}` }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 200 || res.status === 201) {
      const body = JSON.parse(res.body);
      tokens.push(body.data?.access_token || body.access_token || '');
    }
  }
  return { tokens };
}

// ── Main test ──
export default function (data) {
  const token = data.tokens[__VU % data.tokens.length] || '';
  const authHeaders = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  };

  // 1. Get levels (public)
  group('GET /words/levels', () => {
    const res = http.get(`${BASE}/words/levels`);
    check(res, {
      'levels status 200': (r) => r.status === 200,
      'levels has data': (r) => JSON.parse(r.body).data?.length > 0,
    });
    apiLatency.add(res.timings.duration);
  });

  sleep(0.5);

  // 2. Get today's plan (authenticated)
  group('GET /study/today', () => {
    const res = http.get(`${BASE}/study/today?level=CET4`, authHeaders);
    check(res, {
      'today status ok': (r) => r.status === 200 || r.status === 401,
    });
    apiLatency.add(res.timings.duration);
  });

  sleep(0.3);

  // 3. Search words
  group('GET /words/search', () => {
    const res = http.get(`${BASE}/words/search?keyword=abandon&level=CET4`, authHeaders);
    check(res, {
      'search status ok': (r) => r.status === 200 || r.status === 401,
    });
    apiLatency.add(res.timings.duration);
  });

  sleep(1);
}

// ── Teardown ──
export function teardown(data) {
  // In real env, clean up test users here
  console.log(`Load test complete. Tokens used: ${data.tokens.length}`);
}
