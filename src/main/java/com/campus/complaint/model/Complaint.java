package com.campus.complaint.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Model class representing a Campus Complaint.
 *
 * OOP Concepts demonstrated:
 * - Encapsulation: private fields with public getters/setters
 * - Data hiding: internal state managed carefully
 */
public class Complaint {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    // Unique complaint identifier
    private final String id;
    private String studentName;
    private String studentId;
    private String category;
    private String description;
    private Priority priority;
    private ComplaintStatus status;
    private final LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;
    private String mediaPath; // Path to attached photo or video
    private final List<String> updateHistory;

    /**
     * Constructor – creates a new complaint with SUBMITTED status.
     */
    public Complaint(String studentName, String studentId,
                     String category, String description, Priority priority, String mediaPath) {
        this.id = "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.studentName = studentName;
        this.studentId = studentId;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.mediaPath = mediaPath;
        this.status = ComplaintStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.lastUpdatedAt = this.submittedAt;
        this.updateHistory = new ArrayList<>();
        this.updateHistory.add("[" + submittedAt.format(FORMATTER) + "] Complaint submitted with priority: " + priority.getDisplayName());
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public String getId()                  { return id; }
    public String getStudentName()         { return studentName; }
    public String getStudentId()           { return studentId; }
    public String getCategory()            { return category; }
    public String getDescription()         { return description; }
    public Priority getPriority()          { return priority; }
    public ComplaintStatus getStatus()     { return status; }
    public LocalDateTime getSubmittedAt()  { return submittedAt; }
    public LocalDateTime getLastUpdatedAt(){ return lastUpdatedAt; }
    public String getMediaPath()           { return mediaPath; }
    public List<String> getUpdateHistory() { return new ArrayList<>(updateHistory); }

    public String getSubmittedAtFormatted()   { return submittedAt.format(FORMATTER); }
    public String getLastUpdatedAtFormatted() { return lastUpdatedAt.format(FORMATTER); }

    // ─── Setters / Business Methods ────────────────────────────────────────────

    public void setStudentName(String studentName)   { this.studentName = studentName; }
    public void setStudentId(String studentId)       { this.studentId = studentId; }
    public void setCategory(String category)         { this.category = category; }
    public void setDescription(String description)   { this.description = description; }
    public void setMediaPath(String mediaPath)       { this.mediaPath = mediaPath; }

    public void setPriority(Priority priority) {
        this.priority = priority;
        addUpdate("Priority changed to: " + priority.getDisplayName());
    }

    /**
     * Updates the status and records it in the history log.
     */
    public void updateStatus(ComplaintStatus newStatus) {
        ComplaintStatus old = this.status;
        this.status = newStatus;
        this.lastUpdatedAt = LocalDateTime.now();
        addUpdate("Status changed from [" + old.getDisplayName()
                + "] → [" + newStatus.getDisplayName() + "]");
    }

    private void addUpdate(String message) {
        this.lastUpdatedAt = LocalDateTime.now();
        updateHistory.add("[" + lastUpdatedAt.format(FORMATTER) + "] " + message);
    }

    @Override
    public String toString() {
        return id + " | " + studentName + " | " + category + " | "
                + priority.getDisplayName() + " | " + status.getDisplayName();
    }
}
