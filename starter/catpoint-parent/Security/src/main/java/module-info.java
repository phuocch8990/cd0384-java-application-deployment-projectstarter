module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires miglayout;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;
    opens com.udacity.catpoint.security.data to com.google.gson;
}