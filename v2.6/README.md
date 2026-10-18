## 📊 Proof of Concept (Live Test)

Here is a real output from `stealth_log.csv` and `stealth_log.json` captured during a live test on a local network (Telkomsel Hotspot). The tool successfully detected a bot, engaged the tarpit, and blocked multiple ports.

### CSV Output (`stealth_log.csv`)

```csv
timestamp,event,ip,port,details
2026-10-18 21:17:39,CONN,10.52.***.***,8080,"New connection"
2026-10-18 21:17:39,BOT,10.52.***.***,8080,"Bot signature detected"
2026-10-18 21:17:39,TARPIT,10.52.***.***,8080,"Tarpit engaged"
2026-10-18 21:22:27,CONN,10.52.***.***,3306,"New connection"
2026-10-18 21:22:27,TARPIT,10.52.***.***,3306,"Tarpit engaged"
2026-10-18 21:22:27,CONN,10.52.***.***,2121,"New connection"
2026-10-18 21:22:27,TARPIT,10.52.***.***,2121,"Tarpit engaged"
```

JSON Output (stealth_log.json)

```json
[
  {"timestamp":"2026-10-18 21:17:39","event":"BOT","ip":"10.52.***.***","port":8080,"details":"Bot signature detected"},
  {"timestamp":"2026-10-18 21:17:39","event":"TARPIT","ip":"10.52.***.***","port":8080,"details":"Tarpit engaged"},
  {"timestamp":"2026-10-18 21:22:27","event":"TARPIT","ip":"10.52.***.***","port":3306,"details":"Tarpit engaged"}
]
```

Note: IP addresses are partially redacted for privacy and ethical disclosure.

```
