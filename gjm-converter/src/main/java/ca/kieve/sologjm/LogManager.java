package ca.kieve.sologjm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LogManager {
    public interface Listener {
        void onMessageAdded(String message);
    }

    private static LogManager m_instance;

    public static LogManager getInstance() {
        if (m_instance == null) {
            init();
        }
        return m_instance;
    }

    private static synchronized void init() {
        if (m_instance == null) {
            m_instance = new LogManager();
        }
    }

    private final Set<Listener> m_listeners;
    private final List<String> m_messages;

    private LogManager() {
        m_listeners = new HashSet<>();
        m_messages = new ArrayList<>(50);
    }

    public synchronized void addMessage(String message, Throwable throwable) {
        addMessage(message);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        addMessage(stringWriter.toString());
    }

    public synchronized void addMessage(String message) {
        m_messages.add(message);
        for (Listener listener : m_listeners) {
            listener.onMessageAdded(message);
        }
    }

    public synchronized void addListener(Listener listener) {
        m_listeners.add(listener);
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(m_messages);
    }
}
