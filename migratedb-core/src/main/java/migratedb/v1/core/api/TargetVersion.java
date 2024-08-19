/*
 * Copyright 2022-2024 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package migratedb.v1.core.api;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TargetVersion {
    /**
     * Represents {@code MigrateDb.info().latest()}.
     */
    public static final TargetVersion LATEST = new TargetVersion("<< Latest Version >>");
    /**
     * Represents {@code MigrateDb.info().current()}.
     */
    public static final TargetVersion CURRENT = new TargetVersion("<< Current Version >>");
    /**
     * Represents {@code MigrateDb.info().next()}.
     */
    public static final TargetVersion NEXT = new TargetVersion("<< Next Version >>");

    public static TargetVersion of(Version version) {
        return new TargetVersion(version);
    }

    public static TargetVersion parse(String s) {
        if ("current".equalsIgnoreCase(s)) {
            return TargetVersion.CURRENT;
        }
        if ("next".equalsIgnoreCase(s)) {
            return TargetVersion.NEXT;
        }
        if ("latest".equalsIgnoreCase(s)) {
            return TargetVersion.LATEST;
        }
        return new TargetVersion(Version.parse(s));
    }

    private final @Nullable Version version;
    private final String displayText;

    private TargetVersion(Version version) {
        this.displayText = version.toString();
        this.version = version;
    }

    /**
     * Ctor for "symbolic" versions whose actual version number depends on context.
     */
    private TargetVersion(String displayText) {
        this.displayText = displayText;
        this.version = null;
    }

    public final String getDisplayText() {
        return displayText;
    }

    public OrElseDoStep withVersion(Consumer<Version> action) {
        if (version != null) {
            action.accept(version);
        }
        return new OrElseDoStep();
    }

    public <T> OrElseGetStep<T> mapVersion(Function<? super Version, ? extends T> mapper) {
        return new OrElseGetStep<>(mapper);
    }

    @Override
    public String toString() {
        return displayText;
    }

    public class OrElseGetStep<T> {
        private final Function<? super Version, ? extends T> mapper;

        public OrElseGetStep(Function<? super Version, ? extends T> mapper) {
            this.mapper = mapper;
        }

        public Optional<T> orElseGet(Map<TargetVersion, ? extends Supplier<Optional<T>>> symbolMappers) {
            if (version == null) {
                var symbolMapper = symbolMappers.get(TargetVersion.this);
                if (symbolMapper == null) {
                    throw new MigrateDbException("No mapped value defined for case " + this);
                }
                return symbolMapper.get();
            } else {
                return Optional.ofNullable(mapper.apply(version));
            }
        }
    }

    public class OrElseDoStep {
        public void orElseDo(Map<TargetVersion, Runnable> actions) {
            if (version == null) {
                var action = actions.get(TargetVersion.this);
                if (action != null) {
                    action.run();
                }
            }
        }

        public void orElseDoNothing() {
        }
    }
}
