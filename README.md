# NotSoSimple Voice Chat

Fabric-Addon für [Simple Voice Chat](https://modrinth.com/mod/simple-voice-chat) (Minecraft 26.3): **lokale** Mikrofon-Rauschunterdrückung ohne Cloud.

## Engines

| Engine | Technik | Standard |
|--------|---------|----------|
| **DeepFilterNet3** | ONNX @ 48 kHz, 10 ms-Hops, stateful Streaming | Ja |
| **RNNoise** | JNI (wie SVC) | Fallback |

Beim ersten Start mit **DeepFilterNet** wird das ONNX-Modell (~16 MB) nach  
`config/notsosimplevoicechat/denoiser_model.onnx` geladen (SHA-256-geprüft).  
Modell-Quelle: [SpeechDenoiser](https://github.com/yuyun2000/SpeechDenoiser) / [DeepFilterNet](https://github.com/Rikorose/DeepFilterNet) (MIT/Apache-2.0).

## Einstellungen

**Im Spiel:** `V` → **Einstellungen** (Simple Voice Chat) → **„Rauschunterdrückung…“**

- Verbesserung aktiv  
- SVC-Rauschfilter ersetzen (empfohlen)  
- Engine: DeepFilterNet3 oder RNNoise  
- Filterstärke 0–100 %

## Entwicklung

Optional Modell für Offline-Dev nach `run/models/denoiser_model.onnx` legen (wird sonst automatisch in `config/` kopiert/heruntergeladen).

```bash
./gradlew runClient
```

## Lizenz

MIT (siehe `LICENSE`). ONNX-Modell und DeepFilterNet unter deren Lizenzen.
