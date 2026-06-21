# Lista Smart — App Android (Java)

Cliente Android que lê QR Code de cupom fiscal (NFC-e) com CameraX + ML Kit, faz
cadastro manual de preços e consome a API REST `lista-smart-api`. O backend é a
fonte da verdade; o SQLite local é cache de leitura (catálogo e histórico) e fila
de envio offline (outbox).

## Stack
Java 17 · minSdk 24 / targetSdk 34 · Gradle (Groovy) · Layouts XML · Retrofit + Gson ·
SQLite (`SQLiteOpenHelper`, escrita transacional) · EncryptedSharedPreferences
(Android Keystore) · RecyclerView · CameraX + ML Kit Barcode

## Estrutura
```
com.listasmart.cupons
├── activities/   LoginActivity, MainActivity (abas Escanear/Manual/Perfil),
│                 ScannerActivity, RankingActivity, HistoryActivity
├── adapters/     LeaderboardAdapter, HistoryAdapter (RecyclerView)
├── database/     DBHelper (cache SQLite + outbox; gravações em transação)
├── network/      ApiClient (Retrofit + interceptor JWT), ApiService, *Request/*Response
├── helpers/      SessionManager (token cifrado/Keystore), DateHelper
└── models/       Contribution, Product, Market, LeaderboardUser, UserMe, OutboxEntry
```

## Build / Run
1. Abrir no Android Studio e sincronizar o Gradle.
2. Definir a URL do backend em `local.properties` (não versionado):
   ```
   API_BASE_URL=https://lista-smart-api.onrender.com/   # produção
   # API_BASE_URL=http://10.0.2.2:8080/                 # backend local (emulador)
   ```
   O valor é exposto como `BuildConfig.BASE_URL` e lido pelo `ApiClient`.
3. Executar em emulador/dispositivo (API 24+). A câmera exige dispositivo físico
   ou webcam no emulador.

## Funcionamento
- **Autenticação:** `/auth/login|register` retornam o JWT, persistido cifrado no
  `SessionManager` e enviado como `Authorization: Bearer <token>` em toda chamada.
- **Catálogo:** produtos/mercados vêm da API e são cacheados no SQLite (spinners).
- **Escrita (contribuição):** QR ou manual → enfileira na outbox com `clientUuid` →
  `POST /contributions`. Em sucesso sai da fila; sem rede, permanece para reenvio.
- **Leitura (perfil/ranking/histórico):** buscados da API; o histórico espelha no
  cache SQLite para exibição offline.
- **Sincronização:** `flushOutbox()` reenvia pendências na abertura e no
  pull-to-refresh; o `clientUuid` garante idempotência (o servidor deduplica).

## Telas
- **Login** — autenticação / registro.
- **Main** — abas Escanear (dispara o scanner), Manual (formulário validado) e
  Perfil (pontos, nível/selo, ranking e histórico).
- **Scanner** — CameraX + ML Kit; análise de frames em thread de background.
- **Ranking / History** — listas completas (RecyclerView); History edita/exclui.

## Limitações
- Contribuição na fila offline só aparece no histórico após sincronizar.
- O parser de QR usa o formato simplificado do projeto; a resolução de itens da
  NFC-e é feita no backend (mock plugável).
