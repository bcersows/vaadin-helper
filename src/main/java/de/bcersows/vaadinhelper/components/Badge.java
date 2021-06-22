package de.bcersows.vaadinhelper.components;

import org.springframework.lang.NonNull;

import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;

/**
 * Displays a single, tiny bit of information. Should not be mixed with different component types.
 * 
 * @author BCE
 */
@Tag("badge")
@StyleSheet("context://frontend/component-badge.css")
public class Badge extends Div implements HasTheme {
    private static final long serialVersionUID = 1L;

    /** The theme name for a small badge. **/
    private static final String THEME_NAME_SMALL = "small";
    /** The theme name for a small badge. **/
    private static final String THEME_NAME_BRIGHT = "bright";
    /** The theme name for a fixed size badge. **/
    private static final String THEME_NAME_FIXED = "fixed";

    /**
     * Create a chip component with the given text.
     * 
     * @param text
     *            the text to display
     */
    public Badge(@NonNull final String text) {
        // set the text
        this.setText(text);
    }

    /** Make it small. **/
    @NonNull
    public Badge small() {
        this.setThemeName(THEME_NAME_SMALL, true);

        // return self
        return this;
    }

    /** Make it brighter. **/
    @NonNull
    public Badge bright() {
        this.setThemeName(THEME_NAME_BRIGHT, true);

        // return self
        return this;
    }

    /** Make it fixed size. **/
    @NonNull
    public Badge fixed() {
        this.setThemeName(THEME_NAME_FIXED, true);

        // return self
        return this;
    }

    /** Make it normal-sized and non-bright. **/
    @NonNull
    public Badge reset() {
        this.setThemeName(THEME_NAME_SMALL, false);
        this.setThemeName(THEME_NAME_BRIGHT, false);
        this.setThemeName(THEME_NAME_FIXED, false);

        // return self
        return this;
    }
}
