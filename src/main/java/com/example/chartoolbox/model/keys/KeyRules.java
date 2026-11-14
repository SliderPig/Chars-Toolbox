package com.example.chartoolbox.model.keys;

/**
 * When two actions fight over a key. This is Forge's own rule (KeyMapping.same in Forge 47.3.0, read with javap) with
 * two things left out on purpose:
 * <ul>
 *   <li>Forge's last line calls two actions "same" by their key alone even when their contexts never meet (one only in
 *       menus, one only in the world). Those can never fire together, so they are not a fight here.</li>
 *   <li>Forge counts a modifier as a clash with the plain modifier key (Shift+G with Sneak on Left Shift). That would
 *       call every Shift+ key a fight with sneaking, so it is not one here.</li>
 * </ul>
 */
public final class KeyRules {
    private KeyRules() {}

    public static final String NONE = "NONE";

    public static boolean fight(Bind a, Bind b, Meet meet) {
        if (a == b || a.id().equals(b.id()) || !a.bound() || !b.bound()) return false;
        if (!meet.meet(a.context(), b.context()) && !meet.meet(b.context(), a.context())) return false;
        if (!a.key().equals(b.key())) return false;
        if (a.modifier().equals(b.modifier())) return true;
        // in the world, holding Shift to sneak and pressing B still presses B: a plain key fights the same key with a modifier
        boolean inWorld = meet.meet(a.context(), Meet.IN_GAME);
        return inWorld && (NONE.equals(a.modifier()) || NONE.equals(b.modifier()));
    }
}
