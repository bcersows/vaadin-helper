package de.bcersows.vaadinhelper.helpers;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;

import de.bcersows.vaadinhelper.VaadinUiHelper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The notification helper allows easy access to notifications.
 * 
 * @author BCE
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationHelper {

    /** The display duration of notifications. **/
    private static final int DISPLAY_DURATION = 5000;

    /** If to show stack traces for error notifications. **/
    private static boolean showStackTrace;

    /** Set the configuration for the notifications. **/
    public static void setConfig(final boolean showStackTrace) {
        NotificationHelper.showStackTrace = showStackTrace;
    }

    /**
     * Notify the user about an event.
     * 
     * @param message
     *            the message
     * @param notificationMode
     *            the mode of the notification
     */
    public static void showNotification(@NonNull final String message, @NonNull final NotificationMode notificationMode) {
        configureAndDisplayNotification(new Notification(message, DISPLAY_DURATION), notificationMode);

    }

    /** Show an error. **/
    public static void showError(@NonNull final String message) {
        showNotification(message, NotificationMode.ERROR);
    }

    /** Show a success message. **/
    public static void showSuccess(@NonNull final String message) {
        showNotification(message, NotificationMode.SUCCESS);
    }

    /** Show a warning message. **/
    public static void showWarning(@NonNull final String message) {
        showNotification(message, NotificationMode.WARNING);
    }

    /**
     * Show an exception message. Will display a normal error message, if stack trace setting is false ({@link BaseConfigVaadin#debugException}), otherwise have
     * a full-width notification with a stack trace.
     * 
     * @param errorMessage
     *            the error message to always show
     * @param exception
     *            the exception
     */
    public static void showException(@NonNull final String errorMessage, @NonNull final Throwable exception) {
        if (showStackTrace) {
            // if showing the stack trace, display a custom notification
            final var stackTrace = new Pre(ExceptionUtils.getStackTrace(exception));
            stackTrace.setWidthFull();
            final var stackTraceWrapper = VaadinUiHelper.createPlainVertical(stackTrace);
            stackTraceWrapper.setMaxHeight("150px");
            stackTraceWrapper.getStyle().set("overflow", "auto");

            final var stackTraceDetails = new Details("Stack Trace", stackTraceWrapper);
            stackTraceDetails.getElement().getStyle().set("width", "calc(100vw - 100px)");

            final var notificationText = VaadinUiHelper.createVerticalWithoutPadding(new Label(errorMessage), stackTraceDetails);
            final var notificationContent = VaadinUiHelper.createHorizontalExpanded(notificationText);

            // create the notification
            final var notification = new Notification(notificationContent);
            notification.setPosition(Position.BOTTOM_STRETCH);

            // set up closing
            final var closeButton = new Button(VaadinIcon.CLOSE.create(), event -> notification.close());
            closeButton.setAutofocus(true);
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            notificationContent.add(closeButton);

            // display it
            configureAndDisplayNotification(notification, NotificationMode.ERROR);
        } else {
            // otherwise just show a regular error
            showError(errorMessage);
        }
    }

    /**
     * Notify the user about an event.
     * 
     * @param message
     *            the message
     * @param success
     *            if the message is a success or not
     */
    private static void showNotification(@NonNull final String message, final boolean success) {
        final NotificationMode notificationMode;
        if (success) {
            notificationMode = NotificationMode.SUCCESS;
        } else {
            notificationMode = NotificationMode.ERROR;
        }
        showNotification(message, notificationMode);
    }

    /** Configure the theme/UI of the given notification and display it in the UI. **/
    protected static void configureAndDisplayNotification(@NonNull final Notification notification, @NonNull final NotificationMode notificationMode) {
        // style the notification accordingly
        if (null != notificationMode.notificationVariant) {
            notification.addThemeVariants(notificationMode.notificationVariant);
        } else if (null != notificationMode.themeName) {
            notification.setThemeName(notificationMode.themeName);
        }
        // and display it
        notification.open();
    }

    /** The notification modes. **/
    @Getter
    @AllArgsConstructor
    public enum NotificationMode {
        /** Success. **/
        SUCCESS(NotificationVariant.LUMO_SUCCESS, null),
        /** Error. **/
        ERROR(NotificationVariant.LUMO_ERROR, null),
        /** Error. **/
        WARNING(null, "warning"),
        /** Default. */
        DEFAULT(NotificationVariant.LUMO_PRIMARY, null);

        /** The respective variant. **/
        @Nullable
        private final NotificationVariant notificationVariant;
        /** Or a theme name, if no variant. **/
        @Nullable
        private final String themeName;
    }
}
