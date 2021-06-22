package de.bcersows.vaadinhelper.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.server.VaadinSession;

public class LogHelperTest {

    @Test
    public void testPutSessionIdIntoMdc() throws Exception {
        assertNull(MDC.get(LogHelper.SESSION_ID_KEY), "no session ID yet");

        LogHelper.putSessionIdIntoMdc("mysessionid");

        assertEquals("mysessionid", MDC.get(LogHelper.SESSION_ID_KEY), "session ID set and matches");

        LogHelper.removeSessionIdFromMdc();

        assertNull(MDC.get(LogHelper.SESSION_ID_KEY), "session ID empty again");
    }

    @Test
    public void testGetSessionId() throws Exception {
        try {
            MockVaadin.setup();

            final var sessionId = VaadinSession.getCurrent().getSession().getId();

            assertEquals(sessionId, LogHelper.getSessionId(), "session ID matches");

            VaadinSession.setCurrent(null);

            assertEquals("-", LogHelper.getSessionId());

        } finally {
            MockVaadin.tearDown();
        }
    }
}
