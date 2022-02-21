/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022 The MigrateDB contributors
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
package migratedb.core.internal.resource.android;

import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import migratedb.core.api.MigrateDbException;
import migratedb.core.api.resource.Resource;

/**
 * Resource within an Android App.
 */
public class AndroidResource implements Resource {
    private final AssetManager assetManager;
    private final String fileName;

    public AndroidResource(AssetManager assetManager, String path, String name) {
        this.assetManager = assetManager;
        this.fileName = path + "/" + name;
    }

    @Override
    public Reader read(Charset charset) {
        try {
            return new InputStreamReader(assetManager.open(fileName), charset);
        } catch (IOException e) {
            throw new MigrateDbException("Unable to read asset: " + fileName, e);
        }
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String describeLocation() {
        return "android: " + fileName;
    }

    @Override
    public String toString() {
        return describeLocation();
    }
}
