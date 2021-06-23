package de.bcersows.vaadinhelper;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;

import de.bcersows.vaadinhelper.components.Breadcrumbs.NavigationTarget;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for common Vaadin UI operations.
 * 
 * @author bcersows
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class VaadinUiHelper {

    // navigation
    /**
     * Navigate to the given class.
     *
     * @param navigationTarget
     *            the class to navigate to; has to accept route parameters
     * @param routeParameters
     *            the parameters for the navigation
     */
    public static <C extends Component & NavigationTarget> void navigate(@NonNull final Class<? extends C> navigationTarget,
            @NonNull final Map<String, String> routeParameters) {
        navigate(navigationTarget, new RouteParameters(routeParameters));
    }

    /**
     * Navigate to the given class.
     *
     * @param navigationTarget
     *            the class to navigate to; has to accept route parameters
     * @param routeParameters
     *            the parameters for the navigation
     */
    public static <C extends Component & NavigationTarget> void navigate(@NonNull final Class<? extends C> navigationTarget,
            @NonNull final RouteParameters routeParameters) {
        final var route = navigationTarget.getAnnotation(Route.class);
        if (null != route) {
            final var ui = UI.getCurrent();
            ui.navigate(navigationTarget, routeParameters);
        } else {
            log.warn("The component {} does not have a route.", navigationTarget);
        }
    }

    // component creation and handling
    /** Create a vertical layout without default spacing. **/
    @NonNull
    public static VerticalLayout createVerticalWithoutSpacing() {
        return createVerticalWithoutSpacing((Component[]) null);
    }

    /** Create a vertical layout without default spacing. **/
    @NonNull
    public static VerticalLayout createVerticalWithoutSpacing(@Nullable final Component... components) {
        return createVertical(true, false, components);
    }

    /** Create a vertical layout without default padding. **/
    @NonNull
    public static VerticalLayout createVerticalWithoutPadding() {
        return createVerticalWithoutPadding((Component[]) null);
    }

    /** Create a vertical layout without default padding. **/
    @NonNull
    public static VerticalLayout createVerticalWithoutPadding(@Nullable final Component... components) {
        return createVertical(false, true, components);
    }

    /** Create a plain vertical layout without default spacing and padding. **/
    @NonNull
    public static VerticalLayout createPlainVertical() {
        return createPlainVertical((Component[]) null);
    }

    /** Create a plain vertical layout without default spacing and padding. **/
    @NonNull
    public static VerticalLayout createPlainVertical(@Nullable final Component... components) {
        return createVertical(false, false, components);
    }

    /** Create a vertical layout with the set flags and components. **/
    @NonNull
    private static VerticalLayout createVertical(final boolean withPadding, final boolean withSpacing, @Nullable final Component... components) {
        // create the layout
        final var verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(withPadding);
        verticalLayout.setSpacing(withSpacing);

        // add components, if any
        if (null != components) {
            verticalLayout.add(components);
        }

        // return it
        return verticalLayout;
    }

    /** Create a horizontal layout without default spacing. **/
    @NonNull
    public static HorizontalLayout createHorizontalWithoutSpacing() {
        return createHorizontalWithoutSpacing((Component[]) null);
    }

    /** Create a horizontal layout without default spacing. **/
    @NonNull
    public static HorizontalLayout createHorizontalWithoutSpacing(@Nullable final Component... components) {
        return createHorizontal(true, false, components);
    }

    /** Create a horizontal layout without default padding. **/
    @NonNull
    public static HorizontalLayout createHorizontalWithoutPadding() {
        return createHorizontalWithoutPadding((Component[]) null);
    }

    /** Create a horizontal layout without default padding. **/
    @NonNull
    public static HorizontalLayout createHorizontalWithoutPadding(@Nullable final Component... components) {
        return createHorizontal(false, true, components);
    }

    /** Create a plain horizontal layout without default spacing and padding. **/
    @NonNull
    public static HorizontalLayout createPlainHorizontal() {
        return createPlainHorizontal((Component[]) null);
    }

    /** Create a plain horizontal layout without default spacing and padding. **/
    @NonNull
    public static HorizontalLayout createPlainHorizontal(@Nullable final Component... components) {
        return createHorizontal(false, false, components);
    }

    /**
     * Create a horizontal layout with spacing, but no padding. Expand the first component.
     * 
     * @param expandableComponent
     *            the component to expand, will be added as first
     * @param components
     *            other components
     * @return the created layout
     */
    @NonNull
    public static HorizontalLayout createHorizontalExpanded(@NonNull final Component expandableComponent, @Nullable final Component... components) {
        // create the layout and expand the first component
        final var layout = createHorizontal(false, true, components);
        layout.addComponentAsFirst(expandableComponent);
        layout.expand(expandableComponent);

        // default to full width
        layout.setWidthFull();

        // return the layout
        return layout;
    }

    /** Create an horizontal layout with the set flags and components. **/
    @NonNull
    private static HorizontalLayout createHorizontal(final boolean withPadding, final boolean withSpacing, @Nullable final Component... components) {
        // create the layout
        final var hoprizontalLayout = new HorizontalLayout();
        hoprizontalLayout.setPadding(withPadding);
        hoprizontalLayout.setSpacing(withSpacing);

        // add components, if any
        if (null != components) {
            hoprizontalLayout.add(components);
        }

        // return it
        return hoprizontalLayout;
    }

    /**
     * Removes all components of the wrapper and replaces its contents with the given ones.
     * 
     * @param <T>
     *            the type of the wrapper component
     * @param wrapper
     *            the wrapper component
     * @param componentsToReplaceWith
     *            the components to replace with
     * @return the given wrapper
     */
    @NonNull
    public static <T extends Component & HasComponents> T replace(@NonNull final T wrapper, @NonNull final Component... componentsToReplaceWith) {
        // remove all and add new
        wrapper.removeAll();
        wrapper.add(componentsToReplaceWith);

        // return wrapper
        return wrapper;
    }

    /**
     * Start a file download in the browser with the given content.
     * 
     * @param currentUi
     *            the current UI (e.g. {@code UI.getCurrent()})
     * @param fileName
     *            the name of the download file
     * @param contentInputStreamFactory
     *            the factory for the input stream, e.g. {@code () -> new ByteArrayInputStream("text".getBytes())}
     */
    public static void startFileDownload(@NonNull final UI currentUi, @NonNull final String fileName,
            @NonNull final InputStreamFactory contentInputStreamFactory) {
        // create a stream resource serving the content...
        final var streamResource = new StreamResource(fileName, contentInputStreamFactory);
        final var registeredResource = currentUi.getSession().getResourceRegistry().registerResource(streamResource);
        // ... and open it in a new tab
        currentUi.getPage().open(registeredResource.getResourceUri().toString());
        // unregister the resource after a bit
        Executors.newSingleThreadScheduledExecutor().schedule(registeredResource::unregister, 10, TimeUnit.SECONDS);
    }
}
