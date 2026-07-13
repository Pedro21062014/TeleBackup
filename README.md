# TeleBackup

App Android moderno e minimalista para fazer **backup de fotos e vídeos** para um **grupo/chat do Telegram** usando um bot.

![Android](https://img.shields.io/badge/Android-8.0%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple)
![Version](https://img.shields.io/badge/version-1.2.0-orange)

## Download

Baixe o APK mais recente na página de **[Releases](../../releases)**.

| Versão | Arquivo |
|--------|---------|
| **1.2.0** (atual) | `TeleBackup-v1.2.0-signed.apk` |

> Ative **Fontes desconhecidas** / instalar apps desconhecidos no Android para instalar o APK.

## Funcionalidades

- **Configuração do bot** — token do @BotFather + Group/Chat ID
- **Teste de conexão** — valida o bot e envia mensagem de confirmação
- **Galeria local** — lê mídia de `/storage/emulated/0` (DCIM, Pictures, Download, Movies…)
- **Fotos e vídeos** — filtros Tudo / Fotos / Vídeos
- **Visualizador** — fullscreen, arraste lateral, pinch/duplo toque para zoom, player de vídeo
- **Pastas** — seletor nativo (SAF) com permissão persistente
- **Galeria na nuvem** — histórico do que o app já enviou ao Telegram
- **Metadados** — manter original, remover GPS/câmera/EXIF; legenda com data, pasta, localização, etc.
- **Backup** — progresso em tempo real + rate-limit suave da Bot API
- **UI dark minimalista** — Material 3 + Jetpack Compose

## Como usar

1. Abra o **@BotFather** no Telegram → `/newbot` → copie o **token**
2. Crie (ou use) um grupo, adicione o bot e torne-o **admin** (enviar mídia)
3. Descubra o **Group ID** (ex.: `@userinfobot`, `@getidsbot`)
4. No app → aba **Config** → cole token e Group ID → **Testar conexão**
5. Ajuste **Metadados no envio** (GPS, EXIF, legenda) se quiser
6. Aba **Galeria** → toque para visualizar · segure para selecionar
7. Aba **Backup** → **Iniciar backup**
8. Itens enviados aparecem na aba **Nuvem**

### Gestos no visualizador

| Gesto | Ação |
|-------|------|
| Toque | Mostra/esconde barras |
| Arraste horizontal | Próxima / anterior mídia |
| Botões ‹ › | Navegar |
| Duplo toque | Zoom 2.5× |
| Pinch | Zoom livre |
| Toque (com zoom) | Volta ao 100% |

## Requisitos

- Android **8.0+** (API 26)
- Internet
- Permissão de fotos e vídeos

## Stack

- Kotlin · Jetpack Compose · Material 3
- OkHttp (Telegram Bot API)
- Coil (imagens + frames de vídeo)
- DataStore (preferências)
- ExifInterface (metadados)
- ViewModel · Coroutines

## Build

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk

./gradlew assembleDebug
```

APK gerado em:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Privacidade

- Token, pastas e opções ficam **somente no dispositivo**
- Mídias vão apenas para o **chat/grupo** que você configurar
- Você pode **remover GPS/EXIF** antes do upload
- Nenhum servidor intermediário além da API oficial do Telegram

## Estrutura do projeto

```
TelegramBackup/
├── app/src/main/java/com/telebackup/app/
│   ├── data/          # MediaStore, Telegram API, preferências, metadados
│   ├── ui/screens/    # Galeria, Nuvem, Pastas, Backup, Config, Viewer
│   ├── ui/theme/      # Cores e tipografia
│   └── util/          # Coil / performance
├── dist/              # APKs de release (não versionados no git)
└── README.md
```

## Changelog

### 1.2.0
- Scan completo de `/storage/emulated/0` via MediaStore
- Performance: thumbnails Coil, cache, vídeos offscreen leves
- Swipe do visualizador corrigido (não conflita com zoom)
- Botões de navegação ‹ › e dica “arraste ← →”

### 1.1.0
- Upload de vídeos (`sendVideo`)
- Aba Galeria na nuvem
- Opções de metadados (GPS, EXIF, legenda)
- Toque para visualizar · segure para selecionar

### 1.0.0
- Primeira versão: config bot, pastas, galeria, backup, visualizador

## Licença

Uso pessoal / educacional. Telegram é marca da Telegram FZ-LLC.
