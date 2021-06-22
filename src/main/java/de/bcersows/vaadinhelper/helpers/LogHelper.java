package de.bcersows.vaadinhelper.helpers;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;

import com.vaadin.flow.server.VaadinSession;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Helper for logging-related methods.
 * 
 * @author BCE
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogHelper {
    /** The session ID key in the MDC. **/
    public static final String SESSION_ID_KEY = "sid";
    /** The session ID default if none exists. **/
    public static final String SESSION_ID_DEFAULT = "-";

    /**
     * Returns the current session ID (based on Vaadin).
     */
    @NonNull
    public static String getSessionId() {
        // get the current session and ID
        final var session = VaadinSession.getCurrent();
        if (null != session) {
            return session.getSession().getId();
        } else {
            return SESSION_ID_DEFAULT;
        }
    }

    /** Store the session ID in the MDC. **/
    public static void putSessionIdIntoMdc(@NonNull final String sessionId) {
        MDC.put(SESSION_ID_KEY, sessionId);
    }

    /** Remove any session ID from the MDC. **/
    public static void removeSessionIdFromMdc() {
        MDC.remove(SESSION_ID_KEY);
    }
}
