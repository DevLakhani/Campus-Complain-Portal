package com.campus.complaint.service;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.model.ComplaintStatus;
import com.campus.complaint.model.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing complaints.
 *
 * OOP Concepts demonstrated:
 * - Single Responsibility Principle: manages only complaint CRUD + business logic
 * - Encapsulation: internal list is never exposed directly
 */
public class ComplaintService {

    private final List<Complaint> complaints = new ArrayList<>();

    /**
     * Submits a new complaint and returns it.
     */
    public Complaint submitComplaint(String studentName, String studentId,
                                     String category, String description,
                                     Priority priority, String mediaPath) {
        if (studentName == null || studentName.isBlank())
            throw new IllegalArgumentException("Student name cannot be empty.");
        if (studentId == null || studentId.isBlank())
            throw new IllegalArgumentException("Student ID cannot be empty.");
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Category cannot be empty.");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Description cannot be empty.");
        if (priority == null)
            throw new IllegalArgumentException("Priority must be selected.");

        Complaint complaint = new Complaint(studentName, studentId, category, description, priority, mediaPath);
        complaints.add(complaint);
        return complaint;
    }

    /**
     * Returns an unmodifiable snapshot of all complaints.
     */
    public List<Complaint> getAllComplaints() {
        return new ArrayList<>(complaints);
    }

    /**
     * Finds a complaint by its ID.
     */
    public Optional<Complaint> findById(String id) {
        return complaints.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst();
    }

    /**
     * Manually updates a complaint's status.
     */
    public void updateStatus(String complaintId, ComplaintStatus newStatus) {
        findById(complaintId).ifPresent(c -> c.updateStatus(newStatus));
    }

    /**
     * Assigns a new priority to a complaint.
     */
    public void assignPriority(String complaintId, Priority newPriority) {
        findById(complaintId).ifPresent(c -> c.setPriority(newPriority));
    }

    /**
     * Returns the total number of complaints.
     */
    public int getTotalCount() {
        return complaints.size();
    }

    /**
     * Deletes a complaint by its ID.
     */
    public void deleteComplaint(String id) {
        complaints.removeIf(c -> c.getId().equals(id));
    }

    /**
     * Counts complaints with a specific status.
     */
    public long countByStatus(ComplaintStatus status) {
        return complaints.stream()
                .filter(c -> c.getStatus() == status)
                .count();
    }
}
