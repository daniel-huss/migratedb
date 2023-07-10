package migratedb.core.api.migration;

import migratedb.core.api.Checksum;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.configuration.FluentConfiguration;
import migratedb.core.internal.parser.PlaceholderReplacingReader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.function.Consumer;

/**
 * An easy way to define SQL migration scripts as code. This can eliminate the need for build-time class path scanning
 * when used with {@link FluentConfiguration#javaMigrations(JavaMigration...)}. Example:
 * <pre>
 *    {@literal @}Component
 *     {@code
 *      public class V009__Add_Cool_Stuff extends ScriptMigration {
 *          protected Object script() {
 *              return """
 *                 -- Add cool stuff
 *                 create table cool_stuff(id int primary key);
 *                 insert into cool_stuff(id) values (${cool_id});
 *                 drop table old_stuff;
 *              """;
 *          }
 *      }
 * }}</pre>
 *
 * <p>Computes a checksum from the script contents by default, just like MigrateDB does for migration scripts. You can
 * turn this off by overriding {@link #getChecksum(Configuration)}.</p>
 */
public abstract class ScriptMigration extends BaseJavaMigration {
    /**
     * Provides the script to execute. If this returns a {@code Reader}, its lines are executed and the reader is
     * properly closed. Everything else is converted to a String via {@link Object#toString()}.
     */
    protected abstract Object script();

    @Override
    public Checksum getChecksum(Configuration configuration) {
        var checksum = Checksum.builder();
        withScript(configuration, checksum::addLines);
        return checksum.build();
    }

    @Override
    public final void migrate(Context context) throws Exception {
        withScript(context.getConfiguration(), context::runScript);
    }

    private void withScript(Configuration configuration, Consumer<Reader> consumer) {
        var script = script();
        try (var reader = script instanceof Reader ? (Reader) script : new StringReader(script.toString());
             var placeholderReplacingReader = new PlaceholderReplacingReader(configuration.getPlaceholderPrefix(),
                     configuration.getPlaceholderSuffix(),
                     configuration.getPlaceholders(),
                     reader)) {
            consumer.accept(placeholderReplacingReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
