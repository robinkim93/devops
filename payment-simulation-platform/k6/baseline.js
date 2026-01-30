import http from 'k6/http'
import { sleep } from 'k6'

export const options = {
    vus: 150,
    duration: '5m',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01']
    }
};

export default function () {
    http.post('http://localhost:8080/api/auth/login', {body: JSON.stringify({email: 'test@test.com', password: 'testtest'})}, {headers: {'Content-Type': 'application/json'}});
}