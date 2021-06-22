package de.bcersows.vaadinhelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinService;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class VaadinTestHelper {

    /**
     * Prepare the test for background loading. {@code MockVaadin.setup()} must be called before using this. Best to use in a {@code @BeforeEach} method.
     * 
     * @see #waitForBackgroundLoad()
     */
    public static void prepareBackgroundLoading() {
        // fake that currently in a request
        final var attributes = Mockito.mock(RequestAttributes.class);
        RequestContextHolder.setRequestAttributes(attributes);

        // set a custom error handler for debugging
        UI.getCurrent().getSession().setErrorHandler(new ErrorHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public void error(final ErrorEvent event) {
                log.error("Error in Vaadin: {}", event.toString(), event.getThrowable());
            }
        });
    }

    /**
     * Wait until a background load has finished. Will fail after 5s of no success.<br/>
     * Normally has to prepare for a background load with {@link #prepareBackgroundLoading()}.
     * 
     * @see #prepareBackgroundLoading()
     */
    public static void waitForBackgroundLoad() {
        waitForBackgroundLoad(50);
    }

    /**
     * Wait until a background load has finished. Will fail after (n*10)s of no success.<br/>
     * Normally has to prepare for a background load with {@link #prepareBackgroundLoading()}.
     * 
     * @see #prepareBackgroundLoading()
     */
    @SuppressWarnings({ "java:S106", "java:S2142" })
    public static void waitForBackgroundLoad(final int timeout) {
        final var loadingCounter = new AtomicInteger();

        log.info("Waiting for BG load.");
        final var bodyElement = UI.getCurrent().getElement();

        // loop until the "loaded" attribute is set
        while (!bodyElement.hasAttribute(BackgroundOperationManager.ATTRIBUTE_BACKGROUND_OPERATION_DONE)) {
            System.out.println(".".repeat(loadingCounter.get()) + " - waiting");
            try {
                // flush the access tasks
                VaadinTestHelper.flushVaadinAccessQueue();
                if (loadingCounter.incrementAndGet() > timeout) {
                    throw new IllegalStateException("Waiting for BG task took to long.");
                }
            } catch (final InterruptedException e) {
                log.error("Interrupted wait.");
            }
        }

        log.info("Finished BG loading.");
    }

    /**
     * If testing using {@code Karibu}, use this method to flush the queued {@link UI#access(com.vaadin.flow.server.Command)} calls.
     */
    public static void flushVaadinAccessQueue() throws InterruptedException {
        final var session = UI.getCurrent().getSession();
        final var service = VaadinService.getCurrent();

        log.info("Pending access: {}", session.getPendingAccessQueue().size());
        service.runPendingAccessTasks(session);

        // wait for all sessions to be cleared
        final var loopCounter = new AtomicInteger();
        while (!session.getPendingAccessQueue().isEmpty()) {
            log.info("Wait for access queue");
            Thread.sleep(100);

            // if too many, break out
            if (loopCounter.incrementAndGet() > 10) {
                log.debug("Break out.");
                break;
            }
        }

        log.info("Wait for access queue - outer");
        Thread.sleep(100);
    }

    /**
     * Extract the component structure of the given root into an easily-queryable structure.
     */
    public static ComponentChildren extractRootStructure(@NonNull final Component root) {
        final var children = root.getChildren().map(VaadinTestHelper::extractRootStructure).collect(Collectors.toList());
        return new ComponentChildren(root, root.getClass(), children);
    }

    @Data
    @AllArgsConstructor
    public static class ComponentChildren {
        @NonNull
        public final Component root;
        @NonNull
        public final Class<? extends Component> rootClass;
        @NonNull
        public final List<ComponentChildren> children;

        /** Get first child. **/
        public ComponentChildren getFirst() {
            return children.get(0);
        }

        /** Get first child component. **/
        public Component getFirstComponent() {
            return getFirst().root;
        }

        /** Get last child. **/
        public ComponentChildren getLast() {
            return children.get(size() - 1);
        }

        /** Get last child component. **/
        public Component getLastComponent() {
            return getLast().root;
        }

        /** Get child with index. **/
        public ComponentChildren get(final int index) {
            return children.get(index);
        }

        /** Get the amount of children. **/
        public int size() {
            return this.children.size();
        }

        public boolean hasChildren() {
            return !this.children.isEmpty();
        }
    }
}
