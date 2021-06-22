package de.bcersows.vaadinhelper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.vaadin.flow.dom.Element;

import de.bcersows.vaadinhelper.helpers.LogHelper;
import de.bcersows.vaadinhelper.helpers.NotificationHelper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * The base class for background operations.
 * 
 * @author BCE
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BackgroundOperationManager {
    /** ID for the loading area. **/
    protected static final String ID_LOADING_AREA = "bg-loading-area";
    /** ID for the loading indicator. **/
    protected static final String ID_LOADING_INDICATOR = "bg-loading-indicator";
    /** Attribute indicating a load in progress. **/
    protected static final String ATTRIBUTE_BACKGROUND_OPERATION = "background-operation";
    /** Attribute indicating the background operation is finished. **/
    public static final String ATTRIBUTE_BACKGROUND_OPERATION_DONE = "background-operation-done";

    /** Interface for handling the data in the background. **/
    @FunctionalInterface
    public static interface DataHandlingInterface<R> {
        /**
         * Load the data.
         * 
         * @return the loaded data, can be {@code null}
         * @throws RestServiceException
         *             if an exception happened during the rest call.
         **/
        @Nullable
        R handleData() throws RestServiceException;
    }

    /** Interface for updating the UI after loading. **/
    @FunctionalInterface
    public static interface UpdateUiInterface<R> {
        /** Update the UI with the given data. **/
        void updateUi(@Nullable R loadedData);
    }

    /** Interface for doing stuff with the UI in case of an error. **/
    @FunctionalInterface
    public static interface UpdateUiOnErrorInterface {
        /** Allows to do stuff on the UI after an error. **/
        void runOnUiAfterError(@NonNull RestServiceException restServiceException);
    }

    /**
     * Base background operation blueprint.
     * 
     * @author BCE
     */
    protected abstract static class BackgroundOperation<R> {
        /** The task to handle data. **/
        @NonNull
        private final DataHandlingInterface<R> dataHandlingTask;
        /** The task to update the UI on success. **/
        @NonNull
        private final UpdateUiInterface<R> updateUiTask;
        /** The task to update the UI on error. **/
        @Nullable
        private final UpdateUiOnErrorInterface updateUiOnErrorTask;

        /**
         * Create an instance.
         * 
         * @param dataHandlingTask
         *            task/provider for data handling<br/>
         *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
         * @param updateUiTask
         *            task/consumer to display the UI with the loaded data
         * @param updateUiOnErrorTask
         *            if provided, will be called in the error case instead of showing a notification. Allows for modification on the UI.
         */
        protected BackgroundOperation(@NonNull final DataHandlingInterface<R> dataHandlingTask, @NonNull final UpdateUiInterface<R> updateUiTask,
                @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
            this.dataHandlingTask = dataHandlingTask;
            this.updateUiTask = updateUiTask;
            this.updateUiOnErrorTask = updateUiOnErrorTask;
        }

        /**
         * Start a task for background data loading. Will show a custom loading indicator.<br/>
         * Please be well aware of when the UI is available and when not!
         * 
         * @param <R>
         *            the data type
         */
        public final void startBackgroundOperation() {
            // store the current UI
            final var ui = UI.getCurrent();

            // if there's no UI, throw an error.
            if (null == ui) {
                throw new IllegalStateException("Background operation must be started from a valid UI location.");
            }

            // store request context data
            final var authentication = SecurityContextHolder.getContext().getAuthentication();
            final var requestContext = RequestContextHolder.currentRequestAttributes();

            final var sessionId = LogHelper.getSessionId();

            // create a thread to load the data...
            final var bgLoadingThread = new Thread(() -> {
                // set the actual request context data
                SecurityContextHolder.getContext().setAuthentication(authentication);
                RequestContextHolder.setRequestAttributes(requestContext);
                LogHelper.putSessionIdIntoMdc(sessionId);

                // create the progress bar and add it to the UI
                final var loadingIndicator = new ProgressBar();
                loadingIndicator.setId(ID_LOADING_INDICATOR);

                final var loadingArea = new Div(loadingIndicator);
                loadingArea.setId(ID_LOADING_AREA);

                ui.access(() -> {
                    final var page = ui.getElement();
                    page.setAttribute(ATTRIBUTE_BACKGROUND_OPERATION, true);
                    preOperationSetup(page, loadingArea);
                    page.removeAttribute(ATTRIBUTE_BACKGROUND_OPERATION_DONE);
                    ui.add(loadingArea);
                });

                // create/start a timer increasing the loading indicator to fake progress
                final var cancelTimer = createUpdateTimer(ui, loadingIndicator);

                try {
                    // load the data
                    final var loadedData = dataHandlingTask.handleData();

                    // start the UI update (after verifying)
                    ui.access(() -> {
                        // last step indicator
                        loadingIndicator.setValue(0.9);

                        // check if data was ok
                        final var continueDisplaying = postDataLoad(loadedData);
                        if (!continueDisplaying) {
                            // mark the indicator with error
                            loadingIndicator.addThemeVariants(ProgressBarVariant.LUMO_ERROR);
                            return;
                        }

                        // loaded data valid, update the progress bar...
                        loadingIndicator.removeThemeVariants(ProgressBarVariant.LUMO_ERROR);
                        loadingIndicator.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);

                        // ... and then the UI
                        updateUiTask.updateUi(loadedData);
                        loadingIndicator.setValue(1.0);

                        ui.getElement().setAttribute(ATTRIBUTE_BACKGROUND_OPERATION_DONE, true);
                    });
                } catch (final RestServiceException restServiceException) {
                    log.debug("Background loading exception detected!", restServiceException);

                    // if a custom exception handler was passed, use it; otherwise show a notification
                    ui.access(() -> {
                        if (null != updateUiOnErrorTask) {
                            // error task, call it
                            updateUiOnErrorTask.runOnUiAfterError(restServiceException);
                        } else {
                            // show notification with the user message
                            NotificationHelper.showException(restServiceException.getUserMessage(), restServiceException);
                        }

                        // add the background loaded attribute, even tho an error happened
                        ui.getElement().setAttribute(ATTRIBUTE_BACKGROUND_OPERATION_DONE, true);
                    });
                } catch (final Exception e) {
                    log.error("Could not finish background operation.", e);
                } finally {
                    // cancel timer
                    cancelTimer.run();

                    // clean up UI again
                    ui.access(() -> {
                        final var page = ui.getElement();
                        page.removeAttribute(ATTRIBUTE_BACKGROUND_OPERATION);
                        postOperationTeardown(page);
                        ui.remove(loadingArea);
                    });
                }
            });
            // ... and start it
            bgLoadingThread.setName("background-data-operation");
            bgLoadingThread.start();
        }

        /**
         * Can update the page after finishing the operation.
         */
        protected void postOperationTeardown(@NonNull final Element page) {
            // nothing as default
        }

        /**
         * Can update the page or loading area before starting the operation.
         */
        protected void preOperationSetup(@NonNull final Element page, @NonNull final Div loadingArea) {
            // nothing as default
        }

        /**
         * Check the loaded data, and return if it's valid and can be displayed.
         */
        protected boolean postDataLoad(@NonNull final R loadedData) {
            // return true as default
            return true;
        }

        /**
         * Create a timer that updates the loading indicator regularly. Returns a runnable that cancels the timer.
         */
        @NonNull
        private Runnable createUpdateTimer(@NonNull final UI ui, @NonNull final ProgressBar loadingIndicator) {
            // counter to store the current fake task progress
            final var progressCounter = new AtomicInteger();
            // create the timer task to update the loading indicator
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (loadingIndicator.getValue() < 0.9) {
                        ui.access(() -> loadingIndicator.setValue(progressCounter.incrementAndGet() / 10.0));
                    } else {
                        // cancel itself and set to error variation
                        ui.access(() -> loadingIndicator.addThemeVariants(ProgressBarVariant.LUMO_ERROR));
                        this.cancel();
                    }
                }
            };
            // create the timer and schedule the task
            final Timer timer = new Timer("BackgroundOperationUpdateTimer");
            timer.scheduleAtFixedRate(task, 0, 1000l);

            // return the runnable to cancel the task
            return () -> {
                task.cancel();
                timer.cancel();
            };
        }
    }

    /**
     * Base exception for problems occurring during REST calls.
     * 
     * @author BCE
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class RestServiceException extends Exception {
        private static final long serialVersionUID = 1L;

        /** The message shown to the user. **/
        private final String userMessage;

        /** Create an instance with only a message. **/
        public RestServiceException(@NonNull final String message, @NonNull final String userMessage) {
            this(message, userMessage, null);
        }

        /** Create an instance including a cause. **/
        public RestServiceException(@NonNull final String message, @NonNull final String userMessage, @Nullable final Throwable cause) {
            super(message, cause);

            this.userMessage = userMessage;
        }

    }
}
