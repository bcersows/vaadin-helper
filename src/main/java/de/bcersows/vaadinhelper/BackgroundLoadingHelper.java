package de.bcersows.vaadinhelper;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Helper to allow background loading.
 * 
 * @author bcersows
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackgroundLoadingHelper extends BackgroundOperationManager {

    /** The CSS class name that will allow for UI blurring during loading. **/
    public static final String STYLE_CLASS_BG_LOADING_CONTENT = "bg-loading-content";
    /** Attribute indicating a background load is in progress. **/
    private static final String ATTRIBUTE_BACKGROUND_LOADING = "background-loading";

    /**
     * Start a task for background data loading. Will show a custom loading indicator.<br/>
     * Please be well aware of when the UI is available and when not!
     * 
     * @param <R>
     *            the data type
     * @param loadDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     */
    public static <R> void startBackgroundLoading(@NonNull final DataLoadingInterface<R> loadDataTask, @NonNull final UpdateUiInterface<R> updateUiTask) {
        // start the loading and will only display a notification on error.
        startBackgroundLoading(loadDataTask, updateUiTask, (UpdateUiOnErrorInterface) null);
    }

    /**
     * Start a task for background data loading. Will show a custom loading indicator.<br/>
     * Please be well aware of when the UI is available and when not!
     * 
     * @param <R>
     *            the data type
     * @param loadDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     * @param updateUiOnErrorTask
     *            if provided, will be called in the error case instead of showing a notification. Allows for modification on the UI.
     */
    public static <R> void startBackgroundLoading(@NonNull final DataLoadingInterface<R> loadDataTask, @NonNull final UpdateUiInterface<R> updateUiTask,
            @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
        // start without data validation
        startBackgroundLoading(loadDataTask, updateUiTask, null, updateUiOnErrorTask);
    }

    /**
     * Start a task for background data loading. Will show a custom loading indicator.<br/>
     * Please be well aware of when the UI is available and when not!
     * 
     * @param <R>
     *            the data type
     * @param loadDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     * @param dataCheckingTask
     *            allows to check the loaded data before updating the UI. <strong>Allows UI access, e.g. for forwarding the request to another view, or to
     *            display notifications.</strong><br/>
     *            As this is run on the UI, it should only perform simple checks.
     */
    public static <R> void startBackgroundLoading(@NonNull final DataLoadingInterface<R> loadDataTask, @NonNull final UpdateUiInterface<R> updateUiTask,
            @Nullable final DataCheckingInterface<R> dataCheckingTask) {
        // start without custom error handling
        startBackgroundLoading(loadDataTask, updateUiTask, dataCheckingTask, null);
    }

    /**
     * Start a task for background data loading. Will show a custom loading indicator.<br/>
     * Please be well aware of when the UI is available and when not!
     * 
     * @param <R>
     *            the data type
     * @param loadDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     * @param dataCheckingTask
     *            allows to check the loaded data before updating the UI. <strong>Allows UI access, e.g. for forwarding the request to another view, or to
     *            display notifications.</strong><br/>
     *            As this is run on the UI, it should only perform simple checks.
     * @param updateUiOnErrorTask
     *            if provided, will be called in the error case instead of showing a notification. Allows for modification on the UI.
     */
    public static <R> void startBackgroundLoading(@NonNull final DataLoadingInterface<R> loadDataTask, @NonNull final UpdateUiInterface<R> updateUiTask,
            @Nullable final DataCheckingInterface<R> dataCheckingTask, @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
        final var backgroundLoadingOperation = new BackgroundLoadingOperation<>(loadDataTask, updateUiTask, dataCheckingTask, updateUiOnErrorTask);
        backgroundLoadingOperation.startBackgroundOperation();
    }

    /** Interface for loading the data in the background. **/
    @FunctionalInterface
    public static interface DataLoadingInterface<R> {
        /**
         * Load the data.
         * 
         * @return the loaded data, can be {@code null}
         * @throws BackgroundLoadingException
         *             if an exception happened while loading; the container {@link BackgroundLoadingException#userMessage} will be displayed to the user
         * @throws RestServiceException
         *             if an exception happened during the rest call.
         **/
        @Nullable
        R loadData() throws BackgroundLoadingException, RestServiceException;
    }

    /** Interface for checking the loaded data fulfills the requested requirements. **/
    @FunctionalInterface
    public static interface DataCheckingInterface<R> {
        /**
         * Verify the loaded data.
         * 
         * @return if the loaded data is valid according to the specific checks
         **/
        boolean checkLoadedData(@Nullable R loadedData);
    }

    /**
     * Background loading implementation of a background operation.
     * 
     * @author bcersows
     */
    private static class BackgroundLoadingOperation<R> extends BackgroundOperation<R> {
        /** Task for checking the loading data. **/
        @Nullable
        private final DataCheckingInterface<R> dataCheckingTask;

        public BackgroundLoadingOperation(@NonNull final DataLoadingInterface<R> loadDataTask, @NonNull final UpdateUiInterface<R> updateUiTask,
                @Nullable final DataCheckingInterface<R> dataCheckingTask, @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
            super(loadDataTask::loadData, updateUiTask, updateUiOnErrorTask);

            this.dataCheckingTask = dataCheckingTask;
        }

        @Override
        protected boolean postDataLoad(final R loadedData) {
            // check the loaded data
            return
            /// ok if no checker...
            null == dataCheckingTask
                    /// or check was valid
                    || dataCheckingTask.checkLoadedData(loadedData);
        }

        @Override
        protected void preOperationSetup(final Element page, final Div loadingArea) {
            page.setAttribute(ATTRIBUTE_BACKGROUND_LOADING, true);
        }

        @Override
        protected void postOperationTeardown(final Element page) {
            page.removeAttribute(ATTRIBUTE_BACKGROUND_LOADING);
        }
    }

    /** An exception that happened while loading data in the background. **/
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class BackgroundLoadingException extends RestServiceException {
        private static final long serialVersionUID = 1L;

        /** Create an instance. **/
        public BackgroundLoadingException(@NonNull final String message, @NonNull final String userMessage) {
            this(message, userMessage, null);
        }

        /** Create an instance including a cause. **/
        public BackgroundLoadingException(@NonNull final String message, @NonNull final String userMessage, @Nullable final Throwable cause) {
            super(message, userMessage, cause);

        }

    }
}
