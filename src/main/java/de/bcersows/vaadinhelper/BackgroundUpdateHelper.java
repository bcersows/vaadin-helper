package de.bcersows.vaadinhelper;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.dom.Element;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A helper for background operations that write data.
 * 
 * @author bcersows
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BackgroundUpdateHelper extends BackgroundOperationManager {
    /** The ID of the modal. **/
    protected static final String ID_MODAL = "bg-loading-modal";
    /** The ID of the modal label. **/
    protected static final String ID_MODAL_CONTENT = "bg-loading-modal-content";

    /**
     * Start a task for background data update. Will show a custom loading indicator.
     * 
     * @param <V>
     *            the input data type
     * @param <R>
     *            the return data type
     * @param input
     *            the input data, if any
     * @param updateDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     */
    public static <V, R> void startBackgroundUpdate(@Nullable final V input, @NonNull final DataUpdateInterface<V, R> updateDataTask,
            @NonNull final UpdateUiInterface<R> updateUiTask) {
        startBackgroundUpdate(input, updateDataTask, updateUiTask, null);
    }

    /**
     * Start a task for background data update. Will show a custom loading indicator.
     * 
     * @param <V>
     *            the input data type
     * @param <R>
     *            the return data type
     * @param input
     *            the input data, if any
     * @param updateDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     * @param updateUiOnErrorTask
     *            if provided, will be called in the error case instead of showing a notification. Allows for modification on the UI.
     */
    public static <V, R> void startBackgroundUpdate(@Nullable final V input, @NonNull final DataUpdateInterface<V, R> updateDataTask,
            @NonNull final UpdateUiInterface<R> updateUiTask, @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
        startBackgroundUpdate(input, updateDataTask, updateUiTask, updateUiOnErrorTask, null);
    }

    /**
     * Start a task for background data update. Will show a custom loading indicator and block the UI.
     * 
     * @param <V>
     *            the input data type
     * @param <R>
     *            the return data type
     * @param input
     *            the input data, if any
     * @param updateDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     * @param updateUiOnErrorTask
     *            if provided, will be called in the error case instead of showing a notification. Allows for modification on the UI.
     */
    public static <V, R> void startBlockingBackgroundUpdate(@Nullable final V input, @NonNull final DataUpdateInterface<V, R> updateDataTask,
            @NonNull final UpdateUiInterface<R> updateUiTask, @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
        startBackgroundUpdate(input, updateDataTask, updateUiTask, updateUiOnErrorTask, BackgroundUpdateConfiguration.withModal());
    }

    /**
     * Start a task for background data update. Will show a custom loading indicator.
     * 
     * @param <V>
     *            the input data type
     * @param <R>
     *            the return data type
     * @param input
     *            the input data, if any
     * @param updateDataTask
     *            task/provider to actually load the data<br/>
     *            <strong>This task shall not access the UI elements in any way! In case of a problem, throw the appropriate exception!</strong>
     * @param updateUiTask
     *            task/consumer to display the UI with the loaded data
     * @param updateUiOnErrorTask
     *            if provided, will be called in the error case instead of showing a notification. Allows for modification on the UI.
     * @param backgroundUpdateConfiguration
     *            the configuration of the update
     */
    public static <V, R> void startBackgroundUpdate(@Nullable final V input, @NonNull final DataUpdateInterface<V, R> updateDataTask,
            @NonNull final UpdateUiInterface<R> updateUiTask, @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask,
            @Nullable final BackgroundUpdateConfiguration backgroundUpdateConfiguration) {
        final var backgroundLoadingOperation = new BackgroundWriteOperation<>(input, updateDataTask, updateUiTask, updateUiOnErrorTask,
                backgroundUpdateConfiguration);
        backgroundLoadingOperation.startBackgroundOperation();
    }

    /** Interface for updating the data in the background. **/
    @FunctionalInterface
    public static interface DataUpdateInterface<V, R> {
        /**
         * Update the data.
         * 
         * @param input
         *            the input
         * @return the loaded data, can be {@code null}
         * @throws RestServiceException
         *             if an exception happened during the rest call.
         **/
        @Nullable
        R updateData(@Nullable V input) throws RestServiceException;
    }

    /**
     * Implementation of a background operation.
     * 
     * @author bcersows
     */
    private static class BackgroundWriteOperation<V, R> extends BackgroundOperation<R> {
        /** The configuration of the operation. **/
        private final BackgroundUpdateConfiguration backgroundUpdateConfiguration;

        /** Create the operation. **/
        public BackgroundWriteOperation(@Nullable final V input, @NonNull final DataUpdateInterface<V, R> updateDataTask,
                @NonNull final UpdateUiInterface<R> updateUiTask, @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask,
                @Nullable final BackgroundUpdateConfiguration backgroundUpdateConfiguration) {
            super(() -> updateDataTask.updateData(input), updateUiTask, updateUiOnErrorTask);

            this.backgroundUpdateConfiguration = backgroundUpdateConfiguration;
        }

        @Override
        protected void preOperationSetup(final Element page, final Div loadingArea) {
            // if configured to to so, block the UI using a modal
            if (null != this.backgroundUpdateConfiguration && this.backgroundUpdateConfiguration.blockUi) {
                final var modalLabel = new Label(this.backgroundUpdateConfiguration.wipMessage);
                modalLabel.setId(ID_MODAL_CONTENT);

                final var modal = new Div(modalLabel);
                modal.setId(ID_MODAL);
                loadingArea.add(modal);
            }
        }
    }

    /** The configuration of the update. **/
    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BackgroundUpdateConfiguration {
        /** The WIP message. **/
        @NonNull
        private final String wipMessage;
        /** If to show a modal. **/
        private final boolean blockUi;

        /** Show a modal with default text. **/
        @NonNull
        public static BackgroundUpdateConfiguration withModal() {
            return withModal("Working...");
        }

        /** Show a modal with given text. **/
        @NonNull
        public static BackgroundUpdateConfiguration withModal(@NonNull final String wipMessage) {
            return new BackgroundUpdateConfiguration(wipMessage, true);
        }
    }
}
