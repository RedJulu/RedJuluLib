package de.redjulu.lib.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogBuilder {

    private Component title = Component.empty();
    private final List<DialogBody> bodies = new ArrayList<>();
    private final List<DialogInput> inputs = new ArrayList<>();
    private final List<ActionButton> actions = new ArrayList<>();
    private DialogType type = null;
    private boolean canCloseWithEscape = true;
    private Component externalTitle;
    private DialogBase.DialogAfterAction afterAction;

    public DialogBuilder() {
    }

    public DialogBuilder(@NotNull Component title) {
        this.title = title;
    }

    public static DialogBuilder create() {
        return new DialogBuilder();
    }

    public static DialogBuilder create(@NotNull Component title) {
        return new DialogBuilder(title);
    }

    /**
     * Zentriert den Titel grob durch Padding.
     */
    public DialogBuilder centeredTitle() {
        String plain = PlainTextComponentSerializer.plainText().serialize(this.title);
        int spaces = Math.max(0, (30 - plain.length()) / 2);
        this.title = Component.text(" ".repeat(spaces)).append(this.title);
        return this;
    }

    public DialogBuilder title(@NotNull Component title) {
        this.title = title;
        return this;
    }

    public DialogBuilder addText(@NotNull Component text) {
        this.bodies.add(DialogBody.plainMessage(text));
        return this;
    }

    public DialogBuilder addItem(@NotNull ItemStack item) {
        this.bodies.add(DialogBody.item(item).build());
        return this;
    }

    public DialogBuilder addTextInput(@NotNull String key, @NotNull Component label, @Nullable String initial, int maxLength) {
        TextDialogInput.Builder builder = DialogInput.text(key, label);
        if (initial != null) builder.initial(initial);
        if (maxLength > 0) builder.maxLength(maxLength);
        this.inputs.add(builder.build());
        return this;
    }

    public DialogBuilder addBooleanInput(@NotNull String key, @NotNull Component label, boolean initial) {
        this.inputs.add(DialogInput.bool(key, label).initial(initial).build());
        return this;
    }

    public DialogBuilder addNumberInput(@NotNull String key, @NotNull Component label, float min, float max, float initial, float step) {
        NumberRangeDialogInput.Builder builder = DialogInput.numberRange(key, label, min, max).initial(initial);
        if (step > 0) {
            builder.step(step);
        }
        this.inputs.add(builder.build());
        return this;
    }

    public DialogBuilder addOptionsInput(@NotNull String key, @NotNull Component label, @NotNull Map<String, Component> options, @Nullable String initialKey) {
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();

        options.forEach((id, display) -> {
            boolean isInitial = id.equals(initialKey);
            entries.add(SingleOptionDialogInput.OptionEntry.create(id, display, isInitial));
        });

        if (!entries.isEmpty() && entries.stream().noneMatch(SingleOptionDialogInput.OptionEntry::initial)) {
            SingleOptionDialogInput.OptionEntry first = entries.get(0);
            entries.set(0, SingleOptionDialogInput.OptionEntry.create(first.id(), first.display(), true));
        }

        this.inputs.add(DialogInput.singleOption(key, label, entries).build());
        return this;
    }

    public DialogBuilder addButton(@NotNull Component text, @Nullable DialogActionCallback callback) {
        this.actions.add(ActionButton.builder(text)
                .action(callback == null ? null : DialogAction.customClick(callback, ClickCallback.Options.builder().uses(1).build()))
                .build());
        return this;
    }

    public DialogBuilder setConfirmation(@NotNull Component yesText, @NotNull DialogActionCallback yesCallback, @NotNull Component noText, @Nullable DialogActionCallback noCallback) {
        this.type = DialogType.confirmation(
                ActionButton.builder(yesText).action(DialogAction.customClick(yesCallback, ClickCallback.Options.builder().uses(1).build())).build(),
                ActionButton.builder(noText).action(noCallback == null ? null : DialogAction.customClick(noCallback, ClickCallback.Options.builder().uses(1).build())).build()
        );
        return this;
    }

    public DialogBuilder escape(boolean canClose) {
        this.canCloseWithEscape = canClose;
        return this;
    }

    public DialogBuilder forceDecision() {
        this.canCloseWithEscape = false;
        return this;
    }

    public DialogBuilder externalTitle(@Nullable Component externalTitle) {
        this.externalTitle = externalTitle;
        return this;
    }

    public DialogBuilder afterAction(DialogBase.DialogAfterAction afterAction) {
        this.afterAction = afterAction;
        return this;
    }

    public Dialog build() {
        DialogBase.Builder baseBuilder = DialogBase.builder(title)
                .canCloseWithEscape(canCloseWithEscape)
                .body(bodies)
                .inputs(inputs);

        if (externalTitle != null) {
            baseBuilder.externalTitle(externalTitle);
        }

        if (afterAction != null) {
            baseBuilder.afterAction(afterAction);
        }

        DialogType finalType = this.type;
        if (finalType == null) {
            if (actions.isEmpty()) {
                finalType = DialogType.notice();
            } else {
                finalType = DialogType.multiAction(actions).build();
            }
        }

        DialogType finalTypeCopy = finalType;
        return Dialog.create(builder -> builder.empty()
                .base(baseBuilder.build())
                .type(finalTypeCopy)
        );
    }

    public void show(@NotNull Audience audience) {
        if (audience instanceof Player player) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BaseGUI<?, ?> gui) {
                gui.setDialogOpen(true);
                player.closeInventory();
            }
        }
        audience.showDialog(build());
    }
}