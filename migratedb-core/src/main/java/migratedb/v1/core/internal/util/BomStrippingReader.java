/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.internal.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reader that strips the BOM from every position in a stream.
 */
public class BomStrippingReader extends FilterReader {
    private static final int EMPTY_STREAM = -1;

    /**
     * Creates a new BOM-stripping reader.
     *
     * @param in a Reader object providing the underlying stream.
     *
     * @throws NullPointerException if {@code in} is {@code null}
     */
    public BomStrippingReader(Reader in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (c != EMPTY_STREAM && BomFilter.isBom((char) c)) {
            // Skip BOM, even if it is not at the start of the stream. (TODO: Uh, is that intended?)
            return super.read();
        }
        return c;
    }
}
