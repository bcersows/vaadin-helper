package de.bcersows.vaadinhelper.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BadgeTest {

    private Badge badge;

    @BeforeEach
    protected void setUp() throws Exception {
        this.badge = new Badge("test");
    }

    @AfterEach
    protected void tearDown() throws Exception {
        this.badge = null;
    }

    @Nested
    public class HtmlTest {
        @Test
        public void testHtml() {
            assertEquals("test", badge.getText(), "text matches");

            badge.setText("new");
            assertEquals("new", badge.getText(), "text was updated");
        }
    }

    @Nested
    public class FunctionalityTest {
        @Test
        public void testSize() {
            badge.small();
            assertTrue(badge.hasThemeName("small"));

            badge.bright();
            assertTrue(badge.hasThemeName("bright"));

            badge.fixed();
            assertTrue(badge.hasThemeName("fixed"));

            badge.reset();
            assertFalse(badge.hasThemeName("small"));
            assertFalse(badge.hasThemeName("bright"));
            assertFalse(badge.hasThemeName("fixed"));
        }
    }
}
