package de.bcersows.vaadinhelper.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

import de.bcersows.vaadinhelper.VaadinTestHelper;
import de.bcersows.vaadinhelper.components.Breadcrumbs.BreadcrumbCreationData;
import de.bcersows.vaadinhelper.components.Breadcrumbs.BreadcrumbCreationEntry;
import de.bcersows.vaadinhelper.components.Breadcrumbs.BreadcrumbCreationLegacyData;
import de.bcersows.vaadinhelper.components.Breadcrumbs.BreadcrumbEnabledComponent;
import de.bcersows.vaadinhelper.components.Breadcrumbs.BreadcrumbStep;
import de.bcersows.vaadinhelper.components.Breadcrumbs.NavigationTarget;
import de.bcersows.vaadinhelper.components.Breadcrumbs.ParameterBreadcrumbCreationEntry;
import de.bcersows.vaadinhelper.components.Breadcrumbs.RouteParameterBreadcrumbCreationEntry;

public class BreadcrumbsTest {

    private static final String L2_PARAM_NAME = "param";
    private static final String L2_2_PARAM_NAME = "param2";
    private static final String L3_PARAM_2 = "query2";
    private static final String L3_PARAM_1 = "query1";

    private Breadcrumbs component;
    private Map<String, String> parameters;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockVaadin.setup(new Routes(Set.of(Level1.class, Level1Showing.class, Level2.class, Level22.class, Level22Plain.class, Level3.class, Level4.class,
                Level3Manual.class, NestedLevel2.class, NestedLevel22.class, NestedLevel22Plain.class, NestedLevel22Hidden.class), Set.of(), true));
        this.component = new Breadcrumbs();
        this.parameters = Map.of(L3_PARAM_2, "p2", L3_PARAM_1, "p1", L2_PARAM_NAME, "lala");
    }

    @AfterEach
    public void afterEach() throws Exception {
        MockVaadin.tearDown();
        this.component = null;
        this.parameters = null;
    }

    @Nested
    public class AutomaticCreationTest {
        @Test
        public void testPopulateBreadcrumbs() throws Exception {
            final var routeParameters = new RouteParameters(parameters);
            component.tryPopulatingBreadcrumbs((BreadcrumbStep) null, routeParameters);
            assertFalse(component.isVisible(), "Component invisible, as no crumbs.");

            final BreadcrumbStep manualBreadcrumbStep = Level3Manual.class.getAnnotation(BreadcrumbStep.class);
            component.tryPopulatingBreadcrumbs(manualBreadcrumbStep, routeParameters);
            assertFalse(component.isVisible(), "Component invisible, as not auto-creating enabled.");

            final BreadcrumbStep breadcrumbStep = Level3.class.getAnnotation(BreadcrumbStep.class);
            component.tryPopulatingBreadcrumbs(breadcrumbStep, routeParameters);
            assertTrue(component.isVisible(), "Component visible, as crumbs exist.");
            assertEquals(5, VaadinTestHelper.extractRootStructure(component).getFirst().size(), "correct amount of crumbs added");
        }

        @Test
        public void testCollectCrumbsFromAnnotation() throws Exception {
            final BreadcrumbStep breadcrumbStep = Level3.class.getAnnotation(BreadcrumbStep.class);
            final var list = component.collectCrumbsFromAnnotation(breadcrumbStep, new HashMap<>());

            assertEquals(3, list.getEntries().size(), "Size of result matches, three levels detected");

            assertEquals("Level1", list.getEntries().get(0).name, "Order of levels is correct, first name matches");
            assertEquals(Level1.class, list.getEntries().get(0).navigationTarget, "Order of levels is correct, first navigation target matches.");

            assertEquals("Level2", list.getEntries().get(1).name, "Order of levels is correct, second name matches");
            assertEquals(Level2.class, list.getEntries().get(1).navigationTarget, "Order of levels is correct, second navigation target matches.");

            assertEquals("Level3", list.getEntries().get(2).name, "Order of levels is correct, third name matches");
            assertNull(list.getEntries().get(2).navigationTarget, "Order of levels is correct, third navigation target is null.");
        }
    }

    @Nested
    public class UtilityTest {
        @Test
        public void testTryToFindBreadcrumbStep() throws Exception {
            final var layout = new Layout();
            final var l1 = new Level1();
            final var l2 = new Level2();

            final List<HasElement> activeElements = List.of(l2, l1, layout);

            final var step = Breadcrumbs.tryToFindBreadcrumbStep(activeElements);
            assertEquals("Level2", step.name(), "Found the correct step.");
        }
    }

    @Nested
    public class FindAndUpdateTest {

        @Test
        public void testFindAndUpdateBreadcrumbs() throws Exception {
            final var layout = new Layout();
            final var l1 = new Level1();
            final var l2 = new Level2();

            final List<HasElement> activeElements = List.of(l2, l1, layout);

            layout.getBreadcrumbs().setVisible(false);

            // test with an auto-creating crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, parameters);
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");

            // test with a manual crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(List.of(new Level3Manual(), l2, l1, layout), parameters);
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");
        }

        @Test
        public void testFindAndUpdateBreadcrumbsHtml() throws Exception {
            // test the breadcrumb HTML
            final var layout = new Layout();
            final var l22plain = new Level22Plain();

            final List<HasElement> activeElements = List.of(l22plain, layout);

            layout.getBreadcrumbs().setVisible(false);

            // test with an auto-creating crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, Map.of(L2_PARAM_NAME, "l2p", L2_2_PARAM_NAME, "l22p"));
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");

            final var breadcrumbChildren = layout.getBreadcrumbs().getElement().getChild(0).getChildren().collect(Collectors.toList());
            assertEquals(7, breadcrumbChildren.size(), "Amount with all showing matches.");
            testHtmlExceptLast(breadcrumbChildren);

            final var child3 = breadcrumbChildren.get(6).getComponent().get();
            assertTrue(child3 instanceof Label, "C3 -- last item -- is a label.");
            assertEquals("Level22plain", ((Label) child3).getText(), "C3 text matches.");
            assertTrue(((HasStyle) child3).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertTrue(((HasStyle) child3).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
        }

        @Test
        public void testFindAndUpdateBreadcrumbsHtml_lastIsHidden() throws Exception {
            // test the breadcrumb HTML; same creation as other HTML test, but with hidden last element
            final var layout = new Layout();
            final var l22hidden = new NestedLevel22Hidden();

            final List<HasElement> activeElements = List.of(l22hidden, layout);

            layout.getBreadcrumbs().setVisible(false);

            // test with an auto-creating crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, Map.of(L2_PARAM_NAME, "l2p", L2_2_PARAM_NAME, "l22p"));
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");

            final var breadcrumbChildren = layout.getBreadcrumbs().getElement().getChild(0).getChildren().collect(Collectors.toList());
            assertEquals(5, breadcrumbChildren.size(), "Amount with last hidden matches.");
            testHtmlExceptLast(breadcrumbChildren);
        }

        /** Test the elements except the last. **/
        private void testHtmlExceptLast(final List<Element> breadcrumbChildren) {
            breadcrumbChildren.forEach(child -> System.err.println("- " + child));

            assertTrue(breadcrumbChildren.get(0).getChild(0).toString().contains("iron-icon"), "The first element is an icon.");
            final var child1 = breadcrumbChildren.get(2).getComponent().get();
            assertEquals(RouterLink.class, child1.getClass(), "C1 is a router link.");
            assertEquals("l2p", ((RouterLink) child1).getText(), "C1 text matches.");
            assertTrue(((HasStyle) child1).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertFalse(((HasStyle) child1).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
            final var child2 = breadcrumbChildren.get(4).getComponent().get();
            assertEquals(RouterLink.class, child2.getClass(), "C2 is a router link.");
            assertEquals("l22p", ((RouterLink) child2).getText(), "C2 text matches.");
            assertTrue(((HasStyle) child2).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertFalse(((HasStyle) child2).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
        }

        @Test
        public void testFindAndUpdateBreadcrumbsHtml_onlyRoot() throws Exception {
            // test the breadcrumb HTML; same creation as other HTML test, but with hidden last element
            final var layout = new Layout();
            final var rootElement = new Level1();

            final List<HasElement> activeElements = List.of(rootElement, layout);

            layout.getBreadcrumbs().setVisible(false);

            // test with an auto-creating crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, Map.of());
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");

            final var breadcrumbsComponent = layout.getBreadcrumbs().getElement().getChild(0);

            System.out.println(breadcrumbsComponent);

            final var breadcrumbChildrenAmount = breadcrumbsComponent.getChildren().count();
            assertEquals(0, breadcrumbChildrenAmount, "As only root, no elements.");
        }

        @Test
        public void testFindAndUpdateBreadcrumbsHtml_onlyRootShowing() throws Exception {
            // test the breadcrumb HTML; same creation as other HTML test, but with hidden last element
            final var layout = new Layout();
            final var rootElement = new Level1Showing();

            final List<HasElement> activeElements = List.of(rootElement, layout);

            layout.getBreadcrumbs().setVisible(false);

            // test with an auto-creating crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, Map.of());
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");

            final var breadcrumbsComponent = layout.getBreadcrumbs().getElement().getChild(0);

            System.out.println(breadcrumbsComponent);

            final var breadcrumbChildren = layout.getBreadcrumbs().getElement().getChild(0).getChildren().collect(Collectors.toList());
            assertEquals(2, breadcrumbChildren.size(), "Only root element and separator.");

            final var root = breadcrumbChildren.get(1);
            assertEquals("Level1", root.getText(), "Root element with text instead of icon.");

            assertTrue(root.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB));
            assertTrue(root.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
        }

        @Test
        public void testFindAndUpdateBreadcrumbsNested() throws Exception {
            // test with nested parameters
            final var layout = new Layout();
            final var l2 = new NestedLevel2();
            final var l22 = new NestedLevel22();
            final var l22plain = new NestedLevel22Plain();

            final List<HasElement> activeElements = List.of(l22plain, l22, l2, layout);

            layout.getBreadcrumbs().setVisible(false);

            // test with an auto-creating crumb
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, Map.of(L2_PARAM_NAME, "l2p", L2_2_PARAM_NAME, "l22p"));
            assertTrue(layout.getBreadcrumbs().isVisible(), "Breadcrumbs component was made visible.");

            final var breadcrumbChildren = layout.getBreadcrumbs().getElement().getChild(0).getChildren().collect(Collectors.toList());
            breadcrumbChildren.forEach(child -> System.err.println("- " + child));

            assertEquals(7, breadcrumbChildren.size(), "Children count matches.");

            assertTrue(breadcrumbChildren.get(0).getChild(0).toString().contains("iron-icon"), "The first element is an icon.");
            final var child1 = breadcrumbChildren.get(2).getComponent().get();
            assertEquals(RouterLink.class, child1.getClass(), "C1 is a router link.");
            assertEquals("l2p", ((RouterLink) child1).getText(), "C1 text matches.");
            assertTrue(((HasStyle) child1).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertFalse(((HasStyle) child1).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
            final var child2 = breadcrumbChildren.get(4).getComponent().get();
            assertEquals(RouterLink.class, child2.getClass(), "C2 is a router link.");
            assertEquals("l22p", ((RouterLink) child2).getText(), "C2 text matches.");
            assertTrue(((HasStyle) child2).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertFalse(((HasStyle) child2).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));

            final var child3 = breadcrumbChildren.get(6).getComponent().get();
            assertTrue(child3 instanceof Label, "C3 -- last item -- is a label.");
            assertEquals("Level22plain", ((Label) child3).getText(), "C3 text matches.");
            assertTrue(((HasStyle) child3).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertTrue(((HasStyle) child3).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
        }

        @Test
        public void testFindAndUpdateBreadcrumbs_fromUi() {
            final UI ui = Mockito.mock(UI.class);
            final var layout = new Layout();
            final Stream<Component> uiChildren = Stream.of(layout);
            Mockito.doReturn(uiChildren).when(ui).getChildren();

            final var currentView = new NestedLevel22();

            Breadcrumbs.findAndUpdateBreadcrumbs(ui, currentView, Map.of(L2_PARAM_NAME, "l2p", L2_2_PARAM_NAME, "l22p"));

            final var breadcrumbChildren = layout.getBreadcrumbs().getElement().getChild(0).getChildren().collect(Collectors.toList());
            assertEquals(5, breadcrumbChildren.size(), "Children count matches.");

            // no need for any further tests, as this is mostly about detecting the elements

        }
    }

    @Nested
    public class ManualCreation {
        @Test
        public void testFindAndUpdateBreadcrumbsManually() {
            final var layout = new Layout();
            final var l1 = new Level1();
            final var l2 = new Level2();
            final var l3 = new Level3();

            final List<HasElement> activeElements = List.of(l3, l2, l1, layout);

            final List<BreadcrumbCreationEntry<?>> entries = new ArrayList<>();
            entries.add(BreadcrumbCreationEntry.plainTarget("Test L1", Level1.class));
            entries.add(ParameterBreadcrumbCreationEntry.parameterTarget("L2", Level2.class, "hi"));
            entries.add(RouteParameterBreadcrumbCreationEntry.routeParameterTarget("L3", Level3.class, Map.of(L3_PARAM_1, "hi", L3_PARAM_2, "there")));

            final BreadcrumbCreationData breadcrumbCreationData = new BreadcrumbCreationData(true, entries);
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, breadcrumbCreationData);

            final var breadcrumbs = layout.getBreadcrumbs();
            assertTrue(breadcrumbs.isVisible(), "Breadcrumbs component was made visible.");
            assertEquals(5, breadcrumbs.getElement().getChild(0).getChildCount(), "Children count matches.");

            breadcrumbs.getElement().getChild(0).getChildren().forEach(child -> {
                System.out.println(" - " + child);

                assertTrue(child.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB) || child.getClassList().contains(Breadcrumbs.CSS_CLASS_SEPARATOR),
                        "every child has the base or separator classes");
                assertFalse(child.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN), "no child has the plain class");
            });
        }

        @Test
        public void testFindAndUpdateBreadcrumbsWithNoNavigationCrumbManually() {
            final var layout = new Layout();
            final var l2 = new Level2();
            final var l3 = new Level3();
            final var l4 = new Level4();

            final List<HasElement> activeElements = List.of(l3, l2, l4, layout);

            final List<BreadcrumbCreationEntry<?>> entries = new ArrayList<>();
            entries.add(ParameterBreadcrumbCreationEntry.parameterTarget("L2", Level2.class, "hi"));
            entries.add(RouteParameterBreadcrumbCreationEntry.routeParameterTarget("L3", Level3.class, Map.of(L3_PARAM_1, "hi", L3_PARAM_2, "there")));
            entries.add(BreadcrumbCreationEntry.noNavigation("L4"));

            final BreadcrumbCreationData breadcrumbCreationData = new BreadcrumbCreationData(true, entries);
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, breadcrumbCreationData);

            final var breadcrumbs = layout.getBreadcrumbs();
            assertTrue(breadcrumbs.isVisible(), "Breadcrumbs component was made visible.");
            assertEquals(5, breadcrumbs.getElement().getChild(0).getChildCount(), "Children count matches.");

            breadcrumbs.getElement().getChild(0).getChildren().forEach(child -> {
                System.out.println(" - " + child);

                assertTrue(child.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB) || child.getClassList().contains(Breadcrumbs.CSS_CLASS_SEPARATOR),
                        "every child has the base or separator classes");

            });

            final var breadcrumbChildren = layout.getBreadcrumbs().getElement().getChild(0).getChildren().collect(Collectors.toList());

            // test if the last crumb is a label.
            final var child = breadcrumbChildren.get(4).getComponent().get();
            assertTrue(child instanceof Label, "Child is a Label.");
            assertEquals("L4", ((Label) child).getText(), "Child text matches.");
            assertTrue(((HasStyle) child).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB));
            assertTrue(((HasStyle) child).hasClassName(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN));
        }

        @Test
        public void testFindAndUpdateBreadcrumbsManuallyWithLegacyData() {
            final var layout = new Layout();
            final var l1 = new Level1();
            final var l2 = new Level2();
            final var l3 = new Level3();

            final List<HasElement> activeElements = List.of(l3, l2, l1, layout);

            final List<BreadcrumbCreationEntry<?>> entries = new ArrayList<>();
            entries.add(RouteParameterBreadcrumbCreationEntry.routeParameterTarget("L3", Level3.class, Map.of(L3_PARAM_1, "L3", L3_PARAM_2, "there")));
            entries.add(RouteParameterBreadcrumbCreationEntry.routeParameterTarget("L3 2", Level3.class, Map.of(L3_PARAM_1, "L3 2", L3_PARAM_2, "query2")));

            final BreadcrumbCreationLegacyData legacyData = new BreadcrumbCreationLegacyData(Level2.class, Map.of(L2_PARAM_NAME, "providedparam"));
            final BreadcrumbCreationData breadcrumbCreationData = new BreadcrumbCreationData(true, legacyData, entries);
            Breadcrumbs.findAndUpdateBreadcrumbs(activeElements, breadcrumbCreationData);

            final var breadcrumbs = layout.getBreadcrumbs();
            assertTrue(breadcrumbs.isVisible(), "Breadcrumbs component was made visible.");
            assertEquals(7, breadcrumbs.getElement().getChild(0).getChildCount(), "Children count matches, as also included legacy data.");

            System.out.println("Children:");
            breadcrumbs.getElement().getChild(0).getChildren().forEach(child -> {
                System.out.println(" - " + child);

                assertTrue(child.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB) || child.getClassList().contains(Breadcrumbs.CSS_CLASS_SEPARATOR),
                        "every child has the base or separator classes");
                assertFalse(child.getClassList().contains(Breadcrumbs.CSS_CLASS_CRUMB_PLAIN), "no child has the plain class");
            });
        }
    }

    /** Layout fake. **/
    private static class Layout extends Div implements BreadcrumbEnabledComponent {
        private final Breadcrumbs breadcrumbs;

        public Layout() {
            this.breadcrumbs = new Breadcrumbs();
        }

        @Override
        public Breadcrumbs getBreadcrumbs() {
            return this.breadcrumbs;
        }

    }

    /** Hierarchical view, L1. **/
    @BreadcrumbStep(name = "Level1", hideIfRoot = true)
    @Route("l1")
    private static class Level1 extends Div {

    }

    /** Hierarchical view, L1. **/
    @BreadcrumbStep(name = "Level1", showTextInsteadOfHomeIcon = true, hideIfRoot = false)
    @Route("l1s")
    private static class Level1Showing extends Div {

    }

    /** Hierarchical view, L2. **/
    @BreadcrumbStep(name = "Level2", parent = Level1.class, dynamicTextKey = L2_PARAM_NAME, dynamicTextAppend = false, parameter = L2_PARAM_NAME)
    @Route("l2")
    private static class Level2 extends Div implements HasUrlParameter<String> {

        @Override
        public void setParameter(final BeforeEvent event, final String parameter) {
            // nothing
        }
    }

    /** Hierarchical view, L22. **/
    @BreadcrumbStep(name = "Level22", parent = Level2.class, dynamicTextKey = L2_2_PARAM_NAME, dynamicTextAppend = false, parameter = L2_2_PARAM_NAME)
    @Route("l22")
    private static class Level22 extends Div implements HasUrlParameter<String> {

        @Override
        public void setParameter(final BeforeEvent event, final String parameter) {
            // nothing
        }
    }

    /** Hierarchical view, L22plain. **/
    @BreadcrumbStep(name = "Level22plain", parent = Level22.class)
    @Route("plain")
    private static class Level22Plain extends Div {
        // nothing
    }

    /** Hierarchical view, L3. **/
    @BreadcrumbStep(name = "Level3", parent = Level2.class, routeParameters = { L3_PARAM_1, L3_PARAM_2 })
    @Route("l3/:" + L3_PARAM_1 + "/:" + L3_PARAM_2)
    private static class Level3 extends Div implements NavigationTarget {
        @Override
        public void beforeEnter(final BeforeEnterEvent event) {
            // nothing
        }
    }

    /** Hierarchical view, L4. **/
    @BreadcrumbStep(name = "Level4", parent = Level3.class)
    @Route("l4")
    private static class Level4 extends Div {
    }

    /** Hierarchical view, L3, manual creation. **/
    @BreadcrumbStep(name = "Level3Manual", parent = Level2.class, autoCreate = false)
    @Route("l3m")
    private static class Level3Manual extends Div {

    }

    /** Hierarchical view, L2. **/
    @BreadcrumbStep(name = "Level2", parent = Level1.class, dynamicTextKey = L2_PARAM_NAME, dynamicTextAppend = false, parameter = L2_PARAM_NAME)
    @Route("n2")
    private static class NestedLevel2 extends Div implements HasUrlParameter<String>, RouterLayout {

        @Override
        public void setParameter(final BeforeEvent event, final String parameter) {
            // nothing
        }
    }

    /** Hierarchical view, L22. **/
    @BreadcrumbStep(name = "Level22", parent = NestedLevel2.class, dynamicTextKey = L2_2_PARAM_NAME, dynamicTextAppend = false, parameter = L2_2_PARAM_NAME)
    @Route(value = "n22", layout = NestedLevel2.class)
    private static class NestedLevel22 extends Div implements HasUrlParameter<String>, RouterLayout {

        @Override
        public void setParameter(final BeforeEvent event, final String parameter) {
            // nothing
        }
    }

    /** Hierarchical view, L22plain. **/
    @BreadcrumbStep(name = "Level22plain", parent = NestedLevel22.class)
    @Route(value = "nplain", layout = NestedLevel22.class)
    private static class NestedLevel22Plain extends Div {
        // nothing
    }

    /** Hierarchical view, L22hidden. **/
    @BreadcrumbStep(name = "Level22hidden", parent = NestedLevel22.class, hideIfLast = true)
    @Route(value = "nhidden", layout = NestedLevel22.class)
    private static class NestedLevel22Hidden extends Div {
        // nothing
    }
}