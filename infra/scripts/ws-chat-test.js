import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { Counter, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// 테스트 설정
const durationSec = 28;
const movieId = '8';
const vus = 5000;

export const options = {
    vus: vus,
    duration: `${durationSec}s`,
};

// 사용자 정의 메트릭
const messageSentCount = new Counter('message_sent');
const connectTime = new Trend('connect_time');
const disconnectTime = new Trend('disconnect_time');
const connectionDuration = new Trend('connection_duration');

const globalStartTime = Date.now();

// STOMP 프레임 생성 함수
function createStompFrame(command, headers = {}, body = '') {
    const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
    return [command, ...headerLines, '', `${body}\x00`].join('\n');
}

export default function () {
    const username = `user-${uuidv4().substring(0, 8)}`;
    const url = 'ws://host.docker.internal:8081/CineFinder-ws';
    const token = username; // 실제 환경에서는 JWT 또는 OAuth 토큰

    const startTime = Date.now();

    const res = ws.connect(url, {}, (socket) => {
        let intervalRef = null;
        let connectedAt = null;
        let isClosed = false;

        socket.on('open', () => {
            const openTime = Date.now();
            connectTime.add(openTime - startTime);
            console.log(`✅ 연결됨: ${username}`);
            if (!isClosed) {
                socket.send(createStompFrame('CONNECT', {
                    'accept-version': '1.2',
                    'heart-beat': '10000,10000',
                    'Authorization': `Bearer ${token}`,
                }));
            }
        });

        socket.on('message', (msg) => {
            if (msg.includes('CONNECTED')) {
                connectedAt = Date.now();

                if (!isClosed) {
                    // 구독
                    socket.send(createStompFrame('SUBSCRIBE', {
                        id: movieId,
                        destination: '/topic/chat',
                    }));

                    // 메시지 전송 반복
                    intervalRef = socket.setInterval(() => {
                        const messageBody = {
                            type: 'CHAT',
                            senderId: 'test-room',
                            movieId: movieId,
                            message: `k6 test message from ${username}`,
                        };

                        try {
                            if (!isClosed) {
                                socket.send(createStompFrame('SEND', {
                                    destination: `/app/chat-${movieId}`,
                                    'content-type': 'application/json',
                                    'content-length': JSON.stringify(messageBody).length,
                                }, JSON.stringify(messageBody)));

                                messageSentCount.add(1);
                            }
                        } catch (e) {
                            console.error(`❌ 메시지 전송 오류: ${e}`);
                        }
                    }, 1000);
                }
            }

            if (msg.includes('MESSAGE')) {
                check(msg, {
                    '✅ MESSAGE 수신': (m) => m.includes('MESSAGE'),
                });
            }
        });

        socket.on('error', (e) => {
            isClosed = true;
            console.error(`❌ WebSocket 오류: ${e.error}`);
        });

        socket.on('close', () => {
            const closedAt = Date.now();
            isClosed = true;

            disconnectTime.add(closedAt - globalStartTime);
            if (connectedAt) {
                connectionDuration.add(closedAt - connectedAt);
            }

            console.log(`🔌 연결 종료: ${username}`);
        });

        // 종료 예약
        const remainingTime = (globalStartTime + durationSec * 1000) - Date.now();
        const disconnectDelay = remainingTime > 0 ? remainingTime : 0;

        socket.setTimeout(() => {
            if (intervalRef) {
                socket.clearInterval(intervalRef);
            }

            try {
                if (!isClosed) {
                    socket.send(createStompFrame('DISCONNECT'));
                }
            } catch (e) {
                console.error(`❌ DISCONNECT 전송 실패: ${e}`);
            }

            // 200ms 대기 후 연결 종료
            socket.setTimeout(() => {
                if (!isClosed) {
                    try {
                        socket.close();
                    } catch (e) {
                        console.error(`❌ socket.close() 오류: ${e}`);
                    }
                }
            }, 200);
        }, disconnectDelay);
    });

    // 연결 결과에 대한 check (res가 없을 경우 방지)
    if (res && res.status !== undefined) {
        check(res, {
            '🟢 WebSocket 연결 성공 (101)': (r) => r.status === 101,
        });
        if (res.status !== 101) {
            console.warn(`⚠️ WebSocket 연결 실패: ${username}, 상태 코드: ${res.status}`);
        }
    } else {
        console.warn(`⚠️ 연결 실패 또는 응답 없음: ${username}`);
    }

    sleep(1);
}

// 요약 결과 출력
export function handleSummary(data) {
    return {
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
