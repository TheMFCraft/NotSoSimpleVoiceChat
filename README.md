# NotSoSimple Voice Chat

Fabric-Addon für [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) (Minecraft 26.3), das die Rauschunterdrückung über **Krisp Voice Isolation** (Streaming-API) verbessert.

## Voraussetzungen

- Simple Voice Chat 2.6.27+ für 26.3
- Krisp Developers Portal API-Key mit **Voice Isolation (`viva`)**-Produkt

## Konfiguration

Datei: `config/notsosimplevoicechat.json` (wird beim ersten Start angelegt)

| Feld | Beschreibung |
|------|----------------|
| `krispApiKey` | Master API-Key (optional, wenn `krispSessionToken` gesetzt) |
| `krispSessionToken` | Fertiger Session-Token (Alternative zum API-Key) |
| `enabled` | Krisp-Verarbeitung ein/aus |
| `suppressionLevel` | 0–100 (Krisp `suppression_level`) |
| `model` | Standard: `VIVA-tel` |

**Empfehlung:** RNNoise in den Simple-Voice-Chat-Einstellungen deaktivieren, wenn Krisp aktiv ist (sonst doppelte Filterung).

Umgebungsvariable `KRISP_API_KEY` überschreibt `krispApiKey` in der Config.

## Entwicklung

```bash
./gradlew runClient
```

## Lizenz

MIT
