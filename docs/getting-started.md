# Erste Schritte

Willkommen bei RedJuluLib! Diese Anleitung zeigt dir die ersten Schritte.

## 1. Installation

Siehe [Installation](installation.md) für Details.

## 2. Einfaches Item erstellen

```java
import de.redjulu.lib.ItemBuilder;
import org.bukkit.Material;

// Einfaches Item mit Namen und Lore
ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
    .setName("<gold>Magisches Schwert")
    .setLore(
        "<gray>Ein mächtiges Schwert",
        "<gray>mit magischen Kräften"
    )
    .build();
```

## 3. Generic Item erstellen

```java
import de.redjulu.lib.ItemBuilder;
import de.redjulu.lib.item.GenericItem;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;

public class MyMagicSword extends GenericItem {
    
    public MyMagicSword() {
        super(
            "magic_sword",                    // ID
            "weapons",                        // Kategorie
            new ItemBuilder(Material.DIAMOND_SWORD)
                .setName("<gold>Magisches Schwert")
                .setLore("<gray>Klicke, um zu verwenden")
        );
    }
    
    @Override
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        player.sendMessage("Du hast das magische Schwert verwendet!");
    }
}
```

## 4. Einfaches GUI erstellen

```java
import de.redjulu.lib.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MyGUI extends BaseGUI<String, MyCategory> {
    
    public MyGUI() {
        super(
            6,                    // 6 Reihen
            "gui.my_title",        // Titel-Key
            1, 1, 1, 1,           // top, bottom, left, right
            MyCategory.ALL        // Standard-Kategorie
        );
        
        // Items hinzufügen
        allItems.add("Item 1");
        allItems.add("Item 2");
    }
    
    @Override
    public void compose(Player player) {
        // Hintergrund füllen
        fillBackground(Material.GRAY_STAINED_GLASS_PANE);
        
        // Content rendern
        renderPage(
            item -> true,  // Filter (alle anzeigen)
            (item, slot) -> {
                ItemStack display = new ItemBuilder(Material.DIAMOND)
                    .setName("<gold>" + item)
                    .build();
                inventory.setItem(slot, display);
            }
        );
    }
}
```

## 5. Nachricht senden

```java
import de.redjulu.lib.MessageHelper;

// Einfache Nachricht
MessageHelper.send(player, "system.welcome");

// Mit Placeholdern
MessageHelper.send(player, "system.greeting", 
    "player", player.getName(),
    "level", 42
);

// Action Bar
MessageHelper.sendActionBar(player, "system.action", "value", 100);
```

## Nächste Schritte

- [ItemBuilder Dokumentation](item-builder/README.md) - Lerne alle ItemBuilder-Features
- [GenericItem Dokumentation](generic-item/README.md) - Erstelle interaktive Items
- [BaseGUI Dokumentation](gui/README.md) - Baue komplexe GUIs
- [MessageHelper Dokumentation](message-helper/README.md) - Kommunikation mit Spielern
