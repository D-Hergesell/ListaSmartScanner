# Lista Smart — App Android (Java)

Leitura de cupom fiscal (QR) e cadastro colaborativo de preços, com pontuação,
ranking e selo de confiabilidade. Consome a **API Lista Smart** (Spring Boot +
NeonDB): o backend é a fonte da verdade e o SQLite funciona como cache offline.

## Stack
Java 17 · Layouts XML · Gradle (Groovy) · Retrofit · SQLite · SharedPreferences · CameraX + ML Kit (QR)

## Rodar
1. Abra a pasta no **Android Studio** e aguarde o Gradle sincronizar.
2. Defina a URL do backend em `local.properties` (não versionado):
   ```
   API_BASE_URL=https://lista-smart-api.onrender.com/   # produção
   # API_BASE_URL=http://10.0.2.2:8080/                 # backend local (emulador)
   ```
3. Execute em emulador/dispositivo (API 24+). Câmera real exige dispositivo físico/webcam.

## Arquitetura
```
com.listasmart.cupons
├── activities/   Login, Main (3 abas), Scanner
├── adapters/     Leaderboard, History (ListView)
├── database/     DBHelper (cache SQLite + fila offline)
├── network/      ApiClient (Retrofit + JWT), ApiService
├── helpers/      Session, Gamification, Date
└── models/       Contribution, Product, Market, LeaderboardUser, UserMe, OutboxEntry
```

## Sincronização (online + offline)
- **Auth:** login/registro no backend guardam o **JWT** (`SessionManager`), enviado em todas as chamadas.
- **Escrita:** criar contribuição (QR/manual) envia ao backend; perfil, ranking e histórico vêm da nuvem (cache local para leitura offline).
- **Fila offline (outbox):** sem rede, a contribuição é guardada e reenviada ao reconectar.
- **Idempotência:** cada envio leva um `clientUuid`; reenvios nunca duplicam (o servidor deduplica).

## Limitações
- Contribuição ainda na fila offline só aparece no histórico após sincronizar.
- O parser de QR usa o formato simplificado do projeto; NFC-e real cai em item único.
