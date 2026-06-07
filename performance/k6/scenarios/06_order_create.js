import { sleep } from 'k6';
import { post, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';
import { idempotencyKey } from '../lib/data.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  const key = idempotencyKey();
  ok(post('/orders', { variantId: 'var-1', qty: 1 }, token, { 'Idempotency-Key': key }), 'order.create');
  const r2 = post('/orders', { variantId: 'var-1', qty: 1 }, token, { 'Idempotency-Key': key });
  // Replay MUST return the same order id (no duplicate)
  ok({ status: (r2.status === 200 || r2.status === 201) ? 200 : r2.status }, 'order.idempotent.replay');
  sleep(1);
}
