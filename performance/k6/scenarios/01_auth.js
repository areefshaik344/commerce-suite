import { sleep } from 'k6';
import { post, ok } from '../lib/http.js';
import { profile, THRESHOLDS } from '../lib/config.js';
import { pickUser } from '../lib/data.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const u = pickUser();
  const login = post('/auth/login', { email: u.email, password: u.password });
  ok(login, 'login');
  const rt = login.json()?.data?.refreshToken;
  if (rt) ok(post('/auth/refresh', { refreshToken: rt }), 'refresh');
  sleep(1);
}
