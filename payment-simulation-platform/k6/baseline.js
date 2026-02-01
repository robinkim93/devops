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
    const payload = JSON.stringify({
        email: 'test@test.com', 
        password: 'testtest'
    });
    
    const params = {
        headers: {'Content-Type': 'application/json'}
    };
    
    const res = http.post('http://localhost:30081/api/auth/login', payload, params);
    
    // 디버깅용 로그
    if (res.status !== 200) {
        console.log(`Status: ${res.status}, Body: ${res.body}`);
    }
}