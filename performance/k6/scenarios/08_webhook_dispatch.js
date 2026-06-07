import { sleep } from 'k6';
import { get, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('admin@marketplace.com', 'password');
  if (!token) return;
  ok(get('/admin/webhooks/deliveries?status=DELIVERED&pageSize=50', token), 'webhook.recent');
  ok(get('/actuator/metrics/webhooks.dispatch.success', token), 'webhook.metric');
  sleep(2);
}
