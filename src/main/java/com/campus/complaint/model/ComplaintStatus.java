package com.campus.complaint.model;

/**
 * Enum representing the possible statuses of a complaint.
 * Supports SDG 16 - Peace, Justice and Strong Institutions.
 */
public enum ComplaintStatus {
    SUBMITTED("Submitted", "#475569"),
    UNDER_REVIEW("Under Review", "#ea580c"),
    IN_PROGRESS("In Progress", "#2563eb"),
    RESOLVED("Resolved", "#059669"),
    CLOSED("Closed", "#1e293b"),
    REJECTED("Rejected", "#e11d48");

    private final String displayName;
    private final String colorHex;

    ComplaintStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
