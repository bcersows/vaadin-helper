# Vaadin UI Helpers
A set of small UI helpers and components for Vaadin that allow faster and safer UI creation and better-defined data flows.

## Helpers

The helpers streamline the data flow and component creation, and encapsulate common code.

### `VaadinUiHelper`: standard component creation & configuration, navigation

Allows for
 * easy navigation to app views
 * creation of horizontal and vertical layouts (`create[Vertical|Horizontal]Without`) with pre-configured padding and spacing settings, or expanded children
 * replacing the children of a component (`replace`)
 * exposes an arbitrary file for download in the browser (`startFileDownload`) by opening a new tab

### Background operations in tasks

These allow to do operations in a separate task and prevent blocking the UI thread. Expandable base class: `BackgroundOperationManager`.

#### `BackgroundLoadingHelper`: downloading data

Load data from a backend system and hide the UI while doing so using a simple skeleton approach (for areas with the CSS class `bg-loading-content`). On error, a notification will be shown to the user, if no custom error handling was provided.

Example usage including the optional data checking:

```
BackgroundLoadingHelper.startBackgroundLoading(
	// load the data
	() -> loadData(entityId),
	// update the UI, if verified
	this.updateUiConsumer::accept,
	// verify data alright
	dataContainer -> {
		if (null != dataContainer) {
			// check if editable
			if (checkEntityCanBeEdited(dataContainer)) {
				// all good, continue
				return true;
			} else {
				// if not editable, show a notification and change view
				NotificationHelper.showNotification("Entity is not editable.", false);
				event.forwardTo(this.listViewClass);
			}
		} else {
			// notify user
			NotificationHelper.showNotification("Entity not found!", false);
		}

		// not good, stop process
		return false;
	});
```

#### `BackgroundUpdateHelper`: sending data

Similar to the background loading helper; without UI skeleton, but allows for blocking the UI using a modal with configurable text.

```
BackgroundUpdateHelper.startBackgroundUpdate("my input", input -> {
      // call service instead
      try {
          Thread.sleep(100000);
      } catch (final InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }

      return "returned: " + input;
  }, responseData -> {
      log.info("Response data of background operation: {}.", responseData);
      this.add(responseData);
  });
```

#### Testing

Due to the asynchronous nature of background loading, the unit tests of views using it have to be adapted (a tiny bit). The `VaadinTestHelper` offers two functions for that:

* `prepareBackgroundLoading`: call in the `@Before` method to mock the test being in an actual web request
* `waitForBackgroundLoad`: after calling the respective method to populate the UI/load the data, use it to wait for the completion of the load; will throw an exception after 5s

(!) If the test fails due to an `InvalidStateException` caused by waiting too long, chances are high it’s not because of the functionality of the BLH, but because

* you forgot to prepare the test for background operations (or put it in the wrong place)
* there was a different exception which prevented the task from completing (scroll up in the logs to see it)

### `UiBinder`: bind data to UI fields and refresh after loading

The `UiBinder` allows to create and fill UI components on the fly, based on the loaded/given data. The configuration of the field bindings must be done before loading any data (e.g. in the constructor or a method called from there)! For a better separation of concerns, it’s possible to create a nested binder based on some sub-data. Works great together with the `BackgroundLoadingHelper`.

Some of the possibilities are:

* `set`, `setText`: set the text/value
* `setItems`: set the items of a `Grid`, `Select`, …
* `fillWith`: replace the content of a wrapper component (or the whole bound component)
* `addClickListener`: set a button click listener (overwriting previous listeners, so refresh-safe)
* `setRunnable`: run arbitrary code (once per binder; e.g. for `Breadcrumb`s)

The designed restrictions are:

* there can only be one binding per component, others will throw an exceptions
* the arbitrary code (`setRunnable`) can only be added once

Some simple (yet fully-fletched) usage would be:

<details>
<summary>Click to expand!</summary>
<p>

```
private final UiBinder<DashboardData, Void> uiBinder;

public DashboardTestView() {
    this.setSizeUndefined();
    this.uiBinder = createUi();
}

/**
* Create the UI.
*/
private final UiBinder<DashboardData, Void> createUi() {
    final var jsonLabel = new Pre();
    final var a1 = new Label();
    final var a2 = new TextField();
    final var list = new VerticalLayout();
    final var list2 = new VerticalLayout();
    final var nested1 = new H1();
    final var nested2 = new Label();
    final var nested3 = new Button();

    final var nested = VaadinUiHelper.createVerticalWithoutPadding(new H2("Nested"), nested1, nested2, nested3);
    final var nestedComponent = new Div();

    final var nestedBinderArea = VaadinUiHelper.createVerticalWithoutPadding();

    final var uiBinder = UiBinder.with(this, DashboardData.class,
            // add components
            jsonLabel, new Button("reload", clickEvent -> this.afterNavigation(null)),
            // actual components
            VaadinUiHelper.createHorizontalWithoutPadding(a1, a2), VaadinUiHelper.createHorizontalWithoutPadding(new H2("L1"), list, new H2("L2"), list2),
            VaadinUiHelper.createHorizontalWithoutPadding(nested, nestedComponent, nestedBinderArea));

    uiBinder.setText(jsonLabel, entity -> {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(entity);
        } catch (final JsonProcessingException e) {
            return "could not parse";
        }
    });
    uiBinder.setText(a1, DashboardData::getUsers);
    uiBinder.set(a2, DashboardData::getGroups);

    // from lists
    uiBinder.fillWith(list, entity -> entity.lists.stream().map(Label::new).collect(Collectors.toList()));
    uiBinder.fillWith(list2, DashboardData::getLists, listItems -> listItems.stream().map(Label::new).collect(Collectors.toList()));

    // nested
    uiBinder.setText(nested1, entity -> entity.getNested().n1);
    uiBinder.setText(nested2, entity -> entity.getNested().n2);
    uiBinder.setText(nested3, entity -> entity.getNested().n3);

    // nested component
    uiBinder.fillWith(nestedComponent, DashboardData::getNested, nestedData -> {
        final var toggleButton = new ToggleButton(nestedData.n3);
        return List.of(new H3("Created by binder"), new Label("Created in binder " + nestedData.n1), toggleButton);
    });

    // create nested binder
    uiBinder.createNested(nestedBinderArea, NestedData.class, DashboardData::getNested).fillWith(nestedEntity -> {
        final var label = new Label(nestedEntity.getN3());
        return List.of(new H2("From Nested Binder"), label);
    });

    return uiBinder;
}

@Override
public void afterNavigation(final AfterNavigationEvent afterNavigationEvent) {
    BackgroundLoadingHelper.startBackgroundLoading(
            // load the data
            () -> {
                final NestedData nested = new NestedData("n1", "n2", "n3");
                // return container
                return new DashboardData("users", "gr2", List.of("l1", "l2", "l3"), nested);
            },
            // update the UI
            this.uiBinder::load);
}

/** The data container. **/
@Data
@AllArgsConstructor
private class DashboardData {
    /** Users count. **/
    private final String users;
    /** Groups count. **/
    private final String groups;

    private final List<String> lists;
    private final NestedData nested;
}

@Data
@AllArgsConstructor
private class NestedData {
    private final String n1;
    private final String n2;
    private final String n3;
}
```

</p>
</details>

### `UiFlowBinder`: influence a component based on the state of others

Enable a component based on the values of other components, either dependencies (must be valid) or adversaries (must not have a value).

### `EventHandlerHelper`: event handling with more flexible type casting than the default

Allows for adding listeners and firing complex events to them, e.g. for events with generics. Provides handlers for `ComponentEvent`s and `ValueChangedEvent`s. Usage:

```
private final ValueChangedEventHandler<ComplexValueComponent, ComplexValueChangedEvent, ValueChangeListener<ComplexValueChangedEvent>> eventHandler = new ValueChangedEventHandler<>();
private final ComponentEventHandler<ComplexComponent, ComplexComponentEvent, ComponentEventListener<ComplexComponentEvent>> eventHandler = new ComponentEventHandler<>();
```

### `NotificationHelper`: wrapper for notifications

Allows for default notifications (success, error, plain), but also for warnings and exception messages that will print the whole stack trace when used in combination with `showException` and configured to do so (`setConfig`).

## Components

There is a bunch of components that can easily be added to existing views.

### `Breadcrumbs`: a whole generic breadcrumb implementation

This breadcrumbs implementation allows for automatic or manual creation of breadcrumbs based on annotations. It is designed for a separation of the application in actual views (having the `BreadcrumbStep` annotations) and the general layout (having the actual `Breadcrumbs` component including the `BreadcrumbEnabledComponent` interface).  
A response design is implemented so that the middle breadcrumbs steps will be hidden as the screen width gets smaller.

Requirements:

* breadcrumbs added somewhere to the layout
* the view (and potential parent views) has the `BreadcrumbStep` annotation (specifying the name/text, the parent view, the parameters required for navigating to the view, and if to create it automatically)
* starting the population, either automatically in the layout after navigating to a view or manually after loading the data

### `Badge`: a simple badge for displaying tags, versions, etc

A small, stylable badge coming with different sizes. Create using `new Badge("1.6").small().bright()`.

### `ToggleButton`: a checkbox implementation styled as a slider

The checkbox is styled like a slider based on [this implementation](https://www.w3schools.com/howto/howto_css_switch.asp).