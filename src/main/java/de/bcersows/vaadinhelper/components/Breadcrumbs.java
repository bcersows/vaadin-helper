package de.bcersows.vaadinhelper.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * A breadcrumbs component for navigation purposes.
 * 
 * @author BCE
 */
@StyleSheet("context://frontend/component-breadcrumbs.css")
@Slf4j
@Tag("Div")
@SuppressWarnings({ "java:S1075" })
public class Breadcrumbs extends Component {
    private static final long serialVersionUID = 2L;

    /** The format for the parameter-based display name. **/
    private static final String DISPLAY_NAME_PARAMETER_FORMAT = "%s %s";

    /** CSS class for a single crumb entry. **/
    protected static final String CSS_CLASS_CRUMB = "crumb";
    /** CSS class for the last crumb entry. **/
    protected static final String CSS_CLASS_CRUMB_PLAIN = "crumb-plain";
    /** CSS class for the crumb icon. **/
    private static final String CSS_CLASS_CRUMB_ROOT_ICON = "crumb-root-icon";
    /** CSS class for the separator. **/
    protected static final String CSS_CLASS_SEPARATOR = "separator";

    /** The actual component to contain the crumbs. **/
    private final HorizontalLayout breadcrumbsArea;

    /** The separator between the elements. **/
    private String separator = "〉";

    /** Create an instance. **/
    public Breadcrumbs() {
        this.setId("breadcrumbs-wrapper");
        this.breadcrumbsArea = new HorizontalLayout();
        this.breadcrumbsArea.setSpacing(false);
        this.breadcrumbsArea.setPadding(false);
        this.breadcrumbsArea.setId("breadcrumbs");

        this.getElement().appendChild(this.breadcrumbsArea.getElement());
    }

    /**
     * Try to find the first breadcrumb step in the chain of elements.
     * 
     * @param elementsChain
     *            the chain of active elements (e.g. from {@link AfterNavigationEvent#getActiveChain()}); sorted from lowest to highest element
     */
    @Nullable
    public static BreadcrumbStep tryToFindBreadcrumbStep(@NonNull final List<HasElement> elementsChain) {
        return elementsChain.stream().filter(chain -> chain.getClass().isAnnotationPresent(BreadcrumbStep.class))
                .map(chain -> chain.getClass().getAnnotation(BreadcrumbStep.class)).findFirst().orElse(null);
    }

    /**
     * Find a breadcrumbs component in the chain of active elements, and update the breadcrumbs using the given parameters.
     * 
     * @param ui
     *            the surrounding UI, used to get the layout(s)
     * @param currentView
     *            the current view as the source of the breadcrumb step annotation
     * @param parameters
     *            the parameters for display name population and navigation
     */
    public static void findAndUpdateBreadcrumbs(@NonNull final UI ui, @NonNull final Component currentView, @NonNull final Map<String, String> parameters) {
        // create the UI layer list/active elements chain
        final List<HasElement> uiLayers = new ArrayList<>(ui.getChildren().collect(Collectors.toList()));
        uiLayers.add(0, currentView);

        // continue with the normal flow
        findAndUpdateBreadcrumbs(uiLayers, parameters);
    }

    /**
     * Find a breadcrumbs component in the chain of active elements, and update the breadcrumbs using the given parameters.
     * 
     * @param activeElementsChain
     *            the chain of active elements (e.g. from {@link AfterNavigationEvent#getActiveChain()}); sorted from lowest to highest element
     * @param parameters
     *            the parameters for display name population and navigation
     */
    public static void findAndUpdateBreadcrumbs(@NonNull final List<HasElement> activeElementsChain, @NonNull final Map<String, String> parameters) {
        findBreadcrumbsComponent(activeElementsChain, breadcrumbs -> {
            // now try to find a breadcrumb step as well (temporarily creating query parameters)
            final var breadcrumbStep = tryToFindBreadcrumbStep(activeElementsChain);
            if (null != breadcrumbStep) {
                // if found one, update the crumbs
                breadcrumbs.populateBreadcrumbs(breadcrumbStep, parameters);
            }
        });
    }

    /**
     * Find a breadcrumbs component in the chain of active elements, and update the breadcrumbs using the given creation data and parameters.
     * 
     * @param ui
     *            the current UI to extract the active elements from
     * @param breadcrumbCreationData
     *            data required for the crumbs creation
     */
    public static void findAndUpdateBreadcrumbs(@NonNull final UI ui, @NonNull final BreadcrumbCreationData breadcrumbCreationData) {
        // extract the active elements from the UI
        final List<HasElement> activeElementsChain = ui.getChildren().collect(Collectors.toList());
        // update the component
        findAndUpdateBreadcrumbs(activeElementsChain, breadcrumbCreationData);
    }

    /**
     * Find a breadcrumbs component in the chain of active elements, and update the breadcrumbs using the given creation data and parameters.
     * 
     * @param activeElementsChain
     *            the chain of active elements (e.g. from {@link AfterNavigationEvent#getActiveChain()}); sorted from lowest to highest element
     * @param breadcrumbCreationData
     *            data required for the crumbs creation
     */
    public static void findAndUpdateBreadcrumbs(@NonNull final List<HasElement> activeElementsChain,
            @NonNull final BreadcrumbCreationData breadcrumbCreationData) {
        findBreadcrumbsComponent(activeElementsChain, breadcrumbs -> {
            // if there is legacy data, enrich the creation data
            final var legacyData = breadcrumbCreationData.legacyData;
            final BreadcrumbCreationData enrichedData;
            if (
            // if there is legacy data...
            null != legacyData
                    // ... and the given class is a breadcrumb step
                    && legacyData.breadcrumbClass.isAnnotationPresent(BreadcrumbStep.class)) {
                final var breadcrumbStep = legacyData.breadcrumbClass.getAnnotation(BreadcrumbStep.class);

                // using the given legacy data, calculate the crumbs upwards of it
                final var legacyCollected = breadcrumbs.collectCrumbsFromAnnotation(legacyData.breadcrumbClass, breadcrumbStep, legacyData.parameters);
                final List<BreadcrumbCreationEntry<?>> legacyEntries = legacyCollected.entries;
                // combine the two lists and create new creation data
                final List<BreadcrumbCreationEntry<?>> enrichedEntries = new ArrayList<>(legacyEntries);
                enrichedEntries.addAll(breadcrumbCreationData.entries);

                // home icon is depending on legacy or current settings
                final var useHomeIcon = breadcrumbCreationData.useHomeIcon || legacyCollected.useHomeIcon;
                enrichedData = new BreadcrumbCreationData(useHomeIcon, enrichedEntries);
            } else {
                enrichedData = breadcrumbCreationData;
            }

            // and fill into the UI
            breadcrumbs.setElements(enrichedData);
            breadcrumbs.setVisible(!enrichedData.entries.isEmpty());
        });
    }

    /**
     * Tries to populate the breadcrumbs with the eventually given breadcrumb step annotation. Will only create if auto-create is configured.
     * 
     * @return if breadcrumbs are being shown
     * @see #tryPopulatingBreadcrumbs(BreadcrumbStep, RouteParameters)
     * @see #populateBreadcrumbs(BreadcrumbStep, RouteParameters)
     */
    public boolean tryPopulatingBreadcrumbs(@NonNull final List<HasElement> activeElementsChain, @NonNull final RouteParameters routeParameters) {
        // get current view, if has breadcrumb, display them for it and parents
        final var breadcrumbStep = tryToFindBreadcrumbStep(activeElementsChain);
        return tryPopulatingBreadcrumbs(breadcrumbStep, routeParameters);
    }

    /**
     * Tries to populate the breadcrumbs with the eventually given breadcrumb step annotation. Will only create if auto-create is configured.
     * 
     * @param breadcrumbStep
     *            a possible annotation of a breadcrumb step
     * @param queryParameters
     *            the current query parameters as input for the route parameters
     * @return if breadcrumbs are being shown
     * 
     * @see #tryPopulatingBreadcrumbs(List, RouteParameters)
     * @see #tryPopulatingBreadcrumbs(BreadcrumbStep, Map)
     * @see #populateBreadcrumbs(BreadcrumbStep, Map)
     */
    public boolean tryPopulatingBreadcrumbs(@Nullable final BreadcrumbStep breadcrumbStep, @NonNull final RouteParameters routeParameters) {
        // convert route parameters to map temporarily
        final var routeParametersMap = routeParameters.getParameterNames().stream()
                .collect(Collectors.toMap(Function.identity(), param -> routeParameters.get(param).get()));
        return tryPopulatingBreadcrumbs(breadcrumbStep, routeParametersMap);
    }

    /**
     * Tries to populate the breadcrumbs with the eventually given breadcrumb step annotation. Will only create if auto-create is configured.
     * 
     * @param breadcrumbStep
     *            a possible annotation of a breadcrumb step
     * @param parameters
     *            the current parameters as input for the route parameters
     * @return if breadcrumbs are being shown
     * 
     * @see #tryPopulatingBreadcrumbs(List, RouteParameters)
     * @see #tryPopulatingBreadcrumbs(BreadcrumbStep, RouteParameters)
     * @see #populateBreadcrumbs(BreadcrumbStep, Map)
     */
    public boolean tryPopulatingBreadcrumbs(@Nullable final BreadcrumbStep breadcrumbStep, @NonNull final Map<String, String> parameters) {
        final var willEvenTryToPopulate = null != breadcrumbStep && breadcrumbStep.autoCreate();
        if (!willEvenTryToPopulate) {
            // if not even trying to, hide the component
            this.setVisible(false);
        }

        // if trying to, populate the breadcrumbs
        return willEvenTryToPopulate && populateBreadcrumbs(breadcrumbStep, parameters);
    }

    /**
     * Set the elements based on the given view, or hide the whole area if no elements or no view given.
     * 
     * @param breadcrumbStep
     *            an annotation of a breadcrumb step
     * @param parameters
     *            the parameters as input for the routing & display names
     * @return if breadcrumbs are being shown
     */
    public boolean populateBreadcrumbs(@NonNull final BreadcrumbStep breadcrumbStep, @NonNull final Map<String, String> parameters) {
        // collect the crumbs
        final BreadcrumbCreationData breadcrumbsData = collectCrumbsFromAnnotation(breadcrumbStep, parameters);

        return populateBreadcrumbs(breadcrumbsData);
    }

    /**
     * Set the elements based on the given view, or hide the whole area if no elements or no view given.
     * 
     * @param breadcrumbsData
     *            the breadcrumbs data
     * @return if breadcrumbs are being shown
     */
    private boolean populateBreadcrumbs(@NonNull final BreadcrumbCreationData breadcrumbsData) {
        final boolean showCrumbs;

        if (breadcrumbsData.entries.isEmpty()) {
            showCrumbs = false;
        } else {
            setElements(breadcrumbsData);
            showCrumbs = true;
        }

        // show/hide the component itself
        this.setVisible(showCrumbs);

        return showCrumbs;
    }

    /**
     * Set the separator. Must be called before populating the crumbs.
     */
    public void setSeparator(@NonNull final String separator) {
        this.separator = separator;
    }

    /**
     * Collect the list of crumbs based on the given crumb. Iterates up through the parents and adds them to the result list.<br/>
     * The last crumb in the list will be made plain.
     */
    @NonNull
    protected BreadcrumbCreationData collectCrumbsFromAnnotation(@NonNull final BreadcrumbStep breadcrumbStep,
            @NonNull final Map<String, String> routeParameters) {
        final var crumbs = collectCrumbsFromAnnotation(null, breadcrumbStep, routeParameters);

        // if this is the first entry (=last breadcrumb to be shown), make it plain without navigation
        if (!crumbs.entries.isEmpty()) {
            crumbs.entries.get(crumbs.entries.size() - 1).navMode = BreadcrumbNavigationMode.NONE;
        }

        return crumbs;
    }

    /**
     * Collect the list of crumbs based on the given crumb. Iterates up through the parents and adds them to the result list.
     * 
     * @param initialStepTarget
     *            provide an initial breadcrumb step target which must comply with the given breadcrumb step
     * @param breadcrumbStep
     *            the breadcrumb step to collect crumbs from
     * @param routeParameters
     *            route parameters to fill values with
     * 
     * @see #collectCrumbsFromAnnotation(BreadcrumbStep, Map) when in doubt, call this method instead
     */
    @NonNull
    protected BreadcrumbCreationData collectCrumbsFromAnnotation(@Nullable final Class<? extends Component> initialStepTarget,
            @NonNull final BreadcrumbStep breadcrumbStep, @NonNull final Map<String, String> routeParameters) {
        if (null != initialStepTarget && breadcrumbStep != initialStepTarget.getAnnotation(BreadcrumbStep.class)) {
            throw new IllegalStateException("The given initial step target must match with the breadcrumb step!");
        }

        final List<BreadcrumbCreationEntry<?>> crumbs = new ArrayList<>();
        boolean useHomeIcon = false;

        var singleStep = breadcrumbStep;
        Class<? extends Component> currentStepNavigationTarget = initialStepTarget;
        while (null != singleStep) {
            // create the entry, either as a dynamic or normal one
            final BreadcrumbCreationEntry<?> breadcrumbEntry = calculateBreadcrumbCreationEntry(singleStep, currentStepNavigationTarget, routeParameters);

            // add to list as new first item (reverse order)
            crumbs.add(0, breadcrumbEntry);

            // if the root node and configured to use a home icon, set flag
            if (!useHomeIcon && singleStep.parent().equals(BreadcrumbStep.__ROOT_COMPONENT__) && !singleStep.showTextInsteadOfHomeIcon()) {
                useHomeIcon = true;
            }

            // and move to the parent step, if any
            currentStepNavigationTarget = singleStep.parent();
            singleStep = currentStepNavigationTarget.getAnnotation(BreadcrumbStep.class);
        }

        // create the creation data wrapper
        return new BreadcrumbCreationData(useHomeIcon, crumbs);
    }

    /**
     * Find the breadcrumbs component out of the given elements chain.
     * 
     * @param activeElementsChain
     *            the chain of active elements
     * @param breadcrumbComponentConsumer
     *            the consumer that will receive the breadcrumbs, <strong>if found any</strong>
     */
    private static void findBreadcrumbsComponent(@NonNull final List<HasElement> activeElementsChain,
            @NonNull final Consumer<Breadcrumbs> breadcrumbComponentConsumer) {
        final var breadcrumbComponent = activeElementsChain.stream().filter(element -> element instanceof BreadcrumbEnabledComponent)
                .map(element -> (BreadcrumbEnabledComponent) element).findFirst();

        if (breadcrumbComponent.isPresent()) {
            // found a breadcrumb component
            final var breadcrumbs = breadcrumbComponent.get().getBreadcrumbs();

            // pass the component to the caller
            breadcrumbComponentConsumer.accept(breadcrumbs);
        } else {
            log.trace("Found no breadcrumb-enabled component.");
        }
    }

    /**
     * Calculate the crumb entity based on the given step.
     * 
     * @param breadcrumbStep
     *            the step to analyze
     * @param breadcrumbStepAnnotatedClass
     *            the current step class
     * @param routeParameters
     *            route parameters for navigation and display name
     * @return the creation entry data
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private static <C extends Component> BreadcrumbCreationEntry<?> calculateBreadcrumbCreationEntry(@NonNull final BreadcrumbStep breadcrumbStep,
            @Nullable final Class<C> breadcrumbStepAnnotatedClass, @NonNull final Map<String, String> routeParameters) {
        final BreadcrumbCreationEntry<C> breadcrumbEntry;

        // calculate the display name; if part of the parameters, get it
        final String displayName = calculateDisplayName(breadcrumbStep, routeParameters);

        final var gotParameterConfiguration = !breadcrumbStep.parameter().isBlank();
        final var gotRouteParametersConfiguration = breadcrumbStep.routeParameters().length > 0;

        // create the actual entry, based on the configuration
        // the manual casting is alright, we checked before
        final Class targetClass = breadcrumbStepAnnotatedClass;

        // if a crumb with parameters, start the special handling
        if (
        // TODO also later check for @OptionalParameter on method parameter?
        // if has an URL parameter
        null != breadcrumbStepAnnotatedClass && hasStringUrlParameter(breadcrumbStepAnnotatedClass)
        // and the parameter configuration
                && gotParameterConfiguration) {
            // parameter mode
            breadcrumbEntry = ParameterBreadcrumbCreationEntry.parameterTarget(displayName, targetClass,
                    filterRouteParameters(routeParameters, breadcrumbStep.parameter()));
        } else if (
        // if has route parameter config
        null != breadcrumbStepAnnotatedClass && NavigationTarget.class.isAssignableFrom(breadcrumbStepAnnotatedClass) && gotRouteParametersConfiguration) {
            // route parameter mode
            breadcrumbEntry = RouteParameterBreadcrumbCreationEntry.routeParameterTarget(displayName, targetClass,
                    filterRouteParameters(routeParameters, breadcrumbStep.routeParameters()));
        } else {
            // plain targets just get the target class
            breadcrumbEntry = BreadcrumbCreationEntry.plainTarget(displayName, breadcrumbStepAnnotatedClass);
        }

        // configure arbitrary flags
        breadcrumbEntry.setHideIfLast(breadcrumbStep.hideIfLast());
        breadcrumbEntry.setHideIfRoot(breadcrumbStep.hideIfRoot());

        return breadcrumbEntry;
    }

    /**
     * Set the breadcrumbs to display.
     * 
     * @param breadcrumbsData
     *            the list of crumbs
     */
    private void setElements(@NonNull final BreadcrumbCreationData breadcrumbsData) {
        this.breadcrumbsArea.removeAll();

        final var useHomeIconForFirstCrumb = breadcrumbsData.useHomeIcon;

        final var breadcrumbs = breadcrumbsData.entries;
        final var onlyOneEntry = breadcrumbs.size() == 1;
        final var iterator = breadcrumbs.iterator();
        boolean first = true;
        // loop through all crumbs that were provided
        while (iterator.hasNext()) {
            final var crumb = iterator.next();
            final boolean isLast = !iterator.hasNext();

            final var name = crumb.name;

            try {
                createAndAddComponent(crumb, name, first, isLast, onlyOneEntry, useHomeIconForFirstCrumb);
            } catch (final NotFoundException e) {
                log.warn("Invalid breadcrumb configuration for {} ({}) with parameters {}!", name, crumb.navigationTarget, crumb);
                log.trace("Error: ", e);
            }

            first = false;
        }
    }

    /**
     * Create a UI representation for the given crumb and add it to the breadcrumbs area. Also adds separators, if required.
     */
    private void createAndAddComponent(@NonNull final BreadcrumbCreationEntry<?> crumb, @NonNull final String crumbName, final boolean first,
            final boolean isLast, final boolean onlyOneEntry, final boolean useHomeIconForFirstCrumb) {
        // create the crumb component
        final Component crumbComponent;
        // check if normal link or a label, depending on the nav mode
        if (BreadcrumbNavigationMode.NONE == crumb.navMode) {
            // if the element does not have navigation, just display it as a label and also mark it differently
            final var plainLink = new Label(crumbName);
            plainLink.addClassNames(CSS_CLASS_CRUMB, CSS_CLASS_CRUMB_PLAIN);
            crumbComponent = plainLink;
        } else {
            // if a normal crumb, create a router link
            final var crumbLink = createAnchorForNavigation(crumbName, crumb);
            crumbLink.setClassName(CSS_CLASS_CRUMB);

            crumbComponent = crumbLink;
        }

        // allowing the root, if only entry
        final var addIfRoot = !(onlyOneEntry && crumb.hideIfRoot);
        // allowed to add if
        final var addToUi =
                // not last and configured to be hidden
                !(isLast && crumb.hideIfLast)
                        // if the only entry and hiding root
                        && addIfRoot;

        // add to the UI
        if (first && addIfRoot && useHomeIconForFirstCrumb) {
            // if root element and shall display an icon, modify the created component a bit
            crumbComponent.getElement().setText("");
            final var homeIcon = VaadinIcon.HOME.create();
            homeIcon.addClassName(CSS_CLASS_CRUMB_ROOT_ICON);
            crumbComponent.getElement().appendChild(homeIcon.getElement());
            this.breadcrumbsArea.add(crumbComponent);
        } else if (addToUi) {
            // add the crumb and a separator to the UI
            final var separatorLabel = new Label(this.separator);
            separatorLabel.setClassName(CSS_CLASS_SEPARATOR);
            this.breadcrumbsArea.add(separatorLabel);
            this.breadcrumbsArea.add(crumbComponent);
        }
    }

    /**
     * Create a router link for navigating to the given target
     * 
     * @param name
     *            the link name
     * @param crumb
     *            the crumb to create for
     * @return the router link
     */
    @NonNull
    private RouterLink createAnchorForNavigation(@NonNull final String name, @NonNull final BreadcrumbCreationEntry<?> crumb) {
        // parse to parameter crumb
        final var parameterCrumb = crumb instanceof ParameterBreadcrumbCreationEntry ? (ParameterBreadcrumbCreationEntry<?>) crumb : null;

        final RouterLink crumbLink;
        switch (crumb.navMode) {
            case PARAMETER:
                // single parameter
                crumbLink = new RouterLink(name, parameterCrumb.navigationTarget, parameterCrumb.parameter);
                break;
            case PARAMETERS:
                // have a set of parameters
                final var parametersCrumb = ((RouteParameterBreadcrumbCreationEntry<?>) crumb);
                crumbLink = new RouterLink(name, parametersCrumb.navigationTarget, parametersCrumb.routeParameters);

                break;
            case NONE:
                // no navigation
                crumbLink = new RouterLink();
                crumbLink.setText(name);
                break;
            default:
                // default mode, just navigate there
                crumbLink = new RouterLink(name, crumb.navigationTarget);
                break;
        }

        return crumbLink;
    }

    /**
     * Calculate the display name, either by getting the configured value or by enriching using the route parameters, if configured.
     */
    @NonNull
    private static String calculateDisplayName(@NonNull final BreadcrumbStep breadcrumbStep, @NonNull final Map<String, String> routeParameters) {
        final var configuredDisplayName = breadcrumbStep.name();

        // try to get the matching route parameter for the dynamic text, if there's any
        final var parameter = getRouteParameter(routeParameters, breadcrumbStep.dynamicTextKey());

        final String displayName;
        if (null != parameter) {
            // calculate the display name (either append the parameter or replace it)
            if (breadcrumbStep.dynamicTextAppend()) {
                displayName = String.format(DISPLAY_NAME_PARAMETER_FORMAT, configuredDisplayName, parameter);
            } else {
                displayName = parameter;
            }
        } else {
            // if no parameter found, use the configured name
            displayName = configuredDisplayName;
        }

        // and return the display name
        return displayName;
    }

    /**
     * Find a given route parameter value by the configured keys. Order matters; will always return the first.
     * 
     * @param routeParameters
     *            the parameters to check
     * @param dynamicTextKey
     *            the configured keys; can be empty
     * @return the value, if found
     */
    @Nullable
    private static String getRouteParameter(@NonNull final Map<String, String> routeParameters, @NonNull final String[] dynamicTextKeys) {
        for (final var key : dynamicTextKeys) {
            final var parameter = routeParameters.get(key);
            if (null != parameter) {
                return parameter;
            }
        }

        return null;
    }

    /**
     * Filter the route parameters for the given parameters. Returns a new map.
     */
    @NonNull
    private static Map<String, String> filterRouteParameters(@NonNull final Map<String, String> routeParameters, @NonNull final String[] queryParameters) {
        final Map<String, String> parameters = new HashMap<>();

        // for each of the filters, get the value
        for (final String parameter : queryParameters) {
            parameters.put(parameter, filterRouteParameters(routeParameters, parameter));
        }

        return parameters;
    }

    /**
     * Filter the route parameters for the given parameter.
     */
    @NonNull
    private static String filterRouteParameters(@NonNull final Map<String, String> routeParameters, @NonNull final String parameter) {
        final var routeParameter = routeParameters.get(parameter);

        if (null != routeParameter) {
            return routeParameter;
        } else {
            log.warn("Configured route parameter {} is not available.", parameter);
            return "";
        }
    }

    /**
     * Check if the given class is based on a String-backed URL parameter.
     */
    private static <C extends Component> boolean hasStringUrlParameter(@NonNull final Class<C> breadcrumbStepAnnotatedClass) {
        final boolean implementsInterface = HasUrlParameter.class.isAssignableFrom(breadcrumbStepAnnotatedClass);

        if (implementsInterface) {
            // need to check for all interfaces...
            for (final var type : breadcrumbStepAnnotatedClass.getGenericInterfaces()) {
                // ...if they are an HasUrlParameter...
                if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == HasUrlParameter.class) {
                    final ParameterizedType ptype = (ParameterizedType) type;
                    // ... of type String
                    return ptype.getActualTypeArguments()[0] == String.class;
                }
            }

            // without type-safety, check for interface (required in case the view is based on another class)
            return HasUrlParameter.class.isAssignableFrom(breadcrumbStepAnnotatedClass);
        }

        // negative if no match
        return false;
    }

    /**
     * Store all data required to create a complete set of breadcrumbs.
     */
    @Data
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class BreadcrumbCreationData {
        /** If to use home icon instead of name for the highest element. **/
        private final boolean useHomeIcon;

        /** Allows to prepend elements starting with the given legacy data. **/
        @Nullable
        private BreadcrumbCreationLegacyData legacyData = null;

        /** The ordered list of breadcrumb entries. **/
        @NonNull
        private final List<BreadcrumbCreationEntry<?>> entries;
    }

    /**
     * Legacy data allows to prepend data in front of the manual breadcrumb creation data.
     */
    @Data
    @AllArgsConstructor
    public static class BreadcrumbCreationLegacyData {
        /** The legacy breadcrumb step's class. **/
        @NonNull
        private final Class<? extends Component> breadcrumbClass;
        /** Possible parameter for the legacy route. **/
        @NonNull
        private final Map<String, String> parameters;
    }

    /**
     * A single breadcrumb entry with all the data required to create it in the UI.
     */
    @Data
    public static class BreadcrumbCreationEntry<C extends Component> {
        /** The crumb name. **/
        @NonNull
        public final String name;
        /** The target to navigate to. Should always be non-null except for the last entry. **/
        @Nullable
        protected final Class<C> navigationTarget;
        /** The navigation mode. **/
        @NonNull
        protected BreadcrumbNavigationMode navMode;

        /** If to hide if last entry. **/
        protected boolean hideIfLast;
        /** If to hide if root entry. **/
        protected boolean hideIfRoot;

        /** Create an instance. **/
        protected BreadcrumbCreationEntry(@NonNull final String displayName, @Nullable final Class<C> navigationTarget,
                @NonNull final BreadcrumbNavigationMode navMode) {
            this.name = displayName;
            this.navigationTarget = navigationTarget;
            this.navMode = navMode;
        }

        /**
         * Create a breadcrumb with plain navigation mode without any parameters.
         * 
         * @param <C>
         *            the target component type
         * @param displayName
         *            display name of the crumb
         * @param target
         *            the navigation target component
         * @return an instance of the creation entry
         */
        public static <C extends Component> BreadcrumbCreationEntry<C> plainTarget(@NonNull final String displayName, @Nullable final Class<C> target) {
            return new BreadcrumbCreationEntry<>(displayName, target, BreadcrumbNavigationMode.PLAIN);
        }

        /**
         * Create a breadcrumb with NONE navigation mode, which doesn't navigate.
         * 
         * @param <C>
         *            The target component type.
         * @param displayName
         *            Display name of the crumb.
         * @param target
         *            the navigation target component
         * @return an instance of the creation entry.
         */
        public static <C extends Component> BreadcrumbCreationEntry<C> noNavigation(@NonNull final String displayName) {
            return new BreadcrumbCreationEntry<>(displayName, null, BreadcrumbNavigationMode.NONE);
        }
    }

    /**
     * A single breadcrumb entry with parameters with all the data required to create it in the UI.
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class ParameterBreadcrumbCreationEntry<C extends Component & HasUrlParameter<String>> extends BreadcrumbCreationEntry<C> {
        /** The target to navigate to. Should always be non-null except for the last entry. **/
        @Nullable
        protected final Class<C> navigationTarget;

        /** The optional route parameter. Depending on the navigation mode. **/
        @Nullable
        protected final String parameter;

        /** Create an instance. **/
        private ParameterBreadcrumbCreationEntry(@NonNull final String displayName, @Nullable final Class<C> navigationTarget,
                @NonNull final BreadcrumbNavigationMode navMode, @Nullable final String parameter) {
            super(displayName, navigationTarget, navMode);

            this.navigationTarget = navigationTarget;
            this.parameter = parameter;
        }

        /**
         * Create a breadcrumb with parameter navigation mode.
         * 
         * @param <C>
         *            the component with an URL parameter
         * @param displayName
         *            display name of the crumb
         * @param target
         *            the navigation target component
         * @param parameter
         *            the parameter to provide to target
         * @return an instance of the creation entry
         */
        public static <C extends Component & HasUrlParameter<String>> ParameterBreadcrumbCreationEntry<C> parameterTarget(@NonNull final String displayName,
                @NonNull final Class<C> target, @NonNull final String parameter) {
            return new ParameterBreadcrumbCreationEntry<>(displayName, target, BreadcrumbNavigationMode.PARAMETER, parameter);
        }
    }

    /**
     * A single breadcrumb entry with parameters with all the data required to create it in the UI.
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class RouteParameterBreadcrumbCreationEntry<C extends Component & NavigationTarget> extends BreadcrumbCreationEntry<C> {
        /** The target to navigate to. Should always be non-null except for the last entry. **/
        @Nullable
        protected final Class<C> navigationTarget;

        /** The optional route parameters. Depending on the navigation mode. **/
        @Nullable
        protected final RouteParameters routeParameters;

        /** Create an instance. **/
        private RouteParameterBreadcrumbCreationEntry(@NonNull final String displayName, @Nullable final Class<C> navigationTarget,
                @NonNull final BreadcrumbNavigationMode navMode, @Nullable final RouteParameters routeParameters) {
            super(displayName, navigationTarget, navMode);

            this.navigationTarget = navigationTarget;
            this.routeParameters = routeParameters;
        }

        /**
         * Create a breadcrumb with route parameter navigation mode.
         * 
         * @param <C>
         *            the component with an URL parameter
         * @param displayName
         *            display name of the crumb
         * @param target
         *            the navigation target component
         * @param routeParameters
         *            the route parameters
         * @return an instance of the creation entry
         */
        public static <C extends Component & NavigationTarget> RouteParameterBreadcrumbCreationEntry<C> routeParameterTarget(@NonNull final String displayName,
                @NonNull final Class<C> target, @NonNull final Map<String, String> routeParameters) {
            return routeParameterTarget(displayName, target, new RouteParameters(routeParameters));
        }

        /**
         * Create a breadcrumb with route parameter navigation mode.
         * 
         * @param <C>
         *            the component with an URL parameter
         * @param displayName
         *            display name of the crumb
         * @param target
         *            the navigation target component
         * @param routeParameters
         *            the route parameters
         * @return an instance of the creation entry
         */
        public static <C extends Component & NavigationTarget> RouteParameterBreadcrumbCreationEntry<C> routeParameterTarget(@NonNull final String displayName,
                @NonNull final Class<C> target, @NonNull final RouteParameters routeParameters) {
            return new RouteParameterBreadcrumbCreationEntry<>(displayName, target, BreadcrumbNavigationMode.PARAMETERS, routeParameters);
        }
    }

    /**
     * The navigation mode for a crumb.
     */
    public enum BreadcrumbNavigationMode {
        /** Just navigate to a target, no parameters at all. **/
        PLAIN,
        /** A single route parameter. **/
        PARAMETER,
        /** Fully-fletched route parameters. **/
        PARAMETERS,
        /** No navigation at all. **/
        NONE
    }

    /**
     * A single crumb of the breadcrumbs.
     * 
     * @author BCE
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface BreadcrumbStep {

        public Class<? extends Component> __ROOT_COMPONENT__ = Component.class;

        /** The name of the crumb, as displayed in the UI. **/
        String name();

        /** The parent class. **/
        Class<? extends Component> parent() default Component.class;

        /** If the crumb is the highest (so has no other parent), then the name will be shown. Otherwise it will be the home icon. **/
        boolean showTextInsteadOfHomeIcon() default false;

        /**
         * The key(s) for the dynamic value; if multiple ones, order matters, the others are a fallback. Will be appended in the UI. Must be available via
         * {@link QueryParams} or path parameters.
         **/
        String[] dynamicTextKey() default "";

        /** If the dynamic text will append to the {@link #name()}, or will replace it. **/
        boolean dynamicTextAppend() default true;

        /** If the breadcrumbs are being created automatically. **/
        boolean autoCreate() default true;

        /** The name of the parameter. Used for parameter-based routing. **/
        String parameter() default "";

        /** The names of the route parameters. Used for route parameter-based routing. **/
        String[] routeParameters() default {};

        /** If the crumb shall be hidden in the UI if the last element. **/
        boolean hideIfLast() default false;

        /** If the crumb is the only one (and root), hide it. **/
        boolean hideIfRoot() default false;
    }

    /** Marks a component that contains breadcrumbs. **/
    public static interface BreadcrumbEnabledComponent {
        /** Get the breadcrumbs. **/
        @NonNull
        Breadcrumbs getBreadcrumbs();
    }

    /**
     * Marks a page that can be navigated to.
     * 
     * @author BCE
     * 
     * @see EnhancedNavigationTarget
     */
    public interface NavigationTarget extends BeforeEnterObserver {
        // nothing here
    }

    /**
     * Marks a page that can be navigated to.
     * 
     * @author BCE
     * 
     * @see NavigationTarget
     */
    public interface EnhancedNavigationTarget extends NavigationTarget {
        @Override
        default void beforeEnter(final BeforeEnterEvent event) {
            setParameter(new ExtendedRouteParameters(event.getRouteParameters()));
        }

        /**
         * Set the navigation parameters. Will be called during navigation before showing the page.
         */
        void setParameter(@NonNull ExtendedRouteParameters routeParameters);

        /** A wrapper for the route parameters. **/
        @Data
        public static class ExtendedRouteParameters {
            /** The wrapped parameters. **/
            @NonNull
            private final RouteParameters routeParameters;

            /** Get the parameter with the given key. Will fail if the value does not exist. **/
            @NonNull
            public String get(@NonNull final String parameterName) {
                final var value = get(parameterName, null);

                // add a null check
                Objects.requireNonNull(value, "The parameter " + parameterName + " does not exist!");
                return value;
            }

            /** Get the parameter with the given key. {@code null} if non-existing. **/
            @Nullable
            public String getOrNull(@NonNull final String parameterName) {
                return get(parameterName, null);
            }

            /** Get the parameter with the given key. If {@code null}, return the given default. **/
            @Nullable
            public String get(@NonNull final String parameterName, @Nullable final String defaultValue) {
                return this.routeParameters.get(parameterName).orElse(defaultValue);
            }
        }
    }
}
