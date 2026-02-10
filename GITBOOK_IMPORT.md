# GitBook Import Anleitung

Diese Datei erklärt, wie du die RedJuluLib Dokumentation in GitBook importierst.

## Option 1: GitBook.com (Online)

1. Gehe zu [GitBook.com](https://www.gitbook.com)
2. Erstelle ein neues Space oder öffne ein bestehendes
3. Gehe zu **Settings** → **Integrations**
4. Verbinde dein GitHub Repository (falls vorhanden)
5. Oder nutze **Import** → **From GitHub** und wähle dieses Repository
6. GitBook erkennt automatisch die `docs/` Struktur

## Option 2: GitBook CLI (Lokal)

```bash
# Installiere GitBook CLI
npm install -g gitbook-cli

# Navigiere zum docs Ordner
cd docs

# Initialisiere GitBook
gitbook init

# Die SUMMARY.md wird automatisch verwendet

# Starte lokalen Server
gitbook serve

# Oder baue statische HTML-Dateien
gitbook build
```

## Option 3: Direkter Import der Struktur

1. Kopiere den gesamten `docs/` Ordner
2. In GitBook: **Settings** → **Import** → **Upload ZIP**
3. Lade die `docs/` Ordner als ZIP hoch
4. GitBook erstellt automatisch die Struktur basierend auf `SUMMARY.md`

## Wichtige Dateien

- **SUMMARY.md** - Hauptnavigation (wird automatisch erkannt)
- **README.md** - Startseite der Dokumentation
- **book.json** oder **.gitbook.yaml** - Konfiguration (optional)

## Struktur

Die Dokumentation ist in folgende Bereiche unterteilt:

```
docs/
├── SUMMARY.md              ← GitBook Navigation
├── README.md               ← Startseite
├── installation.md
├── getting-started.md
├── item-builder/           ← ItemBuilder Module
├── generic-item/           ← GenericItem Module
├── bound-item/             ← BoundItem Module
├── gui/                    ← BaseGUI Module
├── message-helper/         ← MessageHelper Module
├── language-service/       ← LanguageService Module
├── commands/                ← BaseCommand Module
├── advanced/               ← Erweiterte Themen
├── api/                    ← API Referenz
├── contributing.md
└── license.md
```

## Nächste Schritte nach Import

1. ✅ Überprüfe, ob alle Kapitel in der Navigation erscheinen
2. ✅ Fülle die leeren Kapitel mit Inhalten
3. ✅ Passe das Design in `book.json` an
4. ✅ Veröffentliche die Dokumentation

## Unterstützung

Bei Problemen beim Import:
- Stelle sicher, dass `SUMMARY.md` im `docs/` Ordner liegt
- Überprüfe die Pfade in `SUMMARY.md` (relativ zu `docs/`)
- Nutze GitBook's Preview-Funktion zum Testen
