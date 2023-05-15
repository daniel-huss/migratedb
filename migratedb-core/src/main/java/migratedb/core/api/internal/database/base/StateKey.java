package migratedb.core.api.internal.database.base;

import java.util.Objects;
import java.util.regex.Pattern;

public final class StateKey {
    private static final Pattern VALID_VALUE_PATTERN = Pattern.compile("[a-z][a-z0-9/.:_-]");
    private final String value;

    public StateKey(String value) {
        if (!VALID_VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Key must match " + VALID_VALUE_PATTERN.pattern());
        }
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StateKey)) {
            return false;
        }
        var other = (StateKey) obj;
        return Objects.equals(value, other.value);
    }
}
