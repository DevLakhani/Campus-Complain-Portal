package com.campus.complaint.model;

/**
 * Enum representing priority levels of a complaint.
 * OOP Concept: Enum with behavior (fields + methods).
 */
public enum Priority {
    LOW("Low", 1, "#16a34a"),
    MEDIUM("Medium", 2, "#d97706"),
    HIGH("High", 3, "#ea580c"),
    CRITICAL("Critical", 4, "#dc2626");

    private final String displayName;
    private final int level;
    private final String colorHex;

    Priority(String displayName, int level, String colorHex) {
        this.displayName = displayName;
        this.level = level;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
