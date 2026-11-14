package com.example.chartoolbox.model.keys;

import com.example.chartoolbox.model.keys.Bind.Rank;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.chartoolbox.model.keys.KeyRulesTest.bind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixPlanTest {
    static final String B = "key.keyboard.b", C = "key.keyboard.c", J = "key.keyboard.j", H = "key.keyboard.h", N = "key.keyboard.n";
    static final List<String> FREE = List.of(J, H, N);

    static Map<String, String> moved(List<Move> moves) {
        return moves.stream().collect(Collectors.toMap(Move::id, Move::toKey));
    }

    /** The keys after the moves are done: nothing may fight any more. */
    static List<Bind> after(List<Bind> all, List<Move> moves) {
        Map<String, String> to = moved(moves);
        List<Bind> out = new ArrayList<>();
        for (Bind b : all) out.add(to.containsKey(b.id()) ? b.withKey(to.get(b.id()), "NONE") : b);
        return out;
    }

    @Test
    void theGamesOwnKeyStays() {
        List<Bind> all = List.of(bind("key.inventory", "key.keyboard.e", "UNIVERSAL", Rank.GAME), bind("key.mod.open", "key.keyboard.e", "IN_GAME", Rank.MOD));
        List<Move> moves = FixPlan.make(all, Meet.STANDARD, FREE);
        assertEquals(Map.of("key.mod.open", J), moved(moves));
    }

    @Test
    void aToolboxPartMovesBeforeAnotherModsAction() {
        List<Bind> all = List.of(bind("key.charmap.add", B, "UNIVERSAL", Rank.TOOLBOX), bind("key.other.thing", B, "IN_GAME", Rank.MOD));
        assertEquals(Map.of("key.charmap.add", J), moved(FixPlan.make(all, Meet.STANDARD, FREE)));
    }

    @Test
    void theCreativeHotbarKeyGivesWayToAMod() {
        List<Bind> all = List.of(bind("key.saveToolbarActivator", C, "UNIVERSAL", Rank.GAME_SPARE), bind("key.soulrelease.step", C, "IN_GAME", Rank.MOD));
        assertEquals(Map.of("key.saveToolbarActivator", J), moved(FixPlan.make(all, Meet.STANDARD, FREE)));
    }

    @Test
    void sameRankKeepsTheOneRegisteredFirst() {
        List<Bind> all = List.of(bind("key.charmap.add", B, "UNIVERSAL", Rank.TOOLBOX), bind("key.fortressboxes.toggle", B, "IN_GAME", Rank.TOOLBOX));
        assertEquals(Map.of("key.fortressboxes.toggle", J), moved(FixPlan.make(all, Meet.STANDARD, FREE)));
    }

    @Test
    void aFreeKeyIsReallyFree() {
        // J is taken, so the move goes to H
        List<Bind> all = List.of(bind("key.a.x", B, "IN_GAME", Rank.MOD), bind("key.b.y", B, "IN_GAME", Rank.MOD), bind("key.c.z", J, "UNIVERSAL", Rank.MOD));
        assertEquals(Map.of("key.b.y", H), moved(FixPlan.make(all, Meet.STANDARD, FREE)));
    }

    @Test
    void twoMovesNeverShareTheirNewKey() {
        List<Bind> all = List.of(bind("key.a.x", B, "IN_GAME", Rank.MOD), bind("key.b.y", B, "IN_GAME", Rank.MOD),
                bind("key.c.x", C, "IN_GAME", Rank.MOD), bind("key.d.y", C, "IN_GAME", Rank.MOD), bind("key.e.z", C, "IN_GAME", Rank.MOD));
        List<Move> moves = FixPlan.make(all, Meet.STANDARD, FREE);
        assertEquals(3, moves.size());
        assertTrue(Clashes.find(after(all, moves), Meet.STANDARD).isEmpty());
        assertNotEquals(moves.get(0).toKey(), moves.get(1).toKey());
    }

    @Test
    void nothingFreeClearsTheAction() {
        List<Bind> all = List.of(bind("key.a.x", B, "IN_GAME", Rank.MOD), bind("key.b.y", B, "IN_GAME", Rank.MOD), bind("key.c.z", J, "UNIVERSAL", Rank.MOD));
        List<Move> moves = FixPlan.make(all, Meet.STANDARD, List.of(J));
        assertEquals(1, moves.size());
        assertEquals(Bind.UNBOUND, moves.get(0).toKey());
    }

    @Test
    void aMoveRemembersWhereItCameFrom() {
        List<Bind> all = List.of(bind("key.a.x", B, "IN_GAME", Rank.MOD), bind("key.b.y", B, "SHIFT", "IN_GAME", Rank.MOD));
        Move m = FixPlan.make(all, Meet.STANDARD, FREE).get(0);
        assertEquals("key.b.y", m.id());
        assertEquals(B, m.fromKey());
        assertEquals("SHIFT", m.fromModifier());
    }

    @Test
    void nothingToFixIsNoMoves() {
        assertTrue(FixPlan.make(List.of(bind("key.a.x", B, "IN_GAME", Rank.MOD)), Meet.STANDARD, FREE).isEmpty());
    }

    @Test
    void theFreeListHasNoKeyTheGameKeepsForItself() {
        for (String k : List.of("key.keyboard.escape", "key.keyboard.f1", "key.keyboard.f3", "key.keyboard.left.shift", "key.keyboard.enter")) {
            assertTrue(!FreeKeys.ORDER.contains(k), k);
        }
        assertEquals("key.keyboard.j", FreeKeys.ORDER.get(0));
    }
}
