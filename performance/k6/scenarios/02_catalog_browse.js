import { sleep } from 'k6';
import { get, ok } from '../lib/http.js';
import { profile, THRESHOLDS } from '../lib/config.js';

export const options = { ...profile(), thresholds: THRESHOLDS };

export default function () {
  ok(get('/categories'), 'categories');
  const list = get('/products?page=1&pageSize=24');
  ok(list, 'list');
  const id = list.json()?.data?.items?.[0]?.id;
  if (id) ok(get(`/products/${id}`), 'pdp');
  sleep(1);
}
