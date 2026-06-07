import { sleep } from 'k6';
import { post, put, ok } from '../lib/http.js';
import { login } from '../lib/auth.js';
import { profile, THRESHOLDS } from '../lib/config.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  const token = login('priya@vendor.com', 'password');
  if (!token) return;
  const create = post('/vendor/products', {
    title: `k6 product ${__VU}-${__ITER}`, categoryId: 'cat-fashion', price: 1999, stock: 100,
  }, token);
  ok(create, 'product.create');
  const pid = create.json()?.data?.id;
  if (pid) ok(put(`/vendor/inventory/${pid}`, { delta: 5 }, token), 'inventory.update');
  sleep(2);
}
