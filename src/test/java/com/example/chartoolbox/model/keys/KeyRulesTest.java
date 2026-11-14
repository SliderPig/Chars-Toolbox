package com.example.chartoolbox.model.keys;

import com.example.chartoolbox.model.keys.Bind.Rank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyRulesTest {
    static Bind bind(String id, String key, String modifier, String context, Rank rank) {
        String owner = rank == Rank.GAME || rank == Rank.GAME_SPARE ? "minecraft" : id.split("\\.")[1];
        return new Bind(id, id, owner, owner, key, modifier, context, rank);
    }

    static Bind bind(String id, String key, String context, Rank rank) {
        return bind(id, key, "NONE", context, rank);
    }

    static final String B = "key.keyboard.b", J = "key.keyboard.j";

    @Test
    void sameKeySameModifierFight() {
        assertTrue(KeyRules.fight(bind("key.charmap.add", B, "UNIVERSAL", Rank.TOOLBOX), bind("key.fortressboxes.toggle", B, "IN_GAME", Rank.TOOLBOX), Meet.STANDARD));
        assertTrue(KeyRules.fight(bind("key.a.x", B, "IN_GAME", Rank.MOD), bind("key.b.y", B, "IN_GAME", Rank.MOD), Meet.STANDARD));
    }

    @Test
    void differentKeysNeverFight() {
        assertFalse(KeyRules.fight(bind("key.a.x", B, "UNIVERSAL", Rank.MOD), bind("key.b.y", J, "UNIVERSAL", Rank.MOD), Meet.STANDARD));
    }

    @Test
    void menuOnlyAndWorldOnlyDoNotFight() {
        assertFalse(KeyRules.fight(bind("key.a.x", B, "GUI", Rank.MOD), bind("key.b.y", B, "IN_GAME", Rank.MOD), Meet.STANDARD));
        assertTrue(KeyRules.fight(bind("key.a.x", B, "GUI", Rank.MOD), bind("key.b.y", B, "GUI", Rank.MOD), Meet.STANDARD));
        assertTrue(KeyRules.fight(bind("key.a.x", B, "GUI", Rank.MOD), bind("key.b.y", B, "UNIVERSAL", Rank.MOD), Meet.STANDARD));
    }

    @Test
    void inTheWorldAPlainKeyFightsTheSameKeyWithAModifier() {
        // holding Shift to sneak and pressing B still presses B: Forge's own reason
        assertTrue(KeyRules.fight(bind("key.a.x", B, "NONE", "IN_GAME", Rank.MOD), bind("key.b.y", B, "SHIFT", "IN_GAME", Rank.MOD), Meet.STANDARD));
        assertFalse(KeyRules.fight(bind("key.a.x", B, "CONTROL", "IN_GAME", Rank.MOD), bind("key.b.y", B, "SHIFT", "IN_GAME", Rank.MOD), Meet.STANDARD));
        // menus only: Ctrl+B and B are different keys there
        assertFalse(KeyRules.fight(bind("key.a.x", B, "NONE", "GUI", Rank.MOD), bind("key.b.y", B, "CONTROL", "GUI", Rank.MOD), Meet.STANDARD));
    }

    @Test
    void aModifierKeyIsNotAClashWithShiftSomething() {
        assertFalse(KeyRules.fight(bind("key.sneak", "key.keyboard.left.shift", "UNIVERSAL", Rank.GAME),
                bind("key.b.y", "key.keyboard.g", "SHIFT", "IN_GAME", Rank.MOD), Meet.STANDARD));
    }

    @Test
    void anUnboundActionFightsNothing() {
        Bind a = bind("key.a.x", "key.keyboard.unknown", "UNIVERSAL", Rank.MOD), b = bind("key.b.y", "key.keyboard.unknown", "UNIVERSAL", Rank.MOD);
        assertFalse(a.bound());
        assertFalse(KeyRules.fight(a, b, Meet.STANDARD));
    }

    @Test
    void anActionNeverFightsItself() {
        Bind a = bind("key.a.x", B, "UNIVERSAL", Rank.MOD);
        assertFalse(KeyRules.fight(a, a, Meet.STANDARD));
    }

    @Test
    void threeActionsOnOneKeyAreOneClash() {
        List<Clash> found = Clashes.find(List.of(bind("key.a.x", B, "UNIVERSAL", Rank.MOD), bind("key.b.y", B, "IN_GAME", Rank.MOD),
                bind("key.c.z", B, "IN_GAME", Rank.TOOLBOX), bind("key.d.w", J, "IN_GAME", Rank.MOD)), Meet.STANDARD);
        assertEquals(1, found.size());
        assertEquals(B, found.get(0).key());
        assertEquals(3, found.get(0).binds().size());
    }

    @Test
    void clashesComeSortedByKeyAndNoneWhenAllIsFine() {
        List<Clash> found = Clashes.find(List.of(bind("key.a.x", J, "IN_GAME", Rank.MOD), bind("key.b.y", J, "IN_GAME", Rank.MOD),
                bind("key.c.x", B, "IN_GAME", Rank.MOD), bind("key.d.y", B, "IN_GAME", Rank.MOD)), Meet.STANDARD);
        assertEquals(List.of(B, J), found.stream().map(Clash::key).toList());
        assertTrue(Clashes.find(List.of(bind("key.a.x", J, "IN_GAME", Rank.MOD), bind("key.c.x", B, "IN_GAME", Rank.MOD)), Meet.STANDARD).isEmpty());
    }

    @Test
    void aClashSignatureIgnoresOrder() {
        Bind a = bind("key.a.x", B, "UNIVERSAL", Rank.MOD), b = bind("key.b.y", B, "IN_GAME", Rank.MOD);
        assertEquals(new Clash(B, List.of(a, b)).signature(), new Clash(B, List.of(b, a)).signature());
    }

    @Test
    void ownerFromTheNameOrTheCategory() {
        Set<String> mods = Set.of("minecraft", "charmap", "soulrelease", "forge");
        assertEquals("charmap", Owners.guess("key.charmap.add_waypoint", "key.categories.charmap", mods));
        assertEquals("soulrelease", Owners.guess("key.release", "key.categories.soulrelease", mods));
        assertEquals("soulrelease", Owners.guess("soulrelease.key.release", "Soul Release", mods));
        assertNull(Owners.guess("key.jump", "key.categories.movement", mods));
    }

    @Test
    void theRowSaysHowManyKeysHaveMoreThanOneJob() {
        Bind a = bind("key.a.x", B, "UNIVERSAL", Rank.MOD), b = bind("key.b.y", B, "IN_GAME", Rank.MOD), c = bind("key.c.y", B, "IN_GAME", Rank.MOD);
        Bind d = bind("key.a.z", J, "UNIVERSAL", Rank.MOD), e = bind("key.b.z", J, "IN_GAME", Rank.MOD);
        assertEquals("", ClashWords.rowLine(List.of(), k -> "B"));
        assertEquals("Key B has 2 jobs", ClashWords.rowLine(List.of(new Clash(B, List.of(a, b))), k -> "B"));
        assertEquals("Key B has 3 jobs", ClashWords.rowLine(List.of(new Clash(B, List.of(a, b, c))), k -> "B"));
        assertEquals("2 keys clash", ClashWords.rowLine(List.of(new Clash(B, List.of(a, b)), new Clash(J, List.of(d, e))), k -> k));
    }
}
