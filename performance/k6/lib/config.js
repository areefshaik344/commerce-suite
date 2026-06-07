export const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:8080/api/v1';

const PROFILES = {
  smoke:  { vus: 5,   duration: '1m'  },
  load:   { vus: 200, duration: '15m' },
  stress: { vus: 800, duration: '20m' },
  soak:   { vus: 150, duration: '60m' },
  spike:  {
    stages: [
      { target: 50,   duration: '30s' },
      { target: 1000, duration: '1m'  },
      { target: 1000, duration: '2m'  },
      { target: 0,    duration: '1m'  },
    ],
  },
};

export function profile(name = __ENV.LOAD || 'smoke') {
  return PROFILES[name] || PROFILES.smoke;
}

export const THRESHOLDS = {
  http_req_failed:   ['rate<0.01'],
  http_req_duration: ['p(95)<800', 'p(99)<1500'],
  checks:            ['rate>0.99'],
};
