// k6 load test: shoppers browsing the catalog while others place orders.
//
// Run (stack must be up, rate limit raised so the test measures the services, not the limiter):
//   RATE_LIMIT_PER_SECOND=100000 RATE_LIMIT_BURST=100000 docker compose up -d api-gateway
//   docker run --rm -i --network ezdelivery_default grafana/k6:0.54.0 run - < loadtest/order-flow.js
//
// Tunables: -e BASE_URL=... -e BROWSE_RPS=... -e ORDER_RPS=... -e DURATION=...

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://api-gateway:8080';
const BROWSE_RPS = parseInt(__ENV.BROWSE_RPS || '200');
const ORDER_RPS = parseInt(__ENV.ORDER_RPS || '50');
const DURATION = __ENV.DURATION || '60s';
const PRODUCTS = 20;

const orderPlaceTime = new Trend('order_place_duration', true);
const productReadTime = new Trend('product_read_duration', true);

export const options = {
  scenarios: {
    browse: {
      executor: 'constant-arrival-rate',
      exec: 'browse',
      rate: BROWSE_RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
    checkout: {
      executor: 'constant-arrival-rate',
      exec: 'checkout',
      rate: ORDER_RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 20,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    product_read_duration: ['p(95)<300'],
    order_place_duration: ['p(95)<500'],
  },
};

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

// Creates the catalog once and gives every product plenty of stock.
export function setup() {
  const ids = [];
  for (let i = 0; i < PRODUCTS; i++) {
    const res = http.post(`${BASE_URL}/api/products`,
      JSON.stringify({ title: `Load Test Product ${i}`, price: 20 + i }), JSON_HEADERS);
    const id = res.json('id');
    http.put(`${BASE_URL}/api/inventory/${id}`, JSON.stringify({ availableQuantity: 1000000 }), JSON_HEADERS);
    ids.push(id);
  }
  return { ids };
}

export function browse(data) {
  const id = data.ids[Math.floor(Math.random() * data.ids.length)];
  const res = http.get(`${BASE_URL}/api/products/${id}`, { tags: { name: 'GET /api/products/{id}' } });
  productReadTime.add(res.timings.duration);
  check(res, { 'product 200': (r) => r.status === 200 });
}

export function checkout(data) {
  const pick = () => data.ids[Math.floor(Math.random() * data.ids.length)];
  const body = {
    customerEmail: `shopper${__VU}@loadtest.local`,
    customerName: 'Load Test',
    city: 'Coimbatore',
    items: [
      { productId: pick(), quantity: 1 + Math.floor(Math.random() * 3) },
      { productId: pick(), quantity: 1 },
    ],
  };
  const res = http.post(`${BASE_URL}/api/orders`, JSON.stringify(body),
    Object.assign({ tags: { name: 'POST /api/orders' } }, JSON_HEADERS));
  orderPlaceTime.add(res.timings.duration);
  check(res, { 'order 202': (r) => r.status === 202 });
}
