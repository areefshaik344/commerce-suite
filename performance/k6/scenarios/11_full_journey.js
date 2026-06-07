import { sleep } from 'k6';
import { get, post, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';
import { idempotencyKey } from '../lib/data.js';

export const options = { ...profile('soak'), thresholds: THRESHOLDS };

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  const list = get('/products?page=1&pageSize=12', token);
  ok(list, 'browse');
  const id = list.json()?.data?.items?.[0]?.id || 'var-1';
  ok(post('/cart/items', { variantId: id, qty: 1 }, token, { 'Idempotency-Key': idempotencyKey() }), 'cart');
  ok(post('/checkout/address', { addressId: 'addr-1' }, token), 'addr');
  ok(post('/checkout/payment', { methodId: 'card' }, token), 'pay');
  const order = post('/orders', { variantId: id, qty: 1 }, token, { 'Idempotency-Key': idempotencyKey() });
  ok(order, 'order');
  const oid = order.json()?.data?.id;
  if (oid) ok(post('/payments/intent', { orderId: oid, amount: 1999 }, token,
                   { 'Idempotency-Key': idempotencyKey() }), 'pay.intent');
  sleep(3);
}
