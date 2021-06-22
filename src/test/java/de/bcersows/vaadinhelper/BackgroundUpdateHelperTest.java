package de.bcersows.vaadinhelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.server.VaadinSession;

import de.bcersows.vaadinhelper.BackgroundLoadingHelper.BackgroundLoadingException;
import de.bcersows.vaadinhelper.helpers.LogHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackgroundUpdateHelperTest {

    private final String input = "my input";

    @BeforeEach
    protected void setUp() throws Exception {
        MockVaadin.setup();
        final var attributes = Mockito.mock(RequestAttributes.class);
        RequestContextHolder.setRequestAttributes(attributes);

        UI.getCurrent().getSession().setErrorHandler(BackgroundOperationManagerTest.ERROR_HANDLER);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        MockVaadin.tearDown();
    }

    @Test
    public void testStartBackgroundOperation() throws Exception {
        final CountDownLatch countDownLatchLoading = new CountDownLatch(1);
        final CountDownLatch countDownLatchTest = new CountDownLatch(1);

        final var resultStorage = new AtomicReference<String>();

        final var sessionId = VaadinSession.getCurrent().getSession().getId();

        BackgroundUpdateHelper.startBackgroundUpdate(input, funcInput -> {
            final var mdcSessionId = MDC.get(LogHelper.SESSION_ID_KEY);
            assertNotNull(mdcSessionId, "the MDC was filled with the session ID");
            assertEquals(sessionId, mdcSessionId, "the MDC was filled with the session ID");

            assertEquals(input, funcInput, "input was passed correctly");

            // wait for the in-between check to finish
            try {
                countDownLatchLoading.await();
            } catch (final InterruptedException e) {
                // nothing
            }
            log.info("Returning result");

            assertNull(UI.getCurrent(), "there is no UI available here");

            return "result";
        }, result -> {
            // store result and continue with test
            resultStorage.set(result);
            log.info("Test ore");
            countDownLatchTest.countDown();
            log.info("Test post");

            assertNotNull(UI.getCurrent(), "there is a UI available here");
        });

        // wait for the bg loading thread to start a tiny bit
        Thread.sleep(100);

        // flush the access tasks
        VaadinTestHelper.flushVaadinAccessQueue();

        // test that UI was updated with loading thingies
        final var ui = UI.getCurrent();
        final var body = ui.getElement();

        // check that the loading indicators were added
        assertFalse(body.hasAttribute("background-operation-done"), "Body does not have the loaded attribute");

        final var children = VaadinTestHelper.extractRootStructure(ui);
        assertEquals(1, children.size(), "loading area added");
        assertEquals(Div.class, children.get(0).rootClass, "loading indicator area added - matching class");
        assertEquals(1, children.get(0).size(), "loading indicator added");
        assertEquals(ProgressBar.class, children.get(0).get(0).rootClass, "loading indicator added - matching class");

        log.info("Pre cd");

        // continue with loading
        countDownLatchLoading.countDown();

        log.info("Post cd");

        Thread.sleep(100);
        // flush the access tasks
        VaadinTestHelper.flushVaadinAccessQueue();

        // wait for result being written
        countDownLatchTest.await(3, TimeUnit.SECONDS);

        log.info("cont test");

        log.info("Cleared");

        // test result was received
        assertEquals("result", resultStorage.get(), "Result was received successfully");

        assertTrue(body.hasAttribute("background-operation-done"), "loaded attribute added to body");
        assertEquals(0, body.getChildCount(), "no children anymore");
    }

    @Test
    public void testStartBlockingBackgroundOperation() throws Exception {
        final CountDownLatch countDownLatchLoading = new CountDownLatch(1);
        final CountDownLatch countDownLatchTest = new CountDownLatch(1);

        final var resultStorage = new AtomicReference<String>();

        final var sessionId = VaadinSession.getCurrent().getSession().getId();

        BackgroundUpdateHelper.startBlockingBackgroundUpdate(input, funcInput -> {
            final var mdcSessionId = MDC.get(LogHelper.SESSION_ID_KEY);
            assertNotNull(mdcSessionId, "the MDC was filled with the session ID");
            assertEquals(sessionId, mdcSessionId, "the MDC was filled with the session ID");

            assertEquals(input, funcInput, "input was passed correctly");

            // wait for the in-between check to finish
            try {
                countDownLatchLoading.await();
            } catch (final InterruptedException e) {
                // nothing
            }
            log.info("Returning result");

            assertNull(UI.getCurrent(), "there is no UI available here");

            return "result";
        }, result -> {
            // store result and continue with test
            resultStorage.set(result);
            log.info("Test ore");
            countDownLatchTest.countDown();
            log.info("Test post");

            assertNotNull(UI.getCurrent(), "there is a UI available here");
        }, ex -> {
        });

        // wait for the bg loading thread to start a tiny bit
        Thread.sleep(100);

        // flush the access tasks
        VaadinTestHelper.flushVaadinAccessQueue();

        // test that UI was updated with loading thingies
        final var ui = UI.getCurrent();
        final var body = ui.getElement();

        // check that the loading indicators were added
        assertFalse(body.hasAttribute("background-operation-done"), "Body does not have the loaded attribute");

        final var children = VaadinTestHelper.extractRootStructure(ui);
        assertEquals(1, children.size(), "loading area added");
        final var loadingArea = children.get(0);
        assertEquals(Div.class, loadingArea.rootClass, "loading indicator area added - matching class");
        assertEquals(2, loadingArea.size(), "loading indicator added");
        assertEquals(ProgressBar.class, loadingArea.get(0).rootClass, "loading indicator added - matching class");
        assertEquals(Div.class, loadingArea.get(1).rootClass, "modal content added - matching class");
    }

    @Nested
    public class ErrorTest {
        @Test
        public void testBackgroundOperation_withError() throws InterruptedException {
            BackgroundUpdateHelper.startBackgroundUpdate(input, funcInput -> {
                throw new BackgroundLoadingException("could not finish bg", "error detected");
            }, result -> {
                log.info("Test post");
            });

            // wait and flush to be able to show the notification
            Thread.sleep(100);
            VaadinTestHelper.flushVaadinAccessQueue();
            Thread.sleep(100);
            VaadinTestHelper.flushVaadinAccessQueue();

            NotificationsKt.expectNotifications("error detected");
        }

        @Test
        public void testBackgroundOperation_withError_customHandler() throws InterruptedException {
            final var exceptionStorage = new AtomicReference<BackgroundLoadingException>();

            BackgroundUpdateHelper.startBackgroundUpdate(input, funcInput -> {
                throw new BackgroundLoadingException("could not finish bg", "error detected");
            }, result -> {
                log.info("Test post");
            }, exception -> {
                // check if UI available
                assertNotNull(UI.getCurrent(), "there is a UI available in error handler");

                // store exception
                exceptionStorage.set((BackgroundLoadingException) exception);
            });

            // wait and flush to be able to show the notification
            Thread.sleep(100);
            VaadinTestHelper.flushVaadinAccessQueue();
            Thread.sleep(100);
            VaadinTestHelper.flushVaadinAccessQueue();

            // there are no notifications
            NotificationsKt.expectNoNotifications();

            // check received exception
            final var foundException = exceptionStorage.get();
            assertNotNull(foundException, "Exception stored.");
            assertEquals("could not finish bg", foundException.getMessage());
            assertEquals("error detected", foundException.getUserMessage());
        }

        @Test
        public void testBackgroundOperation_with_Exception() throws InterruptedException {
            BackgroundUpdateHelper.startBackgroundUpdate(input, funcInput -> {
                throw new BackgroundLoadingException("could not finish bg", "could not finish bg.", new Exception());
            }, result -> {
                log.info("Test post");
            });

            // wait and flush to be able to show the notification
            Thread.sleep(100);
            VaadinTestHelper.flushVaadinAccessQueue();
            Thread.sleep(100);
            VaadinTestHelper.flushVaadinAccessQueue();
            NotificationsKt.expectNotifications("could not finish bg.");
        }
    }
}
