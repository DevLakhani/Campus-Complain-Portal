module com.campus.complaint {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;
    requires java.desktop;
    requires com.github.librepdf.openpdf;

    opens com.campus.complaint to javafx.fxml;
    opens com.campus.complaint.controller to javafx.fxml;
    opens com.campus.complaint.model to javafx.base;
    opens com.campus.complaint.service to javafx.base;

    exports com.campus.complaint;
    exports com.campus.complaint.model;
    exports com.campus.complaint.service;
    exports com.campus.complaint.controller;
}
