package de.bcersows.vaadinhelper.helpers;

import static com.github.mvysny.kaributesting.v10.NotificationsKt.expectNotifications;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification.Position;

import de.bcersows.vaadinhelper.VaadinTestHelper;

public class NotificationHelperTest {

    @BeforeEach
    public void setUp() throws Exception {
        MockVaadin.setup();
    }

    @Test
    public void testShowNotification() {

        final String message = "Test notification";

        NotificationHelper.showSuccess(message);
        expectNotifications(message);

        NotificationHelper.showError(message);
        expectNotifications(message);

        NotificationHelper.showWarning(message);
        expectNotifications(message);
    }

    @Test
    public void testShowException() {
        // enable stack trace debug
        NotificationHelper.setConfig(true);

        final var message = "oh no";
        final var exception = new IllegalStateException("darn");

        NotificationHelper.showException(message, exception);

        final var notifications = NotificationsKt.getNotifications();
        assertEquals(1, notifications.size(), "One notification.");
        final var not = notifications.get(0);
        assertTrue(not.hasThemeName("error"), "has error theme");
        assertEquals(Position.BOTTOM_STRETCH, not.getPosition(), "normal notification, as not displaying stack trace");

        final var structure = VaadinTestHelper.extractRootStructure(not);

        assertEquals(1, structure.size(), "children amount (hor)");
        assertEquals(2, structure.getFirst().size(), "children amount (ver + button)");
        assertEquals(2, structure.getFirst().getFirst().size(), "children amount (msg + details)");
        assertEquals(Label.class, structure.getFirst().getFirst().getFirstComponent().getClass(), "message");
        assertEquals(message, ((Label) structure.getFirst().getFirst().getFirstComponent()).getText(), "message");
        assertEquals(Details.class, structure.getFirst().getFirst().getLastComponent().getClass(), "stack trace");
    }

    @Test
    public void testShowException_noStackTrace() {
        NotificationHelper.setConfig(false);

        final var message = "oh no";
        final var exception = new IllegalStateException("darn");

        NotificationHelper.showException(message, exception);

        final var notifications = NotificationsKt.getNotifications();
        assertEquals(1, notifications.size(), "One notification.");
        final var not = notifications.get(0);
        assertTrue(not.hasThemeName("error"), "has error theme");
        assertEquals(Position.BOTTOM_START, not.getPosition(), "normal notification, as not displaying stack trace");

    }

}
