package de.bcersows.vaadinhelper.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.mvysny.kaributesting.v10.MockVaadin;

public class ToggleButtonTest {
    private ToggleButton toggleButton;

    @BeforeEach
    protected void setUp() {
        MockVaadin.setup();
        toggleButton = new ToggleButton("Mock Button");
    }

    @Test
    public void setUiStateTest() {
        toggleButton.setPresentationValue(true);
        assertTrue(toggleButton.generateModelValue());

        toggleButton.setPresentationValue(false);
        assertFalse(toggleButton.generateModelValue());
    }
}
