package com.example.chartoolbox.model.keys;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What the Key clash fixer remembers in a profile (config/chartoolbox/keyclash.properties): the clashes it has already
 * told you about (so the notice never comes twice for one) and the last "fix for me" (so it can be undone after a restart).
 *
 * <pre>
 * seen=key.keyboard.b=key.charmap.add_waypoint+key.fortressboxes.toggle
 * seen=...
 * undo=key.fortressboxes.toggle|key.keyboard.b|NONE|key.keyboard.j|Toggle fortress boxes|Fortress Boxes
 * </pre>
 * One line each. Values are escaped (\| and \\) so any name fits.
 */
public final class ClashMemory {
    private final Set<String> seen = new LinkedHashSet<>();
    private List<Move> undo = List.of();

    public List<Clash> newOnes(List<Clash> clashes) {
        return clashes.stream().filter(c -> !seen.contains(c.signature())).toList();
    }

    public void remember(List<Clash> clashes) {
        for (Clash c : clashes) seen.add(c.signature());
    }

    public List<Move> undo() {
        return undo;
    }

    public void setUndo(List<Move> moves) {
        undo = List.copyOf(moves);
    }

    public String format() {
        StringBuilder sb = new StringBuilder("# Char's Toolbox: key clashes already shown, and the last fix (for Undo)\n");
        for (String s : seen) sb.append("seen=").append(s).append('\n');
        for (Move m : undo) {
            sb.append("undo=").append(String.join("|", esc(m.id()), esc(m.fromKey()), esc(m.fromModifier()), esc(m.toKey()), esc(m.label()), esc(m.ownerName())))
                    .append('\n');
        }
        return sb.toString();
    }

    /** Read forgivingly: a line that doesn't make sense is skipped. */
    public static ClashMemory parse(String text) {
        ClashMemory m = new ClashMemory();
        List<Move> moves = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            if (line.startsWith("seen=") && line.length() > 5) m.seen.add(line.substring(5));
            if (line.startsWith("undo=")) {
                List<String> f = split(line.substring(5));
                if (f.size() == 6 && !f.get(0).isEmpty()) moves.add(new Move(f.get(0), f.get(4), f.get(5), f.get(1), f.get(2), f.get(3)));
            }
        }
        m.undo = List.copyOf(moves);
        return m;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) cur.append(s.charAt(++i));
            else if (c == '|') {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
}
