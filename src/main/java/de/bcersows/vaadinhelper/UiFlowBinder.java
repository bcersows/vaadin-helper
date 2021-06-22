package de.bcersows.vaadinhelper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.shared.Registration;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Allows for UI flows and simple validation by enabling some elements only after others are filled.
 * 
 * @author BCE
 */
// TODO add mode to allow for showing/hiding based on other component?
// don't care about generics naming
@SuppressWarnings("java:S119")
public class UiFlowBinder {

    /** The collection of sources for our target. **/
    @NonNull
    private final Set<BinderSource<?>> sources = new CopyOnWriteArraySet<>();

    /** The target component that will be enabled/disabled. **/
    private final HasEnabled target;

    /** Create the binder. **/
    public <C extends Component & HasEnabled, DepTypeValueT extends Object, DepType extends Component & HasValue<? extends ValueChangeEvent<DepTypeValueT>, DepTypeValueT>> UiFlowBinder(
            @NonNull final C target) {
        this(target, (DepType[]) null);
    }

    /** Create the binder. Add some initial dependencies (which cannot be removed). **/
    public <C extends Component & HasEnabled, DepTypeValueT extends Object, DepType extends Component & HasValue<? extends ValueChangeEvent<DepTypeValueT>, DepTypeValueT>> UiFlowBinder(
            @NonNull final C target, @Nullable final DepType... dependencies) {
        this.target = target;

        // if there are initials, add them
        if (null != dependencies) {
            for (final var dependency : dependencies) {
                addDependency(dependency);
            }
        }

        // calculate initially
        valueChanged();
    }

    /** Add a new dependency that must have a valid value. **/
    @NonNull
    public <DepTypeValueT extends Object, DepType extends Component & HasValue<? extends ValueChangeEvent<DepTypeValueT>, DepTypeValueT>> Registration addDependency(
            @NonNull final DepType dependency) {
        return addSource(dependency, true);
    }

    /** Add a new adversary that <strong>must not</strong> have a valid value for the target to be enabled. **/
    @NonNull
    public <DepTypeValueT extends Object, DepType extends Component & HasValue<? extends ValueChangeEvent<DepTypeValueT>, DepTypeValueT>> Registration addAdversary(
            @NonNull final DepType adversary) {
        return addSource(adversary, false);
    }

    /** Add a new source. Flag decides if value required or forbidden. **/
    @NonNull
    private <DepTypeValueT extends Object, DepType extends Component & HasValue<? extends ValueChangeEvent<DepTypeValueT>, DepTypeValueT>> Registration addSource(
            @NonNull final DepType source, final boolean positiveRequirement) {
        // add a listener that will force a re-calculation
        final var listenerRegistration = source.addValueChangeListener(event -> valueChanged());

        // add to collection
        final var binderSource = new BinderSource<>(source, positiveRequirement, listenerRegistration);
        this.sources.add(binderSource);

        // calculate state
        valueChanged();

        // return a registration that will remove the listener and the source from the storage
        return Registration.once(() -> {
            binderSource.listenerRegistration.remove();
            this.sources.remove(binderSource);
        });
    }

    /** Method to check the current values of **/
    protected void valueChanged() {
        final var allSourcesValid = this.sources.stream().allMatch(source -> {
            // valid if positive requirement and valid value
            final var value = source.source.getValue();

            // decide if the value is valid; Strings have an extended handling
            var valueValid = null != value;
            if (value instanceof String) {
                valueValid &= !((String) value).isEmpty();
            }

            // ok if requirement matches reality
            return source.positiveRequirement == valueValid;
        });
        this.target.setEnabled(allSourcesValid);
    }

    /** Store the data belonging together. **/
    @Data
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class BinderSource<DepTypeValueT extends Object> {
        /** The source element. **/
        @EqualsAndHashCode.Include
        @NonNull
        private final HasValue<? extends ValueChangeEvent<DepTypeValueT>, DepTypeValueT> source;
        /** If a non-null value is required. **/
        private final boolean positiveRequirement;
        /** The registration for the change listener. **/
        @NonNull
        private final Registration listenerRegistration;
    }
}
