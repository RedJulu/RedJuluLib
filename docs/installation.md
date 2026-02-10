# Installation

## Maven Dependency

Füge RedJuluLib zu deinem `pom.xml` hinzu:

```xml
<repositories>
    <repository>
        <id>your-repo</id>
        <url>https://your-repo-url</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>de.redjulu</groupId>
        <artifactId>RedJuluLib</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Plugin Initialisierung

In deiner `onEnable()` Methode:

```java
@Override
public void onEnable() {
    // Initialisiere RedJuluLib
    RedJuluLib.init(this, "de"); // "de" = Standard-Sprache
    
    // Dein Code...
}
```

### Parameter

- `pluginInstance` - Deine JavaPlugin-Instanz
- `selectedLanguage` - Sprachcode (z.B. "de", "en", "fr")

## Was wird automatisch registriert?

Nach der Initialisierung werden automatisch registriert:

- ✅ GUIListener (für BaseGUI)
- ✅ BoundItem.BoundListener (für BoundItem-Schutz)
- ✅ GUIAnimationTask (für GUI-Animationen)
- ✅ MessageHelper (wird initialisiert)

## Nächste Schritte

- [Erste Schritte](getting-started.md) - Erstelle dein erstes Item oder GUI
- [ItemBuilder](item-builder/README.md) - Lerne Items zu erstellen
- [BaseGUI](gui/README.md) - Erstelle dein erstes GUI
