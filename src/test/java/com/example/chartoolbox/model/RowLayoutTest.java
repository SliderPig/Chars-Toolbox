package com.example.chartoolbox.model;

import com.example.chartoolbox.model.RowLayout.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowLayoutTest {
    static final Part A = part("a"), B = part("b"), C = part("c");
    static final Rect area = new Rect(0, 0, 320, 200);

    static Part part(String id) {
        return new Part(id, id, "", Part.Kind.CARRIED, true, List.of(), null, false);
    }

    @Test
    void twoRowsShareALine() {
        var p = RowLayout.place(List.of("Mods"), List.of(List.of(A, B, C)), area);
        assertEquals("Mods", p.get(0).label());
        assertEquals(p.get(1).rect().y(), p.get(2).rect().y());
        assertTrue(p.get(2).rect().x() > p.get(1).rect().x());
        assertEquals(p.get(1).rect().y() + RowLayout.ROW_H + RowLayout.GAP, p.get(3).rect().y());
    }

    @Test
    void narrowAreaUsesOneColumn() {
        var p = RowLayout.place(List.of("Mods"), List.of(List.of(A, B)), new Rect(0, 0, 200, 200));
        assertEquals(p.get(1).rect().x(), p.get(2).rect().x());
        assertEquals(200f, p.get(1).rect().w());
    }

    @Test
    void anEmptyGroupIsSkipped() {
        assertTrue(RowLayout.place(List.of("New"), List.of(List.of()), area).isEmpty());
    }

    @Test
    void theSwitchSitsInsideTheRowOnTheRight() {
        Rect row = new Rect(10, 10, 157, 26), s = RowLayout.switchOf(row), g = RowLayout.gearOf(row);
        assertEquals(22f, s.w());
        assertEquals(12f, s.h());
        assertEquals(row.x() + row.w() - 8, s.x() + s.w());
        assertEquals(s.x() - 6, g.x() + g.w());
        assertTrue(row.has(s.x() + 1, s.y() + 1));
    }

    @Test
    void contentHeightIsTheBottomOfTheLastRow() {
        var p = RowLayout.place(List.of("Mods"), List.of(List.of(A, B, C)), area);
        assertEquals(RowLayout.LABEL_H + RowLayout.GAP + 2 * RowLayout.ROW_H + RowLayout.GAP, RowLayout.contentHeight(p, area));
    }
}
