package elya;

import elya.constants.exceptions.ApiEmulatorConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static elya.constants.exceptions.ExceptionMessage.*;

/**
 * Token settings bound from {@code api.credentials.token.*}.
 * <p>Single source of truth for the token lifetime and issuer. Immutable: values are bound
 * once at startup through the canonical constructor and validated there (fail-fast).</p>
 *
 * @param expiration token lifetime. A bare number is read as seconds ({@code 3600});
 *                   a unit suffix is also accepted ({@code 1h}, {@code 30m}). Defaults to 1 hour.
 * @param issuer     value returned in the {@code issuer} field of the token response.
 *                   Defaults to {@code api-emulator}.
 */
@ConfigurationProperties(prefix = "api.credentials.token")
public record ApiTokenProperties(
        @DefaultValue("1h") @DurationUnit(ChronoUnit.SECONDS) Duration expiration,
        @DefaultValue("api-emulator") String issuer) {

    /**
     * Rejects a misconfiguration at startup instead of issuing tokens that expire instantly.
     *
     * @throws ApiEmulatorConfigurationException if the lifetime is not positive or the issuer is blank.
     */
    public ApiTokenProperties {
        if (expiration == null || !expiration.isPositive()) {
            throw new ApiEmulatorConfigurationException(TOKEN_EXPIRATION_MUST_BE_POSITIVE.formatted(expiration));
        }
        if (issuer == null || issuer.isBlank()) {
            throw new ApiEmulatorConfigurationException(TOKEN_ISSUER_MUST_NOT_BE_BLANK);
        }
    }
}