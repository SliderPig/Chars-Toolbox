package com.example.chartoolbox.model;

import java.util.ArrayList;
import java.util.List;

/** Where the group labels and the part rows go inside the list area of the switch screen. Plain numbers, no game. */
public final class RowLayout {
    private RowLayout() {}

    public record Rect(float x, float y, float w, float h) {
        public boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** A group label (part == null) or a part's row (label == null). */
    public record Placed(String label, Part part, Rect rect) {}

    public static final float ROW_H = 26, GAP = 6, LABEL_H = 14, MIN_COL_W = 150;
    private static final float SWITCH_W = 22, SWITCH_H = 12, SWITCH_INSET = 8, GEAR = 12, GEAR_GAP = 6;

    /**
     * Each group that has parts gets its label on a line of its own, then its rows: two to a line when there is room
     * for two columns, otherwise one. Positions start at the top of the area; scrolling is the screen's job.
     */
    public static List<Placed> place(List<String> groupNames, List<List<Part>> groups, Rect area) {
        List<Placed> out = new ArrayList<>();
        boolean two = area.w() >= 2 * MIN_COL_W + GAP;
        float colW = two ? (area.w() - GAP) / 2f : area.w();
        float y = area.y();
        for (int g = 0; g < groups.size(); g++) {
            List<Part> parts = groups.get(g);
            if (parts.isEmpty()) continue;
            if (!out.isEmpty()) y += GAP;
            out.add(new Placed(groupNames.get(g), null, new Rect(area.x(), y, area.w(), LABEL_H)));
            y += LABEL_H + GAP;
            for (int k = 0; k < parts.size(); k++) {
                int col = two ? k % 2 : 0;
                if (k > 0 && col == 0) y += ROW_H + GAP;
                out.add(new Placed(null, parts.get(k), new Rect(area.x() + col * (colW + GAP), y, colW, ROW_H)));
            }
            y += ROW_H;
        }
        return out;
    }

    /** How tall everything placed is, from the top of the area to the bottom of the last row. */
    public static float contentHeight(List<Placed> placed, Rect area) {
        float bottom = area.y();
        for (Placed p : placed) bottom = Math.max(bottom, p.rect().y() + p.rect().h());
        return bottom - area.y();
    }

    public static Rect switchOf(Rect row) {
        return new Rect(row.x() + row.w() - SWITCH_INSET - SWITCH_W, row.y() + (row.h() - SWITCH_H) / 2f, SWITCH_W, SWITCH_H);
    }

    public static Rect gearOf(Rect row) {
        Rect s = switchOf(row);
        return new Rect(s.x() - GEAR_GAP - GEAR, row.y() + (row.h() - GEAR) / 2f, GEAR, GEAR);
    }
}
