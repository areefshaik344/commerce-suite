import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const USERS = [
  { email: 'rahul@example.com', password: 'password' },
  { email: 'priya@vendor.com',  password: 'password' },
];

export function pickUser()        { return USERS[Math.floor(Math.random() * USERS.length)]; }
export function idempotencyKey()  { return `k6-${__VU}-${__ITER}-${randomString(12)}`; }
export function randomQty()       { return randomIntBetween(1, 3); }
