package migratedb.core.api.internal.database.base;

import java.util.Map;

public interface Store {
    Map<StateKey, String> get(StateQuery query);

    void put(StateKey key, String value);

    void remove(StateKey key);
}
