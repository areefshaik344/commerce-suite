// Race 100 VUs on a single SKU with stock=10.
// Reservation FSM + advisory lock must keep total reserved <= 10.
import { post, get } from '../lib/http.js';
import { check } from 'k6';
import { login } from '../lib/auth.js';
import { idempotencyKey } from '../lib/data.js';

export const options = {
  scenarios: { race: { executor: 'shared-iterations', vus: 100, iterations: 100, maxDuration: '30s' } },
  thresholds: { checks: ['rate>0.99'] },
};

const SKU = __ENV.RACE_SKU || 'var-low-stock';

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  post('/cart/items',        { variantId: SKU, qty: 1 }, token, { 'Idempotency-Key': idempotencyKey() });
  post('/checkout/reserve',  { variantId: SKU, qty: 1 }, token, { 'Idempotency-Key': idempotencyKey() });
}

export function teardown() {
  const probe = get(`/inventory/${SKU}`);
  const body = probe.json()?.data;
  check(body, { 'no overselling': v => !v || v.reserved <= v.totalStock });
}
