module ca.kieve.sologjm {
    requires java.base;
    requires java.desktop;

    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires jakarta.xml.bind;
    requires org.glassfish.jaxb.runtime;
    requires org.audiveris.proxymusic;

    exports ca.kieve.sologjm;
    opens ca.kieve.sologjm;
}
