# ColorVoice (Fabric, Minecraft 26.2)

Sagst du im Simple Voice Chat eine Farbe (Deutsch oder Englisch), werden alle Blöcke im Umkreis von 100 m
(Kugel um dich) zu Wolle dieser Farbe. Luft und Blöcke mit Inhalt (Truhen, Öfen ...) bleiben unverändert.

## Bauen
1. JDK 25 installieren.
2. In gradle.properties `fabric_api_version` (und ggf. loader/loom) von https://fabricmc.net/develop für 26.2 eintragen.
3. `gradle wrapper` (einmalig) und dann `./gradlew build`.
4. Die Datei `build/libs/colorvoice-1.0.0.jar` kommt in den `mods`-Ordner des **Servers** (bzw. deiner Welt, die du für Freunde öffnest).

## Benötigt
- Fabric Loader, Fabric API, Simple Voice Chat (Server + Clients)
- Vosk-Sprachmodelle (kostenlos, offline): https://alphacephei.com/vosk/models
  - `vosk-model-small-de-0.15` und `vosk-model-small-en-us-0.15` entpacken nach `config/colorvoice/`

## Config: `config/colorvoice.json`
radius, cooldownSeconds, allowedPlayers (leer = alle dürfen), Modellpfade.

## Farben ändern
In `ColorBlocks.java`, z.B. `Blocks.NETHERRACK` statt `Blocks.RED_WOOL`.
