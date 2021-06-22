package de.bcersows.vaadinhelper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.select.Select;

import de.bcersows.vaadinhelper.EventHandlerHelper.ComponentEventHandler;
import de.bcersows.vaadinhelper.EventHandlerHelper.ValueChangedEventHandler;

public class EventHandlerHelperTest {
    @Test
    public void testListener() throws Exception {
        final var helper = new ComponentEventHandler<Label, ClickEvent<Label>, ComponentEventListener<ClickEvent<Label>>>();

        final var e1Listener = new AtomicBoolean();
        final var e2Listener = new AtomicBoolean();

        // attach listeners
        final var r1 = helper.addListener(event -> e1Listener.set(true));
        final var r2 = helper.addListener(event -> e2Listener.set(true));

        // send an event
        final var label = new Label();
        helper.fireEvent(new ClickEvent<>(label));

        assertTrue(e1Listener.get(), "e1 fired");
        assertTrue(e2Listener.get(), "e2 fired");

        // de-register one
        r2.remove();

        // send another event (reset before)
        e1Listener.set(false);
        e2Listener.set(false);

        helper.fireEvent(new ClickEvent<>(label));
        assertTrue(e1Listener.get(), "e1 fired");
        assertFalse(e2Listener.get(), "e2 not fired");
    }

    @Test
    public void testChangedHandler() throws Exception {
        final var helper = new ValueChangedEventHandler<Select<String>, TestValueChangedEvent, ValueChangeListener<TestValueChangedEvent>>();

        final var e1Listener = new AtomicBoolean();
        final var e2Listener = new AtomicBoolean();

        // attach listeners
        final var r1 = helper.addListener(event -> e1Listener.set(true));
        final var r2 = helper.addListener(event -> e2Listener.set(true));

        // send an event
        final var source = new Select<String>();
        helper.fireEvent(new TestValueChangedEvent(source));

        assertTrue(e1Listener.get(), "e1 fired");
        assertTrue(e2Listener.get(), "e2 fired");

        // de-register one
        r2.remove();

        // send another event (reset before)
        e1Listener.set(false);
        e2Listener.set(false);

        helper.fireEvent(new TestValueChangedEvent(source));
        assertTrue(e1Listener.get(), "e1 fired");
        assertFalse(e2Listener.get(), "e2 not fired");
    }

    private static class TestValueChangedEvent extends ComponentValueChangeEvent<Select<String>, String> {
        private static final long serialVersionUID = 1L;

        /** Create the event. **/
        public TestValueChangedEvent(final Select<String> source) {
            super(source, source, null, false);
        }
    }
}
