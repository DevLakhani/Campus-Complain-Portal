package com.campus.complaint.controller;

import com.campus.complaint.model.Complaint;
import com.campus.complaint.model.ComplaintStatus;
import com.campus.complaint.model.Priority;
import com.campus.complaint.service.ComplaintService;
import com.campus.complaint.service.StatusUpdaterThread;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.chart.PieChart;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main JavaFX Controller.
 * OOP: Separation of concerns – controller delegates to service layer.
 */
public class MainController implements Initializable {

    // ── Form fields (Top) ──────────────────────────────────────────────────────
    @FXML private TextField txtStudentName;
    @FXML private TextField txtStudentId;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Priority> cmbPriority;
    @FXML private Button btnSubmit;
    @FXML private Button btnClear;
    @FXML private Label lblFormError;

    // ── Table (Center) ─────────────────────────────────────────────────────────
    @FXML private TableView<Complaint> tblComplaints;
    @FXML private TableColumn<Complaint, String> colId;
    @FXML private TableColumn<Complaint, String> colName;
    @FXML private TableColumn<Complaint, String> colCategory;
    @FXML private TableColumn<Complaint, String> colPriority;
    @FXML private TableColumn<Complaint, String> colStatus;
    @FXML private TableColumn<Complaint, String> colSubmitted;
    @FXML private TableColumn<Complaint, String> colUpdated;
    @FXML private TableColumn<Complaint, Void> colMedia;
    @FXML private TableColumn<Complaint, Void> colDelete;

    // ── Media Selection ────────────────────────────────────────────────────────
    @FXML private Label lblMediaPath;
    @FXML private Button btnSelectMedia;
    private String selectedMediaPath = null;

    // ── Analysis ───────────────────────────────────────────────────────────────
    @FXML private StackPane analysisPane;
    @FXML private PieChart chartCategory;
    @FXML private PieChart chartPriority;

    // ── Status Detail (Bottom) ─────────────────────────────────────────────────
    @FXML private Label lblDetailId;
    @FXML private Label lblDetailStudent;
    @FXML private Label lblDetailCategory;
    @FXML private Label lblDetailPriority;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailSubmitted;
    @FXML private Label lblDetailUpdated;
    @FXML private TextArea txtDetailDescription;
    @FXML private ComboBox<ComplaintStatus> cmbNewStatus;
    @FXML private ComboBox<Priority> cmbNewPriority;
    @FXML private Button btnUpdateStatus;
    @FXML private Button btnUpdatePriority;
    @FXML private StackPane overlayPane;

    // ── Sorting (Center) ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> cmbSort;
    @FXML private Button btnSortToggle;
    private boolean sortDescending = true;

    // ── Stats bar ──────────────────────────────────────────────────────────────
    @FXML private Label lblStatTotal;
    @FXML private Label lblStatSubmitted;
    @FXML private Label lblStatReview;
    @FXML private Label lblStatProgress;
    @FXML private Label lblStatResolved;
    @FXML private Label lblThreadStatus;

    // ── Services ───────────────────────────────────────────────────────────────
    private final ComplaintService service = new ComplaintService();
    private StatusUpdaterThread updaterThread;
    private final ObservableList<Complaint> tableData = FXCollections.observableArrayList();

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCombos();
        setupTable();
        setupCharts();
        setupSelectionListener();
        seedSampleData();
        startBackgroundThread();
        refreshTable();
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private void setupCombos() {
        cmbCategory.setItems(FXCollections.observableArrayList(
                "Academic", "Facility", "IT / Technical", "Library",
                "Hostel / Accommodation", "Financial", "Harassment", "Other"));
        cmbPriority.setItems(FXCollections.observableArrayList(Priority.values()));
        cmbPriority.setValue(Priority.MEDIUM);
        cmbNewStatus.setItems(FXCollections.observableArrayList(ComplaintStatus.values()));
        cmbNewPriority.setItems(FXCollections.observableArrayList(Priority.values()));

        // Setup Sorting combo
        cmbSort.setItems(FXCollections.observableArrayList(
                "Date (Submitted)", "Priority", "Status", "Category", "Student Name"
        ));
        cmbSort.setValue("Date (Submitted)");
        cmbSort.setOnAction(e -> refreshTable());
    }

    private void setupCharts() {
        chartCategory.setAnimated(true);
        chartPriority.setAnimated(true);
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPriority.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPriority().getDisplayName()));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().getDisplayName()));
        colSubmitted.setCellValueFactory(new PropertyValueFactory<>("submittedAtFormatted"));
        colUpdated.setCellValueFactory(new PropertyValueFactory<>("lastUpdatedAtFormatted"));

        // Colour-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                Complaint c = getTableView().getItems().get(getIndex());
                String hex = c.getStatus().getColorHex();
                setStyle("-fx-text-fill: " + hex + "; -fx-font-weight: bold;");
            }
        });

        // Colour-code priority column
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                Complaint c = getTableView().getItems().get(getIndex());
                String hex = c.getPriority().getColorHex();
                setStyle("-fx-text-fill: " + hex + "; -fx-font-weight: bold;");
            }
        });

        tblComplaints.setItems(tableData);
        tblComplaints.setPlaceholder(new Label("No complaints submitted yet."));

        // Setup Delete Column
        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 10;");
                btn.setTooltip(new Tooltip("Delete Complaint"));
                btn.setOnAction(e -> {
                    Complaint c = getTableView().getItems().get(getIndex());
                    handleDeleteComplaint(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btn);
            }
        });

        // Setup Media Column
        colMedia.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("👁");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b82f6; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 10;");
                btn.setTooltip(new Tooltip("View Media"));
                btn.setOnAction(e -> {
                    Complaint c = getTableView().getItems().get(getIndex());
                    handleViewMedia(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Complaint c = getTableView().getItems().get(getIndex());
                    if (c.getMediaPath() != null && !c.getMediaPath().isBlank()) {
                        setGraphic(btn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupSelectionListener() {
        tblComplaints.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null) showDetail(selected);
                });
    }

    // ── Actions ────────────────────────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        lblFormError.setText("");
        try {
            String name = txtStudentName.getText().trim();
            String sid  = txtStudentId.getText().trim();
            String cat  = cmbCategory.getValue();
            String desc = txtDescription.getText().trim();
            Priority pri = cmbPriority.getValue();

            if (cat == null || cat.isBlank()) {
                showError("Please select a category.");
                return;
            }
            
            Complaint c = service.submitComplaint(name, sid, cat, desc, pri, selectedMediaPath);
            refreshTable();
            tblComplaints.getSelectionModel().select(c);
            handleClear();
            showInfo("Complaint " + c.getId() + " submitted successfully!");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleSelectMedia() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Photo or Video");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files", "*.png", "*.jpg", "*.jpeg", "*.mp4", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(btnSelectMedia.getScene().getWindow());
        if (selectedFile != null) {
            selectedMediaPath = selectedFile.getAbsolutePath();
            lblMediaPath.setText(selectedFile.getName());
        }
    }

    private void handleViewMedia(Complaint c) {
        if (c.getMediaPath() != null) {
            try {
                File file = new File(c.getMediaPath());
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showError("File not found: " + c.getMediaPath());
                }
            } catch (IOException ex) {
                showError("Could not open file: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleClear() {
        txtStudentName.clear();
        txtStudentId.clear();
        cmbCategory.setValue(null);
        txtDescription.clear();
        cmbPriority.setValue(Priority.MEDIUM);
        selectedMediaPath = null;
        lblMediaPath.setText("No file selected");
        lblFormError.setText("");
    }

    @FXML
    private void handleUpdateStatus() {
        Complaint selected = tblComplaints.getSelectionModel().getSelectedItem();
        ComplaintStatus newStatus = cmbNewStatus.getValue();
        if (selected == null || newStatus == null) {
            showError("Select a complaint and a new status.");
            return;
        }
        service.updateStatus(selected.getId(), newStatus);
        refreshTable();
        // Re-select to refresh details
        tblComplaints.getSelectionModel().select(selected);
        showDetail(selected);
    }

    @FXML
    private void handleUpdatePriority() {
        Complaint selected = tblComplaints.getSelectionModel().getSelectedItem();
        Priority newPriority = cmbNewPriority.getValue();
        if (selected == null || newPriority == null) {
            showError("Select a complaint and a new priority.");
            return;
        }
        service.assignPriority(selected.getId(), newPriority);
        refreshTable();
        tblComplaints.getSelectionModel().select(selected);
        showDetail(selected);
    }

    @FXML
    private void handleQuickResolve() {
        quickUpdateStatus(ComplaintStatus.RESOLVED);
    }

    @FXML
    private void handleQuickClose() {
        quickUpdateStatus(ComplaintStatus.CLOSED);
    }

    private void quickUpdateStatus(ComplaintStatus status) {
        Complaint selected = tblComplaints.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a complaint first.");
            return;
        }
        service.updateStatus(selected.getId(), status);
        refreshTable();
        tblComplaints.getSelectionModel().select(selected);
        showDetail(selected);
    }

    @FXML
    private void handleSortToggle() {
        sortDescending = !sortDescending;
        btnSortToggle.setText(sortDescending ? "▼ Desc" : "▲ Asc");
        refreshTable();
    }

    @FXML
    private void handleCloseDetail() {
        if (overlayPane != null && overlayPane.isVisible()) {
            // Mac-like snappy close
            FadeTransition fade = new FadeTransition(Duration.millis(180), overlayPane);
            fade.setToValue(0);
            fade.setInterpolator(javafx.animation.Interpolator.EASE_IN);

            ScaleTransition scale = new ScaleTransition(Duration.millis(180), overlayPane);
            scale.setToX(0.75);
            scale.setToY(0.75);
            scale.setInterpolator(javafx.animation.Interpolator.EASE_IN);

            ParallelTransition anim = new ParallelTransition(fade, scale);
            anim.setOnFinished(e -> {
                overlayPane.setVisible(false);
                tblComplaints.getSelectionModel().clearSelection();
            });
            anim.play();
        }
    }

    @FXML
    private void handleOverlayClick(javafx.scene.input.MouseEvent event) {
        // Only close if the background (StackPane) itself was clicked, not the detail panel inside it
        if (event.getSource() == overlayPane && event.getTarget() == overlayPane) {
            handleCloseDetail();
        }
    }

    @FXML
    private void handleShowAnalysis() {
        updateCharts();
        if (analysisPane != null && !analysisPane.isVisible()) {
            analysisPane.setOpacity(0);
            analysisPane.setScaleX(0.65);
            analysisPane.setScaleY(0.65);
            analysisPane.setVisible(true);

            FadeTransition fade = new FadeTransition(Duration.millis(300), analysisPane);
            fade.setToValue(1);
            fade.setInterpolator(javafx.animation.Interpolator.SPLINE(0.1, 1.0, 0.22, 1.0));

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), analysisPane);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(javafx.animation.Interpolator.SPLINE(0.1, 1.0, 0.22, 1.0));

            new ParallelTransition(fade, scale).play();
        }
    }

    @FXML
    private void handleCloseAnalysis() {
        if (analysisPane != null && analysisPane.isVisible()) {
            FadeTransition fade = new FadeTransition(Duration.millis(180), analysisPane);
            fade.setToValue(0);
            fade.setInterpolator(javafx.animation.Interpolator.EASE_IN);

            ScaleTransition scale = new ScaleTransition(Duration.millis(180), analysisPane);
            scale.setToX(0.75);
            scale.setToY(0.75);
            scale.setInterpolator(javafx.animation.Interpolator.EASE_IN);

            ParallelTransition anim = new ParallelTransition(fade, scale);
            anim.setOnFinished(e -> analysisPane.setVisible(false));
            anim.play();
        }
    }

    @FXML
    private void handleAnalysisOverlayClick(javafx.scene.input.MouseEvent event) {
        if (event.getSource() == analysisPane && event.getTarget() == analysisPane) {
            handleCloseAnalysis();
        }
    }

    private void updateCharts() {
        List<Complaint> all = service.getAllComplaints();
        
        // Category Chart
        ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList();
        cmbCategory.getItems().forEach(cat -> {
            long count = all.stream().filter(c -> c.getCategory().equals(cat)).count();
            if (count > 0) categoryData.add(new PieChart.Data(cat, count));
        });
        chartCategory.setData(categoryData);

        // Priority Chart
        ObservableList<PieChart.Data> priorityData = FXCollections.observableArrayList();
        for (Priority p : Priority.values()) {
            long count = all.stream().filter(c -> c.getPriority() == p).count();
            if (count > 0) priorityData.add(new PieChart.Data(p.getDisplayName(), count));
        }
        chartPriority.setData(priorityData);
    }

    @FXML
    private void handleDownloadComplaintPDF() {
        Complaint c = tblComplaints.getSelectionModel().getSelectedItem();
        if (c == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Complaint PDF");
        chooser.setInitialFileName("Complaint_" + c.getId() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(overlayPane.getScene().getWindow());

        if (file != null) {
            try (Document document = new Document()) {
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // Header
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                Paragraph header = new Paragraph("CAMPUS COMPLAINT PORTAL", headerFont);
                header.setAlignment(Element.ALIGN_CENTER);
                document.add(header);
                document.add(new Paragraph("Official Complaint Document"));
                document.add(new LineSeparator());
                document.add(Chunk.NEWLINE);

                // Details
                Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                document.add(new Paragraph("Complaint ID: " + c.getId(), labelFont));
                document.add(new Paragraph("Student Name: " + c.getStudentName()));
                document.add(new Paragraph("Student ID: " + c.getStudentId()));
                document.add(new Paragraph("Category: " + c.getCategory()));
                document.add(new Paragraph("Priority: " + c.getPriority().getDisplayName()));
                document.add(new Paragraph("Status: " + c.getStatus().getDisplayName()));
                document.add(new Paragraph("Submitted At: " + c.getSubmittedAtFormatted()));
                document.add(Chunk.NEWLINE);

                document.add(new Paragraph("Description:", labelFont));
                document.add(new Paragraph(c.getDescription()));
                document.add(Chunk.NEWLINE);
                document.add(Chunk.NEWLINE);
                document.add(Chunk.NEWLINE);

                // Signature Lines
                document.add(new Paragraph("__________________________          __________________________"));
                document.add(new Paragraph("   Student Signature                   Principal Signature"));

                document.close();
                Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                showError("Failed to save PDF: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void handleDownloadReport() {
        List<Complaint> reportList = tblComplaints.getItems();
        if (reportList.isEmpty()) {
            showError("No complaints to download.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report PDF");
        chooser.setInitialFileName("Complaint_Report.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(tblComplaints.getScene().getWindow());

        if (file != null) {
            try (Document document = new Document(PageSize.A4.rotate())) {
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                document.add(new Paragraph("CAMPUS COMPLAINT PORTAL - FULL REPORT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
                document.add(new Paragraph("Generated on: " + java.time.LocalDateTime.now().toString()));
                document.add(Chunk.NEWLINE);

                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
                table.setWidthPercentage(100);
                table.addCell("ID");
                table.addCell("Student");
                table.addCell("Category");
                table.addCell("Priority");
                table.addCell("Status");
                table.addCell("Submitted At");

                for (Complaint c : reportList) {
                    table.addCell(c.getId());
                    table.addCell(c.getStudentName());
                    table.addCell(c.getCategory());
                    table.addCell(c.getPriority().getDisplayName());
                    table.addCell(c.getStatus().getDisplayName());
                    table.addCell(c.getSubmittedAtFormatted());
                }

                document.add(table);
                document.close();
                Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                showError("Failed to save Report: " + ex.getMessage());
            }
        }
    }

    private void handleDeleteComplaint(Complaint c) {
        if (c == null) return;
        
        // Confirmation dialog (optional but good practice)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Complaint: " + c.getId());
        alert.setContentText("Are you sure you want to permanently delete this complaint?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            service.deleteComplaint(c.getId());
            if (overlayPane.isVisible()) {
                Complaint selected = tblComplaints.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getId().equals(c.getId())) {
                    handleCloseDetail();
                }
            }
            refreshTable();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void refreshTable() {
        Complaint previously = tblComplaints.getSelectionModel().getSelectedItem();
        List<Complaint> all = service.getAllComplaints();

        String sortBy = cmbSort.getValue();
        if (sortBy != null) {
            all.sort((c1, c2) -> {
                int cmp = 0;
                switch (sortBy) {
                    case "Priority":
                        cmp = Integer.compare(c1.getPriority().getLevel(), c2.getPriority().getLevel());
                        break;
                    case "Status":
                        cmp = c1.getStatus().name().compareTo(c2.getStatus().name());
                        break;
                    case "Category":
                        cmp = c1.getCategory().compareToIgnoreCase(c2.getCategory());
                        break;
                    case "Student Name":
                        cmp = c1.getStudentName().compareToIgnoreCase(c2.getStudentName());
                        break;
                    case "Date (Submitted)":
                    default:
                        cmp = c1.getSubmittedAt().compareTo(c2.getSubmittedAt());
                        break;
                }
                return sortDescending ? -cmp : cmp;
            });
        }

        tableData.setAll(all);
        updateStats();
        // Re-select if still present
        if (previously != null) {
            tableData.stream()
                     .filter(c -> c.getId().equals(previously.getId()))
                     .findFirst()
                     .ifPresent(c -> {
                         tblComplaints.getSelectionModel().select(c);
                         // Just update the fields, don't trigger a new popup
                         updateDetailFields(c);
                     });
        }
    }

    private void showDetail(Complaint c) {
        updateDetailFields(c);
        if (overlayPane != null && !overlayPane.isVisible()) {
            // Mac-like opening: snappier scale with a spline for a 'pop' effect
            overlayPane.setOpacity(0);
            overlayPane.setScaleX(0.65);
            overlayPane.setScaleY(0.65);
            overlayPane.setVisible(true);

            FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
            fade.setToValue(1);
            fade.setInterpolator(javafx.animation.Interpolator.SPLINE(0.1, 1.0, 0.22, 1.0));

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), overlayPane);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.setInterpolator(javafx.animation.Interpolator.SPLINE(0.1, 1.0, 0.22, 1.0));

            new ParallelTransition(fade, scale).play();
        }
    }

    private void updateDetailFields(Complaint c) {
        lblDetailId.setText(c.getId());
        lblDetailStudent.setText(c.getStudentName() + "  (" + c.getStudentId() + ")");
        lblDetailCategory.setText(c.getCategory());
        lblDetailPriority.setText(c.getPriority().getDisplayName());
        lblDetailPriority.setStyle("-fx-text-fill: " + c.getPriority().getColorHex() + "; -fx-font-weight: bold;");
        lblDetailStatus.setText(c.getStatus().getDisplayName());
        lblDetailStatus.setStyle("-fx-text-fill: " + c.getStatus().getColorHex() + "; -fx-font-weight: bold;");
        lblDetailSubmitted.setText(c.getSubmittedAtFormatted());
        lblDetailUpdated.setText(c.getLastUpdatedAtFormatted());
        txtDetailDescription.setText(c.getDescription());
        cmbNewStatus.setValue(c.getStatus());
        cmbNewPriority.setValue(c.getPriority());
    }

    private void updateStats() {
        lblStatTotal.setText(String.valueOf(service.getTotalCount()));
        lblStatSubmitted.setText(String.valueOf(service.countByStatus(ComplaintStatus.SUBMITTED)));
        lblStatReview.setText(String.valueOf(service.countByStatus(ComplaintStatus.UNDER_REVIEW)));
        lblStatProgress.setText(String.valueOf(service.countByStatus(ComplaintStatus.IN_PROGRESS)));
        lblStatResolved.setText(String.valueOf(service.countByStatus(ComplaintStatus.RESOLVED)));
    }

    private void showError(String msg) {
        lblFormError.setStyle("-fx-text-fill: #dc3545;");
        lblFormError.setText("⚠ " + msg);
    }

    private void showInfo(String msg) {
        lblFormError.setStyle("-fx-text-fill: #198754;");
        lblFormError.setText("✔ " + msg);
    }

    private void startBackgroundThread() {
        updaterThread = new StatusUpdaterThread(service, this::refreshTable);
        updaterThread.start();
        lblThreadStatus.setText("🟢 Auto-Updater Running");
    }

    public void shutdown() {
        if (updaterThread != null) updaterThread.stop();
    }

    // ── Sample seed data ───────────────────────────────────────────────────────
    private void seedSampleData() {
        service.submitComplaint("DEV", "CS-2024-001", "Academic",
                "Request for extra credit project in Data Structures.", Priority.MEDIUM, null);
        service.submitComplaint("UTSAV", "CS-2024-002", "Facility",
                "Laboratory air conditioning system needs repair.", Priority.HIGH, null);
        service.submitComplaint("DHROM", "CS-2024-003", "IT / Technical",
                "Inconsistent internet speeds in the central library.", Priority.LOW, null);
        service.submitComplaint("PRIT", "CS-2024-004", "Academic",
                "Seeking extension for the final internship report.", Priority.MEDIUM, null);
        service.submitComplaint("TIRTH", "CS-2024-005", "Financial",
                "Clarification needed on the semester fee breakdown.", Priority.LOW, null);
        service.submitComplaint("RENISH", "CS-2024-006", "Other",
                "Proposition for a new student-led hobby club.", Priority.MEDIUM, null);
    }
}
