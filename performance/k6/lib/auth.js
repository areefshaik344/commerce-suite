import { post } from './http.js';

export function login(email, password) {
  const res = post('/auth/login', { email, password });
  if (res.status !== 200) return null;
  const body = res.json();
  return body?.data?.accessToken || body?.accessToken;
}

export function refresh(refreshToken) {
  const res = post('/auth/refresh', { refreshToken });
  return res.status === 200 ? res.json()?.data?.accessToken : null;
}
