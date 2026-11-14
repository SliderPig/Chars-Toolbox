package com.example.chartoolbox.model.keys;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Fix for me": in each clash one action keeps the key (the game's own, then another mod's, then the game's creative
 * hotbar keys, then a Toolbox part; on a tie the one registered first) and every other one moves to the first free key.
 * A planned move counts straight away, so two moves never land on one key. Nothing free = the action is cleared.
 */
public final class FixPlan {
    private FixPlan() {}

    public static List<Move> make(List<Bind> all, Meet meet, List<String> freeOrder) {
        List<Bind> now = new ArrayList<>(all);
        List<Move> moves = new ArrayList<>();
        for (Clash clash : Clashes.find(all, meet)) {
            List<Bind> members = new ArrayList<>(clash.binds());
            Bind keeper = members.stream().min(Comparator.comparingInt((Bind b) -> b.rank().ordinal())
                    .thenComparingInt(all::indexOf)).orElseThrow();
            for (Bind b : members) {
                if (b == keeper) continue;
                int at = indexOf(now, b.id());
                // still fighting the keeper? (moving an earlier one may already have sorted it out)
                if (!stillFights(now, at, meet)) continue;
                String to = Bind.UNBOUND;
                for (String candidate : freeOrder) {
                    Bind tried = b.withKey(candidate, KeyRules.NONE);
                    if (fightsAny(now, tried, meet)) continue;
                    to = candidate;
                    break;
                }
                now.set(at, b.withKey(to, KeyRules.NONE));
                moves.add(new Move(b.id(), b.label(), b.ownerName(), b.key(), b.modifier(), to));
            }
        }
        return moves;
    }

    private static int indexOf(List<Bind> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(id)) return i;
        return -1;
    }

    private static boolean stillFights(List<Bind> now, int at, Meet meet) {
        return fightsAny(now, now.get(at), meet);
    }

    private static boolean fightsAny(List<Bind> now, Bind b, Meet meet) {
        for (Bind other : now) if (KeyRules.fight(b, other, meet)) return true;
        return false;
    }
}
