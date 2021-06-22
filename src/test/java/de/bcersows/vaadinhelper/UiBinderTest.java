package de.bcersows.vaadinhelper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.textfield.TextField;

import de.bcersows.vaadinhelper.UiBinder.InvalidConfigurationException;
import lombok.AllArgsConstructor;
import lombok.Data;

public class UiBinderTest {

    UiBinder<DataClass, Void> uiBinder;
    private Div parent;
    private DataClass data;

    @BeforeEach
    protected void setUp() throws Exception {
        this.parent = new Div();
        this.uiBinder = UiBinder.with(this.parent, DataClass.class);
        this.data = new DataClass("text", List.of("l1", "l2"), new NestedClass("nested data", "nested text"));
    }

    @AfterEach
    protected void tearDown() throws Exception {
        this.uiBinder = null;
        this.parent = null;
        this.data = null;
    }

    @Nested
    public class GeneralTest {
        @Test
        public void testClear() throws Exception {
            final var label = new Label();

            uiBinder.setText(label, DataClass::getText);
            uiBinder.load(data);

            assertEquals("text", label.getText(), "text was set");

            uiBinder.clear();
            data.text = "update";
            uiBinder.load(data);

            assertEquals("text", label.getText(), "text stayed the same due to clearing");
        }
    }

    @Nested
    public class BindingTest {
        @Test
        public void testFillWith() {
            assertEquals(0, parent.getElement().getChildCount(), "no children yet");

            uiBinder.fillWith(entity -> entity.list.stream().map(Label::new).collect(Collectors.toList()));
            uiBinder.load(data);

            final var structure = VaadinTestHelper.extractRootStructure(parent);
            assertEquals(2, structure.size(), "children added");
            assertEquals(Label.class, structure.get(0).rootClass, "children classes correct");
            assertEquals(Label.class, structure.get(1).rootClass, "children classes correct");

            assertEquals(2, structure.size(), "children added, former removed");
        }

        @Test
        public void testFillWith_component() throws Exception {
            final var wrapper = new Div();
            assertEquals(0, wrapper.getElement().getChildCount(), "no children yet");

            uiBinder.fillWith(wrapper, entity -> entity.list.stream().map(Label::new).collect(Collectors.toList()));
            uiBinder.load(data);

            final var structure = VaadinTestHelper.extractRootStructure(wrapper);
            assertEquals(2, structure.size(), "children added");
            assertEquals(Label.class, structure.get(0).rootClass, "children classes correct");
            assertEquals(Label.class, structure.get(1).rootClass, "children classes correct");

            assertEquals(2, structure.size(), "children added, former removed");
        }

        @Test
        public void testFillWith_extractor() throws Exception {
            final var wrapper = new Div();
            assertEquals(0, wrapper.getElement().getChildCount(), "no children yet");

            uiBinder.fillWith(wrapper, DataClass::getList, list -> list.stream().map(Label::new).collect(Collectors.toList()));
            uiBinder.load(data);

            final var structure = VaadinTestHelper.extractRootStructure(wrapper);
            assertEquals(2, structure.size(), "children added");
            assertEquals(Label.class, structure.get(0).rootClass, "children classes correct");
            assertEquals(Label.class, structure.get(1).rootClass, "children classes correct");

            assertEquals(2, structure.size(), "children added, former removed");
        }

        @Test
        public void testSetText() throws Exception {
            final var label = new Label();
            assertEquals("", label.getText(), "no text yet");

            uiBinder.setText(label, DataClass::getText);
            uiBinder.load(data);

            assertEquals("text", label.getText(), "text was set");

            data.text = "updated";
            uiBinder.load(data);
            assertEquals("updated", label.getText(), "updated text was set");
        }

        @Test
        public void testSet_generic() throws Exception {
            final var field = new TextField();
            assertEquals("", field.getValue(), "no text yet");

            uiBinder.set(field, (entity, comp) -> comp.setValue(entity.text));
            uiBinder.load(data);

            assertEquals("text", field.getValue(), "text was set the generic way");
        }

        @Test
        public void testSet_value() throws Exception {
            final var field = new TextField();
            assertEquals("", field.getValue(), "no text yet");

            uiBinder.set(field, entity -> entity.nested.data);
            uiBinder.load(data);

            assertEquals("nested data", field.getValue(), "text was set (from nested)");
        }

        @Test
        public void testAddClickListener() throws Exception {
            final AtomicReference<String> ar = new AtomicReference<>();
            final var button = new Button("button");

            uiBinder.addClickListener(button, entity -> () -> ar.set("filled"));
            uiBinder.load(data);

            assertEquals(null, ar.get(), "nothing yet");
            button.click();
            assertEquals("filled", ar.get(), "click event triggered");
        }

        @Test
        public void testSetItems_comboBox() {
            final var cb = new ComboBox<String>();

            uiBinder.setItems(cb, DataClass::getList);
            // cannot test more without extracting the items
            assertDoesNotThrow(() -> uiBinder.load(data), "no error");
        }

        @Test
        public void testSetRunnable() {
            final var ds = new AtomicReference<DataClass>();
            uiBinder.setRunnable(ds::set);
            uiBinder.load(data);

            assertEquals(data, ds.get(), "runnable called");
        }
    }

    @Nested
    public class NestingTest {
        @Test
        public void testNesting() {
            // create nested binder
            final var nestedComponent = new Div();
            parent.add(nestedComponent);
            final var nested = uiBinder.createNested(nestedComponent, NestedClass.class, DataClass::getNested);

            // get parent
            assertEquals(uiBinder, nested.getParent(), "parent exists and matches");

            final var label = new Label();
            nested.setText(label, NestedClass::getText);

            uiBinder.load(data);

            // check component filled through nested
            assertEquals("nested text", label.getText(), "nested label filled");
        }

        @Test
        public void testNesting_sameType() {
            // create nested binder
            final var nestedComponent = new Div();
            parent.add(nestedComponent);
            final var nested = uiBinder.createNested(nestedComponent);

            // get parent
            assertEquals(uiBinder, nested.getParent(), "parent exists and matches");

            final var label = new Label();
            nested.setText(label, DataClass::getText);

            uiBinder.load(data);

            // check component filled through nested
            assertEquals("text", label.getText(), "nested label filled");
        }
    }

    @Nested
    public class ErrorHandlingTest {
        @Test
        public void testConfigurationExceptions() {
            // configure binding, try another and nested
            final var div = new Div();
            uiBinder.fillWith(div, data -> List.of(new Label(data.text)));

            /// try another, try nested
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.fillWith(div, data -> List.of(new Label("another binding, shall fail"))),
                    "exception thrown as multiple bindings");
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.createNested(div, NestedClass.class, DataClass::getNested),
                    "exception thrown as multiple bindings");

            uiBinder.clear();

            // configure nested, try binding and nested
            uiBinder.createNested(div, NestedClass.class, DataClass::getNested);

            /// try another, try nested
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.fillWith(div, data -> List.of(new Label("another binding, shall fail"))),
                    "exception thrown as multiple nested");
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.createNested(div, NestedClass.class, DataClass::getNested),
                    "exception thrown as multiple nested");
        }

        @Test
        public void testAfterLoading() {
            final var div = new Div();
            uiBinder.load(data);

            assertThrows(InvalidConfigurationException.class, () -> uiBinder.fillWith(div, data -> List.of(new Label(data.text))), "already loaded, error!");
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.createNested(div, NestedClass.class, DataClass::getNested),
                    "already loaded, error!");
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.setRunnable(data -> {
            }), "already loaded, error!");
        }

        @Test
        public void getParent() {
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.getParent(), "no binder, no parent");
        }

        @Test
        public void testLoadFromParent() {
            assertThrows(InvalidConfigurationException.class, () -> uiBinder.loadFromParent(null), "not nested, no extractor");
        }
    }

    @Data
    @AllArgsConstructor
    public static class DataClass {
        private String text;
        private List<String> list;
        private NestedClass nested;
    }

    @Data
    @AllArgsConstructor
    public static class NestedClass {
        private String data;
        private String text;
    }
}
