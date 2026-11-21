package com.armyja.boxscreen;

/** Creates the organizer-controlled stages used by SplitSession. */
interface SplitStageOrganizer {
    SplitSession.Stage create(String name, int windowingMode, SplitSession.Stage parent)
            throws ReflectiveOperationException;

    /** Some firmware can create only display-root tasks, so their stages must be nested later. */
    boolean requiresExplicitStageParenting();

    void close() throws ReflectiveOperationException;
}
