package com.example.chartoolbox.model.keys;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Finds every key that two or more actions are fighting over. */
public final class Clashes {
    private Clashes() {}

    /** Groups of actions that fight, each group on one key, sorted by the key's name. Actions keep the game's order. */
    public static List<Clash> find(List<Bind> all, Meet meet) {
        int n = all.size();
        int[] group = new int[n];
        for (int i = 0; i < n; i++) group[i] = i;
        boolean[] fights = new boolean[n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (!KeyRules.fight(all.get(i), all.get(j), meet)) continue;
                fights[i] = fights[j] = true;
                join(group, i, j);
            }
        }
        List<Clash> out = new ArrayList<>();
        boolean[] done = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (!fights[i] || done[i]) continue;
            List<Bind> members = new ArrayList<>();
            int root = top(group, i);
            for (int j = i; j < n; j++) {
                if (fights[j] && !done[j] && top(group, j) == root) {
                    members.add(all.get(j));
                    done[j] = true;
                }
            }
            out.add(new Clash(all.get(i).key(), List.copyOf(members)));
        }
        out.sort(Comparator.comparing(Clash::key));
        return out;
    }

    private static int top(int[] group, int i) {
        while (group[i] != i) i = group[i] = group[group[i]];
        return i;
    }

    private static void join(int[] group, int a, int b) {
        int ra = top(group, a), rb = top(group, b);
        if (ra != rb) group[Math.max(ra, rb)] = Math.min(ra, rb);
    }
}
