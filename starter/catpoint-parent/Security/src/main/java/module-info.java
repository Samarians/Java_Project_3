module com.udacity.catpoint.security {
    requires java.desktop;
    requires com.miglayout.swing;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires com.udacity.catpoint.image;

    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.application;
    opens com.udacity.catpoint.security.data to com.google.gson;
}