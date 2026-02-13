package de.redjulu.lib;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.redjulu.RedJuluLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Ultimativer ItemBuilder für Paper 1.21.1+.
 * Unterstützt moderne Data Components, ItemModels, persistente Metadaten,
 * Generic-Items (ID), Bound-Items (Besitzer) und Lore-Anpassungen.
 */
public class ItemBuilder {

    /** PDC-Key für die Generic-Item-ID. */
    public static final String PDC_GENERIC_ID = "generic_id";
    /** PDC-Key für den Besitzer eines Bound-Items. */
    public static final String PDC_BOUND_OWNER = "bound_owner";

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private final MiniMessage mm = MiniMessage.miniMessage();

    /**
     * Erstellt einen neuen ItemBuilder.
     * @param material Das Material des Items.
     */
    public ItemBuilder(@NotNull Material material) {
        this(material, 1);
    }

    /**
     * Erstellt einen neuen ItemBuilder mit einer bestimmten Anzahl.
     * @param material Das Material des Items.
     * @param amount Die Anzahl der Items.
     */
    public ItemBuilder(@NotNull Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * Erstellt einen ItemBuilder basierend auf einem existierenden ItemStack.
     * @param item Das zu kopierende Item.
     */
    public ItemBuilder(@NotNull ItemStack item) {
        this.itemStack = item.clone();
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * Setzt den Anzeigenamen via MiniMessage-String.
     * Deaktiviert standardmäßig das Minecraft-Kursiv.
     * @param name Der Name als String.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setName(@Nullable String name) {
        if (name != null) {
            itemMeta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return this;
    }

    /**
     * Setzt den Anzeigenamen via Adventure Component.
     * Deaktiviert standardmäßig das Minecraft-Kursiv.
     * @param component Die Component.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setName(@Nullable Component component) {
        if (component != null) {
            itemMeta.displayName(component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return this;
    }

    /**
     * Setzt die Lore via MiniMessage-Strings.
     * @param lore Die Lore-Zeilen als Strings.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setLore(@NotNull String... lore) {
        itemMeta.lore(Arrays.stream(lore)
                .map(line -> mm.deserialize(line).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList());
        return this;
    }

    /**
     * Setzt die Lore via Adventure Components.
     * @param lore Die Lore-Zeilen als Components.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setLore(@NotNull Component... lore) {
        itemMeta.lore(Arrays.stream(lore)
                .map(line -> line.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList());
        return this;
    }

    /**
     * Setzt die Lore via einer Liste von Components.
     * @param lore Die Liste der Components.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setLore(@Nullable List<Component> lore) {
        if (lore != null) {
            itemMeta.lore(lore.stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .toList());
        }
        return this;
    }

    /**
     * Hängt MiniMessage-Zeilen an die bestehende Lore an.
     * @param lines Zusätzliche Zeilen.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder appendLore(@NotNull String... lines) {
        List<Component> current = itemMeta.hasLore() && itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        for (String line : lines) {
            current.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        itemMeta.lore(current);
        return this;
    }

    /**
     * Hängt Components an die bestehende Lore an.
     * @param lines Zusätzliche Components.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder appendLore(@NotNull Component... lines) {
        List<Component> current = itemMeta.hasLore() && itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        for (Component line : lines) {
            current.add(line.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        itemMeta.lore(current);
        return this;
    }

    /**
     * Setzt die Stapelgröße des Items.
     * @param amount Menge.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * Setzt die CustomModelData.
     * @param data ID.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setCustomModelData(@Nullable Integer data) {
        itemMeta.setCustomModelData(data);
        return this;
    }

    /**
     * Setzt das ItemModel (Minecraft 1.21+).
     * @param key NamespacedKey des Models.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setItemModel(@Nullable NamespacedKey key) {
        itemMeta.setItemModel(key);
        return this;
    }

    /**
     * Setzt das ItemModel via Namespace und Key.
     * @param namespace Namespace.
     * @param key Key.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setItemModel(@NotNull String namespace, @NotNull String key) {
        return setItemModel(new NamespacedKey(namespace, key));
    }

    /**
     * Setzt die Unzerstörbarkeit des Items.
     * @param unbreakable True für unzerstörbar.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        itemMeta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Erzwingt oder entfernt das Verzauberungs-Leuchten.
     * @param override True für Leuchten.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setEnchantmentGlintOverride(boolean override) {
        itemMeta.setEnchantmentGlintOverride(override);
        return this;
    }

    /**
     * Setzt die Feuerfestigkeit (Netherite-Verhalten).
     * @param fireResistant True für feuerfest.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setFireResistant(boolean fireResistant) {
        itemMeta.setFireResistant(fireResistant);
        return this;
    }

    /**
     * Setzt die maximale Stack-Größe.
     * @param size Größe (1-99).
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setMaxStackSize(@Nullable Integer size) {
        itemMeta.setMaxStackSize(size);
        return this;
    }

    /**
     * Setzt die Seltenheit (Farbe des Namens).
     * @param rarity Seltenheitsgrad.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setRarity(@NotNull ItemRarity rarity) {
        itemMeta.setRarity(rarity);
        return this;
    }

    /**
     * Versteckt den kompletten Tooltip des Items.
     * @param hide True zum Verstecken.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setHideTooltip(boolean hide) {
        itemMeta.setHideTooltip(hide);
        return this;
    }

    /**
     * Fügt eine Verzauberung hinzu.
     * @param enchant Verzauberung.
     * @param level Level.
     * @param ignoreRestriction Ob Limits ignoriert werden.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder addEnchant(@NotNull Enchantment enchant, int level, boolean ignoreRestriction) {
        itemMeta.addEnchant(enchant, level, ignoreRestriction);
        return this;
    }

    /**
     * Fügt ItemFlags hinzu.
     * @param flags Die Flags.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder addItemFlags(@NotNull ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }

    /**
     * Versteckt alle Standard-Informationen (Enchants, Attribute, etc.).
     * @return Der aktuelle Builder.
     */
    public ItemBuilder hideAll() {
        itemMeta.addItemFlags(ItemFlag.values());
        return this;
    }

    /**
     * Speichert persistente Daten auf dem Item.
     * @param key Key.
     * @param type Datentyp.
     * @param value Wert.
     * @return Der aktuelle Builder.
     */
    public <T, Z> ItemBuilder pdc(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        itemMeta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    /**
     * Setzt die Farbe für Lederüstung, Tränke oder Karten.
     * @param color Die Farbe.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setColor(@NotNull Color color) {
        if (itemMeta instanceof LeatherArmorMeta meta) meta.setColor(color);
        else if (itemMeta instanceof PotionMeta meta) meta.setColor(color);
        else if (itemMeta instanceof MapMeta meta) meta.setColor(color);
        return this;
    }

    /**
     * Setzt den Kopf-Besitzer via UUID.
     * @param uuid UUID des Spielers.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setSkullOwner(@NotNull UUID uuid) {
        if (itemMeta instanceof SkullMeta meta) {
            PlayerProfile profile = Bukkit.createProfile(uuid);
            meta.setPlayerProfile(profile);
        }
        return this;
    }

    /**
     * Setzt eine Custom-Textur für einen Kopf via Base64.
     * @param base64 Textur-String.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setSkullTexture(@NotNull String base64) {
        if (itemMeta instanceof SkullMeta meta) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", base64));
            meta.setPlayerProfile(profile);
        }
        return this;
    }

    /**
     * Bearbeitet die Food-Komponente (Minecraft 1.21+).
     * @param consumer Zugriff auf die Komponente.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setFood(@NotNull Consumer<FoodComponent> consumer) {
        FoodComponent food = itemMeta.getFood();
        consumer.accept(food);
        itemMeta.setFood(food);
        return this;
    }

    /**
     * Bearbeitet die Tool-Komponente (Minecraft 1.21+).
     * @param consumer Zugriff auf die Komponente.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setTool(@NotNull Consumer<ToolComponent> consumer) {
        ToolComponent tool = itemMeta.getTool();
        consumer.accept(tool);
        itemMeta.setTool(tool);
        return this;
    }

    /**
     * Fügt einen Attribut-Modifier hinzu.
     * @param attribute Attribut.
     * @param modifier Modifier.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder addAttribute(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) {
        itemMeta.addAttributeModifier(attribute, modifier);
        return this;
    }

    /**
     * Ermöglicht es, das Item in einen bestimmten Slot auszurüsten.
     * @param slot EquipmentSlot.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setEquippable(EquipmentSlot slot) {
        EquippableComponent equippable = itemMeta.getEquippable();
        equippable.setSlot(slot);
        itemMeta.setEquippable(equippable);
        return this;
    }

    /**
     * Setzt die Generic-Item-ID (PDC).
     * @param id Eindeutige ID.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setGenericId(@NotNull String id) {
        return pdc(genericIdKey(), PersistentDataType.STRING, id);
    }

    /**
     * Setzt den Besitzer eines Bound-Items (PDC).
     * @param owner UUID des Besitzers.
     * @return Der aktuelle Builder.
     */
    public ItemBuilder setBoundOwner(@NotNull UUID owner) {
        return pdc(boundOwnerKey(), PersistentDataType.STRING, owner.toString());
    }

    /** @return NamespacedKey für die Generic-ID. */
    public static @NotNull NamespacedKey genericIdKey() {
        return new NamespacedKey(RedJuluLib.getPlugin(), PDC_GENERIC_ID);
    }

    /** @return NamespacedKey für den Bound-Besitzer. */
    public static @NotNull NamespacedKey boundOwnerKey() {
        return new NamespacedKey(RedJuluLib.getPlugin(), PDC_BOUND_OWNER);
    }

    /**
     * Erstellt einen Platzhalter mit leerem Namen.
     * @param material Material.
     * @return Builder mit leerem Namen.
     */
    public static @NotNull ItemBuilder placeholder(@NotNull Material material) {
        return new ItemBuilder(material).setName(Component.empty());
    }

    /**
     * Erstellt den fertigen ItemStack.
     * @return Der gebaute ItemStack.
     */
    public @NotNull ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}