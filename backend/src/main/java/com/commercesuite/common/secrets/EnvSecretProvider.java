package com.commercesuite.common.secrets;

import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvSecretProvider implements SecretProvider {
    private final Environment env;
    public EnvSecretProvider(Environment env) { this.env = env; }
    @Override public Optional<String> get(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = env.getProperty(key);
        return Optional.ofNullable(v).filter(s -> !s.isBlank());
    }
    @Override public String name() { return "env"; }
}
