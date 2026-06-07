package com.commercesuite.common.secrets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SecretProviderConfig {
    @Bean @Primary
    public SecretProvider activeSecretProvider(
            EnvSecretProvider env,
            @Value("${app.secrets.provider:env}") String provider) {
        return switch (provider.toLowerCase()) {
            case "aws"    -> new AwsSecretsManagerProvider(env);
            case "vault"  -> new VaultSecretProvider(env);
            case "azure"  -> new AzureKeyVaultProvider(env);
            case "gcp"    -> new GcpSecretManagerProvider(env);
            default       -> env;
        };
    }
}
