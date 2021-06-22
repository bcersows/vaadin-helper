package de.bcersows.vaadinhelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NavigatorKt;
import com.github.mvysny.kaributesting.v10.Routes;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import de.bcersows.vaadinhelper.components.Breadcrumbs.NavigationTarget;

public class VaadinUiHelperTest {

    @Test
    public void testStartFileDownload() throws Exception {
        try {
            MockVaadin.setup();
            final var ui = Mockito.spy(UI.getCurrent());
            final var page = Mockito.spy(new Page(ui));
            Mockito.doReturn(page).when(ui).getPage();
            Mockito.doNothing().when(page).open(Mockito.anyString());

            VaadinUiHelper.startFileDownload(ui, "myfile.txt", () -> new ByteArrayInputStream("test".getBytes()));
            Mockito.verify(page).open(Mockito.anyString());
        } finally {
            MockVaadin.tearDown();
        }
    }

    @Nested
    public static class LayoutTests {
        @Test
        public void testCreateHorizontal() throws Exception {
            final Label l = new Label("test");
            HorizontalLayout layout;

            layout = VaadinUiHelper.createHorizontalWithoutSpacing();
            assertTrue(layout.isPadding(), "Padding yes");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(0, layout.getChildren().count(), "no child");
            layout = VaadinUiHelper.createHorizontalWithoutSpacing(l);
            assertTrue(layout.isPadding(), "Padding yes");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(1, layout.getChildren().count(), "child added");

            layout = VaadinUiHelper.createHorizontalWithoutPadding();
            assertFalse(layout.isPadding(), "Padding no");
            assertTrue(layout.isSpacing(), "Spacing yes");
            assertEquals(0, layout.getChildren().count(), "no child");
            layout = VaadinUiHelper.createHorizontalWithoutPadding(l);
            assertFalse(layout.isPadding(), "Padding no");
            assertTrue(layout.isSpacing(), "Spacing yes");
            assertEquals(1, layout.getChildren().count(), "child added");

            layout = VaadinUiHelper.createPlainHorizontal();
            assertFalse(layout.isPadding(), "Padding no");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(0, layout.getChildren().count(), "no child");
            layout = VaadinUiHelper.createPlainHorizontal(l);
            assertFalse(layout.isPadding(), "Padding no");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(1, layout.getChildren().count(), "child added");
        }

        @Test
        public void testCreateVertical() throws Exception {
            final Label l = new Label("test");
            VerticalLayout layout;

            layout = VaadinUiHelper.createVerticalWithoutSpacing();
            assertTrue(layout.isPadding(), "Padding yes");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(0, layout.getChildren().count(), "no child");
            layout = VaadinUiHelper.createVerticalWithoutSpacing(l);
            assertTrue(layout.isPadding(), "Padding yes");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(1, layout.getChildren().count(), "child added");

            layout = VaadinUiHelper.createVerticalWithoutPadding();
            assertFalse(layout.isPadding(), "Padding no");
            assertTrue(layout.isSpacing(), "Spacing yes");
            assertEquals(0, layout.getChildren().count(), "no child");
            layout = VaadinUiHelper.createVerticalWithoutPadding(l);
            assertFalse(layout.isPadding(), "Padding no");
            assertTrue(layout.isSpacing(), "Spacing yes");
            assertEquals(1, layout.getChildren().count(), "child added");

            layout = VaadinUiHelper.createPlainVertical();
            assertFalse(layout.isPadding(), "Padding no");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(0, layout.getChildren().count(), "no child");
            layout = VaadinUiHelper.createPlainVertical(l);
            assertFalse(layout.isPadding(), "Padding no");
            assertFalse(layout.isSpacing(), "Spacing no");
            assertEquals(1, layout.getChildren().count(), "child added");
        }

        @Test
        public void testCreateHorizontalExpanded() throws Exception {
            final var label = new Label("title");
            final var label2 = new Label("other");
            final var label3 = new Label("data");

            final var expandedLayout = VaadinUiHelper.createHorizontalExpanded(label, label2, label3);

            assertFalse(expandedLayout.isPadding(), "Padding no");
            assertTrue(expandedLayout.isSpacing(), "Spacing yes");
            assertEquals(3, expandedLayout.getChildren().count(), "children added");

            assertEquals("100%", expandedLayout.getElement().getStyle().get("width"), "full width defaulted");

            final var firstChild = expandedLayout.getElement().getChild(0);
            assertEquals("title", firstChild.getText(), "correct label as first");
            assertEquals("1.0", firstChild.getStyle().get("flex-grow"), "flex added");

            assertFalse(expandedLayout.getElement().getChild(1).getStyle().has("flex-grow"), "mo flex added for other content");
            assertFalse(expandedLayout.getElement().getChild(2).getStyle().has("flex-grow"), "mo flex added for other content");

            // only single element
            final var expandedLayoutSingle = VaadinUiHelper.createHorizontalExpanded(label);
            assertEquals(1, expandedLayoutSingle.getChildren().count(), "single child");
            final var firstChildSingle = expandedLayoutSingle.getElement().getChild(0);
            assertEquals("1.0", firstChildSingle.getStyle().get("flex-grow"), "flex added");
            assertEquals("title", firstChildSingle.getText(), "correct label as first");
        }

        @Test
        public void testReplace() throws Exception {
            // create
            final var former = new Label("former");
            final var layout = new HorizontalLayout(former, new Label("f2"));
            assertEquals(2, layout.getChildren().count(), "children added");

            // replace
            assertEquals(layout, VaadinUiHelper.replace(layout, new Label("foo"), new Paragraph("bar")), "returned wrapper");
            assertEquals(2, layout.getChildren().count(), "children replaced");

            assertEquals("foo", layout.getElement().getChild(0).getText(), "replaced");
            assertEquals("bar", layout.getElement().getChild(1).getText(), "replaced");

            assertTrue(former.getParent().isEmpty(), "no parent anymore");
        }
    }

    @Nested
    public class NavigationTest {
        @BeforeEach
        public void beforeEach() {
            final Routes routes = new Routes(Set.of(TestParamsTarget.class), Set.of(), true);
            MockVaadin.setup(routes);
        }

        @AfterEach
        public void afterEach() {
            MockVaadin.tearDown();
        }

        @Test
        public void testNavigate() {
            VaadinUiHelper.navigate(TestParamsTarget.class, Map.of("p1", "foo", "p2", "bar"));

            NavigatorKt.expectView(TestParamsTarget.class);
        }
    }

    private static class TestTarget extends Component {

    }

    @Route("/:p1/:p2")
    private static class TestParamsTarget extends Component implements NavigationTarget {

        @Override
        public void beforeEnter(final BeforeEnterEvent event) {
        }

    }

    private static class TestParamTarget extends Component implements HasUrlParameter<String> {

        @Override
        public void setParameter(final BeforeEvent event, final String parameter) {
        }

    }

}
