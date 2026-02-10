# Changelog

## [Unreleased] - 2026-02-10

### ✨ Added

#### ItemBuilder

- **`appendLore(String... lines)`** - Hängt Lore-Zeilen mit MiniMessage-Support an bestehende Lore an
- **`appendLore(Component... lines)`** - Hängt Lore-Zeilen als Adventure Components an bestehende Lore an
- **`setGenericId(String id)`** - Setzt die Generic-Item-ID in der PDC (für `GenericItem`). **Antwort:** Ja, speichert die ID als String im PersistentDataContainer unter dem Key "generic_id"
- **`setBoundOwner(UUID owner)`** - Setzt den Besitzer eines Bound-Items in der PDC (für `BoundItem`)
- **`genericIdKey()`** - Statische Methode für den PDC-Key "generic_id" (einheitliche Verwendung). **Antwort:** Ja, gibt `NamespacedKey` zurück, damit alle Klassen denselben Key verwenden (keine Duplikate)
- **`boundOwnerKey()`** - Statische Methode für den PDC-Key "bound_owner" (einheitliche Verwendung). **Antwort:** Ja, gibt `NamespacedKey` zurück, damit alle Klassen denselben Key verwenden (keine Duplikate)
- **`placeholder(Material material)`** - Statische Factory-Methode für Platzhalter-Items (leerer Name, z.B. für GUI-Füller). **Antwort:** Ja genau, erstellt einfach Items mit leerem Namen (`Component.empty()`) - perfekt für GUI-Füller, die nicht interagierbar sein sollen
- Konstanten **`PDC_GENERIC_ID`** und **`PDC_BOUND_OWNER`** für Key-Namen. **Antwort:** Ja, String-Konstanten mit den Werten "generic_id" und "bound_owner" - werden intern von den statischen Methoden verwendet

### 🔄 Changed

#### ItemBuilder

- Verbesserte Javadoc-Dokumentation (deutsch, einheitlich mit @param/@return)
- Erweiterte Klassen-Javadoc um Beschreibung der neuen Features

#### GenericItem

- **Konstruktor**: Verwendet jetzt `builder.setGenericId(id).build()` statt manueller PDC-Zugriffe. **Antwort:** Ja, vorher wurde `builder.pdc(key, PersistentDataType.STRING, id).build()` verwendet, jetzt einfach `builder.setGenericId(id).build()` - viel sauberer!
- **`getHeldId()`**: Nutzt jetzt `ItemBuilder.genericIdKey()` statt lokaler Key-Variable. **Antwort:** Ja, vorher hatte jede Instanz eine eigene `NamespacedKey key` Variable, jetzt wird die statische Methode verwendet - einheitlicher und weniger Speicher
- **GenericListener**: Beim ersten Binden eines Bound-Items wird jetzt `ItemBuilder` verwendet. **Antwort:** Ja, vorher wurde manuell `meta.getPersistentDataContainer().set(...)` und `meta.lore(...)` verwendet, jetzt alles über ItemBuilder - konsistenter Code

  ```java
  new ItemBuilder(item)
      .setBoundOwner(e.getPlayer().getUniqueId())
      .appendLore(Component.empty(), MessageHelper.get("item.bound_to_lore", ...))
      .build()
  ```
- Entfernte lokale `NamespacedKey` Variable (nutzt jetzt statische Methoden). **Antwort:** Ja, die Instanz-Variable `protected final NamespacedKey key` wurde entfernt, da jetzt `ItemBuilder.genericIdKey()` verwendet wird
- Vollständige Javadoc-Dokumentation hinzugefügt (Klasse + alle öffentlichen Methoden)
- `@NotNull` Annotationen hinzugefügt

#### BoundItem

- **`getItem(Player owner)`**: Komplett überarbeitet, verwendet jetzt nur noch `ItemBuilder`. **Antwort:** Ja, vorher wurde manuell `meta.getPersistentDataContainer().set(...)` und `meta.lore(...)` verwendet, jetzt alles über ItemBuilder - viel sauberer und konsistenter!

  ```java
  new ItemBuilder(super.getItem())
      .setBoundOwner(owner.getUniqueId())
      .appendLore(Component.empty(), MessageHelper.get("item.bound_to_lore", ...))
      .build()
  ```
- **BoundListener**: Nutzt jetzt `ItemBuilder.boundOwnerKey()` statt lokaler Key-Variable. **Antwort:** Ja, vorher wurde `new NamespacedKey(RedJuluLib.getPlugin(), "bound_owner")` lokal erstellt, jetzt wird die statische Methode verwendet - einheitlicher und weniger Code-Duplikation
- Entfernte `RedJuluLib` Import (nicht mehr benötigt)
- Vollständige Javadoc-Dokumentation hinzugefügt

#### BaseGUI

- **`fillBackground(Material)`**: Verwendet jetzt `ItemBuilder.placeholder(material).build()` statt manueller ItemBuilder-Erstellung
- **`fillContentArea(Material)`**: Verwendet jetzt `ItemBuilder.placeholder(material).build()` statt manueller ItemBuilder-Erstellung
- Alle Javadoc-Kommentare überarbeitet und verbessert (deutsch, einheitlich, vollständig)

### 🗑️ Removed

#### GenericItem

- Lokale `NamespacedKey key` Variable (ersetzt durch `ItemBuilder.genericIdKey()`)

#### BoundItem

- Lokale `NamespacedKey ownerKey` Variable (ersetzt durch `ItemBuilder.boundOwnerKey()`)
- Unnötiger `RedJuluLib` Import

### 📝 Documentation

- **ItemBuilder**: Alle neuen Methoden vollständig dokumentiert (deutsch)
- **GenericItem**: Vollständige Klassendokumentation + Methoden-Javadocs hinzugefügt
- **BoundItem**: Vollständige Klassendokumentation + Methoden-Javadocs hinzugefügt
- **BaseGUI**: Alle Methoden-Javadocs überarbeitet und verbessert
- Einheitliche Verwendung von `@param`, `@return` und `@NotNull` Annotationen

### 🎯 Improvements

- **Zentrale Item-Erstellung**: Alle Item-Manipulationen laufen jetzt über `ItemBuilder`
- **Konsistenz**: Einheitliche Verwendung von PDC-Keys über statische Methoden
- **Wartbarkeit**: Weniger Code-Duplikation, klarere Struktur
- **Type-Safety**: Bessere Nutzung von `@NotNull` Annotationen
