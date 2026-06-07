import { sleep } from 'k6';
import { post, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';
import { idempotencyKey } from '../lib/data.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  const intent = post('/payments/intent', { orderId: 'order-mock', amount: 1999 }, token,
                      { 'Idempotency-Key': idempotencyKey() });
  ok(intent, 'payment.intent');
  const pid = intent.json()?.data?.id;
  if (pid) ok(post(`/payments/${pid}/capture`, {}, token, { 'Idempotency-Key': idempotencyKey() }), 'payment.capture');
  sleep(1);
}
