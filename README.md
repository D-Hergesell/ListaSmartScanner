# Lista Smart — Scanner de Cupom e Colaboração (Android / Java)

MVP da **Opção 2** do case da Lista Smart: leitura de cupom fiscal (QR Code),
cadastro colaborativo de preços, validação, pontuação, ranking, selo de
confiabilidade e histórico de contribuições.

Desenvolvido em **Java**, com **layouts XML (View system tradicional)**,
**Gradle Groovy DSL**, **SQLite**, **Retrofit** e **SharedPreferences**,
seguindo o conteúdo do curso.

---

## Como abrir e rodar

1. Abra a pasta `ListaSmart` no **Android Studio** (`File > Open`).
2. Aguarde o Gradle sincronizar (baixa as dependências automaticamente).
3. Configure a MockAPI (ver seção abaixo) ou rode direto — há fallback local.
4. Execute em um emulador/dispositivo (API 24+). A câmera real exige
   dispositivo físico ou emulador com webcam.

---

## Arquitetura (visão geral)

Organização **clássica do Android por responsabilidades** (estilo MVC simples,
onde a Activity atua como Controller, os layouts XML como View e os
Models/DB/Network como camada de dados):

```
com.listasmart.cupons
├── activities/      → telas (Controllers)
│   ├── LoginActivity      (cadastro/login simulado)
│   ├── MainActivity       (3 abas + navegação + regras)
│   └── ScannerActivity    (câmera + leitura de QR)
├── adapters/        → adapters de ListView
│   ├── LeaderboardAdapter
│   └── HistoryAdapter
├── database/        → persistência local
│   └── DBHelper           (SQLite + CRUD completo)
├── network/         → consumo de API REST
│   ├── ApiClient          (Retrofit singleton)
│   └── ApiService         (endpoints MockAPI)
├── helpers/         → utilidades
│   ├── SessionManager     (SharedPreferences / sessão)
│   ├── GamificationHelper (selo e progresso)
│   └── DateHelper         (datas)
└── models/          → entidades de dados
    ├── Contribution, Product, Market, LeaderboardUser
```

### Fluxo de dados (Retrofit + SQLite)

- **Retrofit** consome a **MockAPI** para buscar o catálogo (`products`,
  `markets`) e o `leaderboard`. Na **primeira carga**, os dados são gravados
  no **SQLite** (`DBHelper.replaceProducts/replaceMarkets`).
- A partir daí, o app lê o catálogo **direto do SQLite**, funcionando offline.
- Se a API estiver indisponível, há um **catálogo padrão (fallback)** gravado
  localmente, sem quebrar o app.
- As **contribuições** do usuário (QR e manual) são a entidade principal e
  ficam **sempre no SQLite** (CREATE/READ/UPDATE/DELETE em `DBHelper`).

### Sessão (SharedPreferences)

`SessionManager` guarda nome e estado de login. Se já houver sessão,
`LoginActivity` redireciona direto para a `MainActivity`. O nome identifica o
usuário no ranking e no avatar.

---

## Mapeamento Figma → Activities / Layouts

| Tela do Figma            | Layout XML            | Activity / Componente           |
|--------------------------|-----------------------|---------------------------------|
| (entrada simulada)       | `activity_login.xml`  | `LoginActivity`                 |
| Estrutura geral + nav    | `activity_main.xml`   | `MainActivity`                  |
| Aba **Escanear**         | `content_scan.xml`    | `MainActivity` (aba 0)          |
| Câmera de leitura de QR  | `activity_scanner.xml`| `ScannerActivity` (ML Kit)      |
| Aba **Manual**           | `content_manual.xml`  | `MainActivity` (aba 1)          |
| Aba **Perfil**           | `content_profile.xml` | `MainActivity` (aba 2)          |
| Item do ranking          | `item_leaderboard.xml`| `LeaderboardAdapter`            |
| Item do histórico        | `item_history.xml`    | `HistoryAdapter`                |

As três abas seguem o protótipo: header "Leitor de Cupons", conteúdo central e
bottom navigation (Escanear / Manual / Perfil), com a aba ativa destacada em
índigo.

---

## Recursos do curso utilizados

- **Activities e Intents**: navegação Login → Main; Main → Scanner
  (`registerForActivityResult` devolvendo o QR lido).
- **Layouts XML / View system**: todas as telas em XML tradicional.
- **Animações básicas**: `fade_in` nos alertas de sucesso.
- **SharedPreferences**: sessão/login em `SessionManager`.
- **Validação + AlertDialog**: formulário manual e login.
- **ListView + Adapters**: ranking e histórico.
- **SQLite**: `DBHelper` com CRUD completo.
- **Retrofit**: `ApiClient` + `ApiService` consumindo a MockAPI.

---

## Configurando a MockAPI (opcional)

1. Crie um projeto em https://mockapi.io.
2. Crie os recursos `products`, `markets` e `leaderboard`.
   - `products` / `markets`: campos `id` (String) e `name` (String).
   - `leaderboard`: `name`, `points` (int), `contributions` (int), `avatar`.
3. Copie a URL base do projeto e substitua em
   `network/ApiClient.java` → `BASE_URL`.

> Sem essa configuração, o app usa o catálogo e o ranking padrão locais.

---

## Limitações conhecidas

- O ranking de concorrentes é ilustrativo (MockAPI ou fallback); não há
  servidor real agregando todos os usuários.
- A leitura de QR aceita qualquer conteúdo de QR Code; a validação aprofundada
  do layout de NFC-e não faz parte do escopo do MVP.
- Edição/exclusão de contribuições existem na camada `DBHelper`
  (`updateContribution`, `deleteContribution`) prontas para uso, mas a UI do
  MVP foca em criação e listagem, conforme o protótipo.
