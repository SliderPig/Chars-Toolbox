package com.example.chartoolbox.model.keys;

/**
 * Can an action of context a be live while one of context b is? (Forge's IKeyConflictContext.conflicts.) In the game
 * this asks the real context objects; {@link #STANDARD} is Forge's three own ones, for tests.
 */
@FunctionalInterface
public interface Meet {
    boolean meet(String a, String b);

    String UNIVERSAL = "UNIVERSAL", IN_GAME = "IN_GAME", GUI = "GUI";

    /** UNIVERSAL meets everything; GUI and IN_GAME only themselves (as in Forge's KeyConflictContext). */
    Meet STANDARD = (a, b) -> UNIVERSAL.equals(a) || a.equals(b);
}
