package de.bcersows.vaadinhelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.shared.Registration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The UI binder allows to build UI structures on the fly based on an input.
 * 
 * @param <EntityT>
 *            the type of the data
 * @param <ParentEntityT>
 *            when using nested {@code UiBinder}s, this is the data type of the parent binder; {@code Void} for the top level binder
 * @author BCE
 */
// we do want to actively state runtime exceptions; custom generics names
@SuppressWarnings({ "java:S1130", "java:S119" })
public class UiBinder<EntityT, ParentEntityT> {

    /** The different bindings. **/
    @NonNull
    private final Map<Component, Binding<EntityT>> bindings = new HashMap<>();
    /** The nested {@code UiBinder}s.. **/
    @NonNull
    private final Map<Component, UiBinder<?, EntityT>> nestedBinders = new HashMap<>();
    /** Created event registrations. **/
    @NonNull
    private final List<Registration> registrations = new ArrayList<>();
    /** A runnable to run after loading. E.g. for running non-component-related logic. **/
    @Nullable
    private Consumer<EntityT> runnable;

    /** The component the binder is attached to. **/
    @NonNull
    private final HasComponents binderComponent;
    /** The entity type. **/
    @NonNull
    private final Class<EntityT> entityType;
    // nested binder fields
    /** The parent binder, if nested. **/
    @Nullable
    private final UiBinder<ParentEntityT, ?> parentBinder;
    /** The extractor if this is a nested binder. **/
    @Nullable
    private final Function<ParentEntityT, EntityT> nestedExtractor;

    /** If data was loaded already. For security reasons. **/
    private boolean loadedData;

    /**
     * Create an instance of the binder.
     * 
     * @param entityType
     *            the entity type
     * @param nestedExtractor
     *            when having a nested binder, this function is used to convert the parent entity into the current one
     */
    protected UiBinder(@NonNull final HasComponents binderComponent, @NonNull final Class<EntityT> entityType,
            @Nullable final UiBinder<ParentEntityT, ?> parentBinder, @Nullable final Function<ParentEntityT, EntityT> nestedExtractor) {
        this.binderComponent = binderComponent;
        this.entityType = entityType;
        this.parentBinder = parentBinder;
        this.nestedExtractor = nestedExtractor;
    }

    /** Create an instance. **/
    public static <T, C extends Component & HasComponents> UiBinder<T, Void> with(@NonNull final C binderComponent, @NonNull final Class<T> entityType,
            @Nullable final Component... components) {
        // add components
        if (null != components) {
            binderComponent.add(components);
        }

        // create binder
        return new UiBinder<>(binderComponent, entityType, null, null);
    }

    /** Create a nested binder. **/
    protected static <ParentEntityT, NestedEntityT, C extends Component & HasComponents> UiBinder<NestedEntityT, ParentEntityT> withNested(
            @NonNull final C binderComponent, @NonNull final UiBinder<ParentEntityT, ?> parentBinder, @NonNull final Class<NestedEntityT> nestedClass,
            @NonNull final Function<ParentEntityT, NestedEntityT> extractor) {
        return new UiBinder<NestedEntityT, ParentEntityT>(binderComponent, nestedClass, parentBinder, extractor);
    }

    //// nesting methods
    /**
     * Create a nested binder with the same data type.
     * 
     * @param component
     *            the component to register it with
     * @return the nested binder
     * 
     * @throws InvalidConfigurationException
     *             if the component was already configured
     */
    @NonNull
    public <C extends Component & HasComponents> UiBinder<EntityT, EntityT> createNested(@NonNull final C component) {
        return createNested(component, this.entityType, Function.identity());
    }

    /**
     * Create a nested binder.
     * 
     * @param <NestedEntityT>
     *            the nested binder's entity type
     * @param component
     *            the component to register it with
     * @param nestedClass
     *            the class of the nested entity type
     * @param extractor
     *            the function to extract the nested type from the input
     * @return the nested binder
     * 
     * @throws InvalidConfigurationException
     *             if the component was already configured
     */
    @NonNull
    public <NestedEntityT, C extends Component & HasComponents> UiBinder<NestedEntityT, EntityT> createNested(@NonNull final C component,
            @NonNull final Class<NestedEntityT> nestedClass, @NonNull final Function<EntityT, NestedEntityT> extractor) {
        // check it wasn't configured already
        checkForDuplicatesAndLoaded(component);

        // create the nested binder and store it
        final var nestedBinder = withNested(component, this, nestedClass, extractor);
        this.nestedBinders.put(component, nestedBinder);
        return nestedBinder;
    }

    /**
     * Get the parent binder.
     * 
     * @return the parent
     * 
     * @throws InvalidConfigurationException
     *             if not a nested binder
     */
    @NonNull
    public UiBinder<ParentEntityT, ?> getParent() {
        if (null == this.parentBinder) {
            throw new InvalidConfigurationException("This is not a nested binder, no parent is available.");
        }
        return this.parentBinder;
    }

    //// data handling
    /**
     * Load the data into the binder. Starts the UI building.
     * 
     * @param entity
     *            the data to fill it with
     */
    public void load(@NonNull final EntityT entity) {
        // store that we loaded data already
        this.loadedData = true;

        // remove all registrations
        removeRegistrations();

        // run bindings
        for (final var binding : this.bindings.values()) {
            binding.runBinding(entity);
        }
        // run nested binders and load them as well
        for (final var binding : this.nestedBinders.values()) {
            binding.loadFromParent(entity);
        }

        // run the runnable
        if (null != this.runnable) {
            this.runnable.accept(entity);
        }
    }

    /**
     * If the binder is a nested one, convert the parent entity and load it.
     * 
     * @param parentEntity
     *            the entity of the parent binder
     * 
     * @throws InvalidConfigurationException
     *             if it's not a nested binder
     */
    protected void loadFromParent(@NonNull final ParentEntityT parentEntity) throws InvalidConfigurationException {
        // ensure there's an extractor
        if (null == this.nestedExtractor) {
            throw new InvalidConfigurationException("Cannot call extractor without having any.");
        }

        // convert the input entity into the current one and load the data
        final var nestedEntity = this.nestedExtractor.apply(parentEntity);
        this.load(nestedEntity);
    }

    /** Clear all existing bindings. **/
    @NonNull
    public UiBinder<EntityT, ParentEntityT> clear() {
        removeRegistrations();
        this.bindings.clear();
        this.nestedBinders.clear();
        this.loadedData = false;

        return this;
    }

    //// add bindings
    /**
     * Fill the binder component with the calculated components. Replaces all existing children.
     * 
     * @param function
     *            to calculate the new children based on the entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     */
    @NonNull
    public UiBinder<EntityT, ParentEntityT> fillWith(@NonNull final Function<EntityT, List<Component>> fillFunction) throws InvalidConfigurationException {
        // custom casting, as no way to store both parents
        return this.fillWith((Component & HasComponents) this.binderComponent, fillFunction);
    }

    /**
     * Fill the given wrapper component with the calculated components. Replaces all existing ones.
     * 
     * @param component
     *            the component to fill
     * @param fillFunction
     *            function to calculate the new children based on the entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     */
    @NonNull
    public <C extends Component & HasComponents> UiBinder<EntityT, ParentEntityT> fillWith(@NonNull final C component,
            @NonNull final Function<EntityT, List<Component>> fillFunction) throws InvalidConfigurationException {
        return this.fillWith(component, Function.identity(), fillFunction);
    }

    /**
     * Fill the given wrapper component with the calculated components. Replaces all existing ones.
     * 
     * @param component
     *            the component to fill
     * @param extractor
     *            extract data from the entity that will be passed to the fill function
     * @param fillFunction
     *            function to calculate the new children based on the entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     */
    @NonNull
    public <V, C extends Component & HasComponents> UiBinder<EntityT, ParentEntityT> fillWith(@NonNull final C component,
            @NonNull final Function<EntityT, V> extractor, @NonNull final Function<V, List<Component>> fillFunction) throws InvalidConfigurationException {
        addBinding(component, entity -> {
            component.removeAll();
            final var extracted = extractor.apply(entity);
            final var children = fillFunction.apply(extracted);

            component.add(children.toArray(new Component[0]));
        });

        return this;
    }

    /**
     * Fill the given component with the extracted/calculated text.
     * 
     * @param component
     *            the component to update
     * @param setFunction
     *            get the text based on the entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     * 
     * @see #set(Component, BiConsumer)
     * @see #set(Component, Function)
     */
    @NonNull
    public <C extends Component & HasText> UiBinder<EntityT, ParentEntityT> setText(@NonNull final C component,
            @NonNull final Function<EntityT, String> setFunction) throws InvalidConfigurationException {
        addBinding(component, entity -> component.setText(setFunction.apply(entity)));

        return this;
    }

    /**
     * Fill the given component with the extracted/calculated value. Generic implementation if component does not have a value or text interface.
     * 
     * @param component
     *            the component to update
     * @param setFunction
     *            gets the component and entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     * 
     * @see #set(Component, BiConsumer) for {@link HasValue} components
     * @see #setText(Component, Function) for {@link HasText} components
     */
    @NonNull
    public <C extends Component> UiBinder<EntityT, ParentEntityT> set(@NonNull final C component, @NonNull final BiConsumer<EntityT, C> setFunction)
            throws InvalidConfigurationException {
        addBinding(component, entity -> setFunction.accept(entity, component));

        return this;
    }

    /**
     * Fill the given component with the extracted/calculated value.
     * 
     * @param component
     *            the component to update
     * @param setFunction
     *            get the value based on the entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     * 
     * @see #set(Component, BiConsumer)
     * @see #setText(Component, Function)
     */
    @NonNull
    public <V, C extends Component & HasValue<?, V>> UiBinder<EntityT, ParentEntityT> set(@NonNull final C component,
            @NonNull final Function<EntityT, V> setFunction) throws InvalidConfigurationException {
        addBinding(component, entity -> component.setValue(setFunction.apply(entity)));

        return this;
    }

    /**
     * Add a click listener to the button.
     * 
     * @param button
     *            the button to update
     * @param setFunction
     *            get the event runnable based on the entity
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     */
    @NonNull
    public UiBinder<EntityT, ParentEntityT> addClickListener(@NonNull final Button button, @NonNull final Function<EntityT, Runnable> eventFunction)
            throws InvalidConfigurationException {
        addBinding(button, entity -> {
            // get the event runnable, add it as a click listener and store the registration
            final var eventRunnable = eventFunction.apply(entity);
            this.registrations.add(button.addClickListener(clickEvent -> eventRunnable.run()));
        });

        return this;
    }

    /**
     * Set the items of a component that allows it.
     * 
     * @param <V>
     *            the item type of the component
     * @param component
     *            the item component
     * @param itemFunction
     *            the function to calculate the items
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     */
    @NonNull
    public <V, C extends Component & HasDataProvider<V>> UiBinder<EntityT, ParentEntityT> setItems(@NonNull final C component,
            @NonNull final Function<EntityT, List<V>> itemFunction) throws InvalidConfigurationException {
        addBinding(component, entity -> component.setItems(itemFunction.apply(entity)));

        return this;
    }

    /**
     * Set the items of a combo box.
     * 
     * @param <V>
     *            the item type of the component
     * @param component
     *            the item component
     * @param itemFunction
     *            the function to calculate the items
     * @return UiBinder instance
     * @throws InvalidConfigurationException
     *             if already configured the component
     */
    @NonNull
    public <V> UiBinder<EntityT, ParentEntityT> setItems(@NonNull final ComboBox<V> component, @NonNull final Function<EntityT, List<V>> itemFunction)
            throws InvalidConfigurationException {
        addBinding(component, entity -> component.setItems(itemFunction.apply(entity)));

        return this;
    }

    /**
     * Set the simple runnable that will be called with the loaded data. Can only have a single runnable per binder!
     * 
     * @param runnable
     *            the runnable to run with the loaded data
     * @return UiBinder instance
     * 
     * @throws InvalidConfigurationException
     *             if there already is a runnable for the binder
     */
    @NonNull
    public UiBinder<EntityT, ParentEntityT> setRunnable(@NonNull final Consumer<EntityT> runnable) throws InvalidConfigurationException {
        // can only have a single one!
        if (null != this.runnable) {
            throw new InvalidConfigurationException("Can only have one configured runnable.");
        }
        // also check not loaded already
        checkForDuplicatesAndLoaded(null);

        this.runnable = runnable;

        return this;
    }

    //// helpers
    /** Remove all known registrations. **/
    private void removeRegistrations() {
        this.registrations.forEach(Registration::remove);
        this.registrations.clear();
    }

    /**
     * Add a binding to the configuration. Checks for invalid states (already loaded data, duplicate assignments)
     * 
     * @param component
     *            the component to add
     * @param binding
     *            the binding to add
     * 
     * @throws InvalidConfigurationException
     *             if either already loaded with data or duplicate assignment
     */
    private void addBinding(@NonNull final Component component, @NonNull final Binding<EntityT> binding) throws InvalidConfigurationException {
        // check that there are no duplicates and did not load already
        checkForDuplicatesAndLoaded(component);

        // if all good, add the binding
        this.bindings.put(component, binding);
    }

    /**
     * Check for duplicate assignments and that there was no data load before.
     * 
     * @throws InvalidConfigurationException
     *             if either already loaded with data or duplicate assignment
     */
    private void checkForDuplicatesAndLoaded(@Nullable final Component component) throws InvalidConfigurationException {
        if (this.loadedData) {
            // 1. never loaded data...
            throw new InvalidConfigurationException("Cannot add new bindings/nested layers after loading data already!");
        } else if (
        // if provided one...
        null != component
                // ... and if it exists already
                && (this.bindings.containsKey(component) || this.nestedBinders.containsKey(component))) {
            // 2. does not have a duplicated component alignment
            throw new InvalidConfigurationException("Detected multiple bindings/nested for the same component.", component);
        }
    }

    /** A single binding. **/
    @FunctionalInterface
    private interface Binding<T> {
        /** Run the binding. **/
        void runBinding(@NonNull T entity);
    }

    /** Exception when having invalid configuration. **/
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class InvalidConfigurationException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        /** The potential component in question. **/
        @Nullable
        private final Component component;

        public InvalidConfigurationException(@NonNull final String message) {
            this(message, null);
        }

        public InvalidConfigurationException(@NonNull final String message, @Nullable final Component component) {
            super(message);

            this.component = component;
        }
    }
}
