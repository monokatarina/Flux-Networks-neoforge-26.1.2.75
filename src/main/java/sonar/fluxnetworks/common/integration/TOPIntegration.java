package sonar.fluxnetworks.common.integration;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * The One Probe integration entry point.
 *
 * <p>The TOP API is not present on this project's compile classpath. Keeping this
 * class free of direct TOP API references lets the rest of the mod compile until
 * the integration is ported against the matching TOP version.</p>
 */
public class TOPIntegration implements Function<Object, Void> {

    @Override
    public Void apply(@Nonnull Object oneProbe) {
        return null;
    }
}
