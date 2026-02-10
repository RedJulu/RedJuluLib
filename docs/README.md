# RedJuluLib

Eine moderne, umfassende Bibliothek für Minecraft Paper/Spigot Plugins (1.21+), die häufig benötigte Funktionalitäten bereitstellt.

## Features

- 🎨 **ItemBuilder** - Mächtiger Builder für ItemStacks mit modernen Features (Data Components, ItemModels, PDC)
- 🎯 **GenericItem** - Items mit fester ID, Cooldowns und Interaktions-Logik
- 🔒 **BoundItem** - Items, die an Spieler gebunden sind (Schutz vor Verlust, Diebstahl)
- 📦 **BaseGUI** - Flexibles GUI-System mit Pagination, Animationen und History
- 💬 **MessageHelper** - Einfache Kommunikation mit Spielern (Übersetzungen, Action Bars, Sounds)
- 🌍 **LanguageService** - Mehrsprachigkeit mit MiniMessage-Support
- ⚡ **BaseCommand** - Basis für Commands mit Permissions und Tab-Completion

## Voraussetzungen

- **Java 21+**
- **Paper/Spigot 1.21+**
- **Maven** (für Build)

## Schnellstart

```java
// In deiner Plugin onEnable():
RedJuluLib.init(this, "de"); // "de" = Sprache

// Fertig! Die Bibliothek ist initialisiert.
```

## Installation

Siehe [Installation](installation.md) für Details zur Maven-Integration.

## Dokumentation

- [Erste Schritte](getting-started.md) - Schnelleinstieg
- [ItemBuilder](item-builder/README.md) - Items erstellen und anpassen
- [GenericItem](generic-item/README.md) - Items mit ID und Cooldowns
- [BaseGUI](gui/README.md) - GUIs erstellen
- [API Referenz](api/redjulu-lib.md) - Vollständige API-Dokumentation

## Version

Aktuelle Version: **1.0-SNAPSHOT**

Siehe [Changelog](../CHANGELOG.md) für Änderungen.

## Support

Bei Fragen oder Problemen, erstelle ein Issue auf GitHub.

## Lizenz

ARR
