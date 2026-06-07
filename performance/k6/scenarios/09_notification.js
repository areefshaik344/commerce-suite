import { sleep } from 'k6';
import { get, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('rahul@example.com', 'password');
  if (!token) return;
  ok(get('/notifications?status=UNREAD&pageSize=20', token), 'notifications.list');
  sleep(1);
}
