package com.commercesuite.notifications.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Minimal {{var}} substitution — deliberately no external template engine. */
@Component
public class TemplateRenderer {
    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*\\}\\}");

    public String render(String template, Map<String, Object> vars) {
        if (template == null) return null;
        Matcher m = VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            Object v = vars == null ? null : vars.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(v == null ? "" : v.toString()));
        }
        m.appendTail(out);
        return out.toString();
    }
}