package de.bcersows.vaadinhelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinSession;

import de.bcersows.vaadinhelper.BackgroundOperationManager.BackgroundOperation;
import de.bcersows.vaadinhelper.BackgroundOperationManager.UpdateUiInterface;
import de.bcersows.vaadinhelper.BackgroundOperationManager.UpdateUiOnErrorInterface;
import de.bcersows.vaadinhelper.helpers.LogHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackgroundOperationManagerTest {

    public static final ErrorHandler ERROR_HANDLER = event -> log.error("Error: {}", event);

    @BeforeEach
    protected void setUp() throws Exception {
        MockVaadin.setup();
        final var attributes = Mockito.mock(RequestAttributes.class);
        RequestContextHolder.setRequestAttributes(attributes);

        UI.getCurrent().getSession().setErrorHandler(ERROR_HANDLER);
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

        final Supplier<String> testDataTask = () -> {
            final var mdcSessionId = MDC.get(LogHelper.SESSION_ID_KEY);
            assertNotNull(mdcSessionId, "the MDC was filled with the session ID");
            assertEquals(sessionId, mdcSessionId, "the MDC was filled with the session ID");

            // wait for the in-between check to finish
            try {
                countDownLatchLoading.await();
            } catch (final InterruptedException e) {
                // nothing
            }
            log.info("Returning result");

            assertNull(UI.getCurrent(), "there is no UI available here");

            return "result";
        };
        final UpdateUiInterface<String> updateUiTask = result -> {
            // store result and continue with test
            resultStorage.set(result);
            log.info("Test ore");
            countDownLatchTest.countDown();
            log.info("Test post");

            assertNotNull(UI.getCurrent(), "there is a UI available here");
        };
        final UpdateUiOnErrorInterface updateUiOnErrorTask = exception -> {

        };
        final var operation = new BackgroundTestOperation(testDataTask, updateUiTask, updateUiOnErrorTask);
        operation.startBackgroundOperation();

    }

    private static class BackgroundTestOperation extends BackgroundOperation<String> {
        protected BackgroundTestOperation(@NonNull final Supplier<String> testDataTask, @NonNull final UpdateUiInterface<String> updateUiTask,
                @Nullable final UpdateUiOnErrorInterface updateUiOnErrorTask) {
            super(testDataTask::get, updateUiTask, updateUiOnErrorTask);
        }
    }
}
