package de.bcersows.vaadinhelper.components;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.lang.NonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;

/**
 * Depicts a slider-themed toggle button/check box. Based on https://www.w3schools.com/howto/howto_css_switch.asp.
 * 
 * @author bcersows
 */
@StyleSheet("context://frontend/component-toggle-button.css")
public class ToggleButton extends CustomField<Boolean> {
    private static final long serialVersionUID = 5093174819562473231L;

    /** Attribute value for a check box type. **/
    private static final String CHECKBOX_ATTRIBUTE_TYPE = "checkbox";
    /** Attribute value indicating a checked check box. **/
    private static final String CHECKBOX_ATTRIBUTE_CHECKED = "checked";
    /** The CSS class for the toggle. **/
    private static final String CSS_CLASS_TOGGLE = "toggle";
    /** The CSS class for the slider. **/
    private static final String CSS_CLASS_SLIDER = "slider";

    /** The selection state. **/
    private final AtomicBoolean state = new AtomicBoolean();

    /** The checkbox. **/
    private final Input checkbox;

    /**
     * Create a new toggle button.
     * 
     * @param labelText
     *            the displayed label text
     */
    public ToggleButton(@NonNull final String labelText) {
        this.setLabel(labelText);

        // create the actual toggle elements
        this.checkbox = new Input();
        this.checkbox.setType(CHECKBOX_ATTRIBUTE_TYPE);

        final Span slider = new Span();
        slider.setClassName(CSS_CLASS_SLIDER);

        final Label toggle = new Label();
        toggle.setClassName(CSS_CLASS_TOGGLE);
        toggle.add(this.checkbox, slider);
        this.add(toggle);

        // add a listener on the slider that will toggle the state
        slider.addClickListener(event -> this.state.set(!this.state.get()));

        // add a listener that is being fired whenever this component was being attached to the UI; update the visual state in that case
        this.addAttachListener(event -> this.setUiState(this.state.get()));
    }

    /** Set the UI state of the check box according to the given value. **/
    protected void setUiState(final boolean currentStateValue) {
        UI.getCurrent().access(() -> this.checkbox.getElement().setAttribute(CHECKBOX_ATTRIBUTE_CHECKED, currentStateValue));
    }

    @Override
    protected Boolean generateModelValue() {
        return this.state.get();
    }

    @Override
    protected void setPresentationValue(final Boolean newPresentationValue) {
        final boolean currentStateValue = BooleanUtils.isTrue(newPresentationValue);
        this.state.set(currentStateValue);
        this.setUiState(currentStateValue);
    }
}
