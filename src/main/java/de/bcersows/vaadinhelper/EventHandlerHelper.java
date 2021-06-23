package de.bcersows.vaadinhelper;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.NonNull;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.shared.Registration;

/**
 * Helper for components to handler events and listeners without relying on {@link Component#addListener}. If the event type is not generic, or DOM
 * functionality is required, then the {@link Component} version can be used.
 * 
 * @author bcersows
 */
public abstract class EventHandlerHelper<COMPONENT extends Component, EVENT extends ComponentEvent<COMPONENT>, TYPE extends EventListener> {
    /** Listeners for component events. **/
    private final Set<TYPE> componentListeners = new HashSet<>();

    /**
     * Register a listener.
     * 
     * @return the registration to remove the listener later
     **/
    @NonNull
    public Registration addListener(@NonNull final TYPE eventListener) {
        this.componentListeners.add(Objects.requireNonNull(eventListener));

        return Registration.once(() -> this.componentListeners.remove(eventListener));
    }

    /**
     * Fire the given event.
     * 
     * @param event
     *            the event to fire
     */
    public void fireEvent(@NonNull final EVENT event) {
        this.componentListeners.iterator().forEachRemaining(listener -> handleEvent(listener, event));
    }

    /** Handle the given event. Must be implemented by the actual instances. **/
    protected abstract void handleEvent(@NonNull TYPE listener, @NonNull EVENT event);

    /**
     * An instance of the event handler for {@link ComponentEvent}s.
     * 
     * @author bcersows
     */
    public static class ComponentEventHandler<COMPONENT extends Component, EVENT extends ComponentEvent<COMPONENT>, TYPE extends ComponentEventListener<EVENT>>
            extends EventHandlerHelper<COMPONENT, EVENT, TYPE> {
        @Override
        protected void handleEvent(@NonNull final TYPE listener, @NonNull final EVENT event) {
            listener.onComponentEvent(event);
        }
    }

    /**
     * An instance of the event handler for {@link ValueChangeEvent}s.
     * 
     * @author bcersows
     */
    public static class ValueChangedEventHandler<COMPONENT extends Component, EVENT extends ComponentValueChangeEvent<COMPONENT, ?>, TYPE extends ValueChangeListener<EVENT>>
            extends EventHandlerHelper<COMPONENT, EVENT, TYPE> {
        @Override
        protected void handleEvent(@NonNull final TYPE listener, @NonNull final EVENT event) {
            listener.valueChanged(event);
        }

        /**
         * Overwrite the method for explicit type-casting.
         */
        @NonNull
        public Registration addListener(@NonNull final ValueChangeListener<? super EVENT> listener) {
            return super.addListener((TYPE) listener);
        }
    }
}
