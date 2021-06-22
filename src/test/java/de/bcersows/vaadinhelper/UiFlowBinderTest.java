package de.bcersows.vaadinhelper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

public class UiFlowBinderTest {

    private Button button;
    private ComboBox<String> comboBox;
    private TextField textField;
    private TextArea textArea;

    @BeforeEach
    protected void setUp() throws Exception {
        this.button = new Button();
        this.comboBox = new ComboBox<String>();
        this.comboBox.setItems("a1", "b2", "c3");
        this.textField = new TextField();
        this.textArea = new TextArea();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        this.button = null;
        this.comboBox = null;
        this.textField = null;
        this.textArea = null;
    }

    @Test
    public void testUiFlowBinder() throws Exception {
        final var binder = new UiFlowBinder(this.button);
        binder.addDependency(this.comboBox);
        final var tfReg = binder.addDependency(this.textField);
        binder.addDependency(this.textArea);

        assertFalse(this.button.isEnabled(), "initially disabled, as invalid");

        this.textArea.setValue("my text");
        assertFalse(this.button.isEnabled(), "not valid yet");

        this.textField.setValue("hello");
        assertFalse(this.button.isEnabled(), "not valid yet");

        this.comboBox.setValue("a1");
        assertTrue(this.button.isEnabled(), "valid now");

        // remove listening for the text field
        tfReg.remove();
        this.textField.clear();
        assertTrue(this.button.isEnabled(), "still valid, despite TF being empty");
    }

    @Test
    public void testUiFlowBinder_mixed() throws Exception {
        this.comboBox.setValue("a1");
        final var binder = new UiFlowBinder(this.button, this.comboBox);

        assertTrue(this.button.isEnabled(), "initially enabled, as valid");

        this.textField.setValue("my value");
        binder.addAdversary(this.textField);

        assertFalse(this.button.isEnabled(), "invalid, as text field has value");

        this.textField.clear();
        assertTrue(this.button.isEnabled(), "all conditions match");
    }
}
