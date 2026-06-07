import { sleep } from 'k6';
import { post, del, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';
import { idempotencyKey, randomQty } from '../lib/data.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  ok(post('/cart/items', { variantId: 'var-1', qty: randomQty() }, token,
       { 'Idempotency-Key': idempotencyKey() }), 'cart.add');
  ok(del('/cart/items/var-1', token), 'cart.remove');
  sleep(1);
}
