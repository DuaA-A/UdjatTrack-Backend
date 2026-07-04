const { Client } = require('@stomp/stompjs');
const ObjectWs = require('ws');

// USE YOUR CLOUD WEBSOCKET URL (wss:// for secure HTTPS clouds)
const WS_URL = 'wss://udjattrack-backend-production-c744.up.railway.app/ws';
const FLEET_ID = '722f7701-1230-4129-a2e3-fd3dce89afbd';
const TOKEN = 'eyJhbGciOiJIUzM4NCJ9.eyJyb2xlIjoiUk9MRV9GTEVFVF9NQU5BR0VSIiwibmFtZSI6IkZsZWV0IEEiLCJ1c2VySWQiOiI3MjJmNzcwMS0xMjMwLTQxMjktYTJlMy1mZDNkY2U4OWFmYmQiLCJzdWIiOiJhbGkuZXNzYW0uYWJkZWxoYWxlZW0uMjBAZ21haWwuY29tIiwiaWF0IjoxNzgzMTA4MDA1LCJleHAiOjE3ODMxOTQ0MDV9.9IkGqjJEVxvzhB4It-BN3wTdeUUWaChgYKIezIONyp6rpRFfcAzPPgd_DGzBAHYL';

const client = new Client({
  brokerURL: WS_URL,
  connectHeaders: { Authorization: `Bearer ${TOKEN}` },
  webSocketFactory: () => new ObjectWs(WS_URL),
  onConnect: () => {
    console.log('Connected to Cloud STOMP Broker.');
    
    // Subscribe to the alert topic
    client.subscribe(`/topic/fleet/${FLEET_ID}/alerts`, (message) => {
      const receiveTime = Date.now();
      const payload = JSON.parse(message.body);
      const sendTime = payload.timestamp ? new Date(payload.timestamp).getTime() : receiveTime;
      
      const latency = receiveTime - sendTime;
      console.log(`[STOMP ALERT RECEIVED] End-to-End Cloud Latency: ${latency} ms`);
      process.exit(0);
    });

    console.log('Subscribed. Waiting for alert broadcast...');
  },
});

client.activate();
