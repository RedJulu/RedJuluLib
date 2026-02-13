package de.redjulu.lib.gui;

/**
 * Eine vereinfachte Version der BaseGUI für Inventare, die keine Kategorien oder Filter benötigen.
 *
 * @param <T> Datentyp der angezeigten Objekte.
 */
public abstract class SimpleGUI<T> extends BaseGUI<T, SimpleGUI.NoCategory> {

    /** Interner Dummy-Enum für GUIs ohne Kategorien. */
    protected enum NoCategory {
        NONE
    }

    /**
     * Erstellt eine SimpleGUI.
     *
     * @param rows     Anzahl der Reihen (1-6).
     * @param titleKey Sprach-Key oder MiniMessage-String.
     */
    public SimpleGUI(int rows, String titleKey) {
        super(rows, titleKey, 0, 0, 0, 0, NoCategory.NONE);
    }

    /**
     * Erstellt eine SimpleGUI mit definiertem Padding für den Content-Bereich.
     *
     * @param rows     Anzahl der Reihen (1-6).
     * @param titleKey Sprach-Key oder MiniMessage-String.
     * @param t        Padding Top.
     * @param b        Padding Bottom.
     * @param l        Padding Left.
     * @param r        Padding Right.
     */
    public SimpleGUI(int rows, String titleKey, int t, int b, int l, int r) {
        super(rows, titleKey, t, b, l, r, NoCategory.NONE);
    }
}