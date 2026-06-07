package com.commercesuite.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.notifications.service.TemplateRenderer;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRenderingTest {
    private final TemplateRenderer r = new TemplateRenderer();

    @Test void substitutes_variables_and_leaves_unknown_empty() {
        assertThat(r.render("Hi {{name}}, your order {{orderNumber}}!",
                Map.of("name", "Rahul", "orderNumber", "ORD-1")))
                .isEqualTo("Hi Rahul, your order ORD-1!");
        assertThat(r.render("Empty {{missing}}!", Map.of())).isEqualTo("Empty !");
        assertThat(r.render(null, Map.of())).isNull();
    }

    @Test void handles_dollar_and_backslash_safely() {
        assertThat(r.render("Cost {{amount}}", Map.of("amount", "$5\\note")))
                .isEqualTo("Cost $5\\note");
    }
}