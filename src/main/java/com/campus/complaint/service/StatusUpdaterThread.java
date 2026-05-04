package com.campus.complaint.service;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.model.ComplaintStatus;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Background thread service that randomly updates complaint statuses.
 *
 * OOP Concepts demonstrated:
 * - Thread safety awareness
 * - Separation of concerns (thread logic vs. business logic)
 * - Runnable / ScheduledExecutor usage
 */
public class StatusUpdaterThread {

    private static final Logger LOGGER = Logger.getLogger(StatusUpdaterThread.class.getName());

    // The auto-updater now only assigns the UNDER_REVIEW status.
    // Further updates like IN_PROGRESS or RESOLVED will be manual.
    private static final ComplaintStatus AUTO_STATUS = ComplaintStatus.UNDER_REVIEW;

    private final ComplaintService complaintService;
    private final Runnable onUpdate;
    private ScheduledExecutorService scheduler;
    private final Random random = new Random();

    /**
     * @param complaintService the service holding complaints
     * @param onUpdate         a callback run on the JavaFX Application Thread after each update
     */
    public StatusUpdaterThread(ComplaintService complaintService, Runnable onUpdate) {
        this.complaintService = complaintService;
        this.onUpdate = onUpdate;
    }

    /**
     * Starts the background scheduler that fires every 4 seconds.
     */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StatusUpdater-Thread");
            t.setDaemon(true); // won't prevent JVM from exiting
            return t;
        });

        scheduler.scheduleAtFixedRate(this::randomlyUpdateStatus, 4, 4, TimeUnit.SECONDS);
        LOGGER.info("StatusUpdaterThread started.");
    }

    /**
     * Stops the background scheduler gracefully.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            LOGGER.info("StatusUpdaterThread stopped.");
        }
    }

    /**
     * Core logic: picks a random complaint that is NOT yet resolved/closed/rejected,
     * then randomly assigns a new status to it.
     */
    private void randomlyUpdateStatus() {
        List<Complaint> complaints = complaintService.getAllComplaints();
        if (complaints.isEmpty()) return;

        // Filter to only SUBMITTED complaints (as further steps are manual)
        List<Complaint> submitted = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.SUBMITTED)
                .toList();

        if (submitted.isEmpty()) return;

        // Pick a random submitted complaint and move it to UNDER_REVIEW
        Complaint target = submitted.get(random.nextInt(submitted.size()));
        target.updateStatus(AUTO_STATUS);
        
        LOGGER.info("Auto-updated " + target.getId() + " → " + AUTO_STATUS.getDisplayName());

        // Run UI refresh on the JavaFX thread
        javafx.application.Platform.runLater(onUpdate);
    }
}
