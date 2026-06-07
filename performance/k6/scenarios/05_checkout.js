import { sleep } from 'k6';
import { post, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';
import { idempotencyKey } from '../lib/data.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  ok(post('/cart/items', { variantId: 'var-1', qty: 1 }, token, { 'Idempotency-Key': idempotencyKey() }), 'cart.add');
  ok(post('/checkout/address',  { addressId: 'addr-1' }, token), 'checkout.address');
  ok(post('/checkout/shipping', { methodId: 'standard' }, token), 'checkout.shipping');
  ok(post('/checkout/payment',  { methodId: 'card' }, token), 'checkout.payment');
  ok(post('/checkout/reserve',  {}, token), 'checkout.reserve');
  sleep(1);
}
