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
package migratedb.core.internal.util;

import java.lang.reflect.Field;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import migratedb.core.api.MigrateDbException;

public class ClassUtils {
    private ClassUtils() {
    }

    /**
     * Creates a new instance of this class.
     *
     * @param className   The fully qualified name of the class to instantiate.
     * @param classLoader The ClassLoader to use.
     * @param <T>         The type of the new instance.
     * @return The new instance.
     * @throws MigrateDbException Thrown when the instantiation failed.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T instantiate(String className, ClassLoader classLoader) {
        try {
            return (T) Class.forName(className, true, classLoader).getConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MigrateDbException("Unable to instantiate class " + className + " : " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new instance of {@code clazz}.
     *
     * @param <T> The type of the new instance.
     * @return The new instance.
     * @throws MigrateDbException Thrown when the instantiation failed.
     */
    public static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.cast(clazz.getConstructor().newInstance());
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MigrateDbException("Unable to instantiate " + clazz + " : " + e.getMessage(), e);
        }
    }


    /**
     * Instantiate all these classes.
     *
     * @param classes     Fully qualified class names to instantiate.
     * @param classLoader The ClassLoader to use.
     * @param <T>         The common type for all classes.
     * @return The list of instances.
     */
    public static <T> List<T> instantiateAll(String[] classes, ClassLoader classLoader) {
        List<T> clazzes = new ArrayList<>();
        for (String clazz : classes) {
            if (StringUtils.hasLength(clazz)) {
                clazzes.add(ClassUtils.instantiate(clazz, classLoader));
            }
        }
        return clazzes;
    }

    /**
     * Determine whether the {@link Class} identified by the supplied name is present and can be loaded. Will return
     * {@code false} if either the class or one of its dependencies is not present or cannot be loaded.
     *
     * @param className   The name of the class to check.
     * @param classLoader The ClassLoader to use.
     * @return whether the specified class is present
     */
    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            classLoader.loadClass(className);
            return true;
        } catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }

    /**
     * Determine whether a class implementing the service identified by the supplied name is present and can be loaded.
     * Will return {@code false} if either no class is found, or the class or one of its dependencies is not present or
     * cannot be loaded.
     *
     * @param serviceName The name of the service to check.
     * @param classLoader The ClassLoader to use.
     * @return whether an implementation of the specified service is present
     */
    public static boolean isImplementationPresent(String serviceName, ClassLoader classLoader) {
        try {
            Class<?> service = classLoader.loadClass(serviceName);
            return ServiceLoader.load(service).iterator().hasNext();
        } catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }

    /**
     * Loads {@code className} using the class loader.
     *
     * @param className   The name of the class to load.
     * @param classLoader The ClassLoader to use.
     * @return the newly loaded class
     */
    public static Class<?> loadClass(String className,
                                     ClassLoader classLoader) {
        try {
            return classLoader.loadClass(className);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MigrateDbException("Cannot load class " + className);
        }
    }

    /**
     * Tries to get the physical location on disk of {@code aClass}.
     *
     * @param aClass The class to get the location for.
     * @return The absolute path of the .class file.
     */
    public static String guessLocationOnDisk(Class<?> aClass) {
        ProtectionDomain protectionDomain = aClass.getProtectionDomain();
        if (protectionDomain == null) {
            //Android
            return null;
        }
        CodeSource codeSource = protectionDomain.getCodeSource();

        if (codeSource == null || codeSource.getLocation() == null) {
            //Custom classloader with for example classes defined using URLClassLoader#defineClass(String name,
            // byte[] b, int off, int len)
            return null;
        }
        return UrlUtils.decodeURL(codeSource.getLocation().getPath());
    }

    /**
     * Gets the String value of a static field.
     *
     * @param className   The fully qualified name of the class to instantiate.
     * @param classLoader The ClassLoader to use.
     * @param fieldName   The field name
     * @return The value of the field.
     * @throws MigrateDbException Thrown when the instantiation failed.
     */
    public static String getStaticFieldValue(String className, String fieldName, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(className, true, classLoader);
            Field field = clazz.getField(fieldName);
            return (String) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MigrateDbException(
                    "Unable to obtain field value " + className + "." + fieldName + " : " + e.getMessage(), e);
        }
    }

    /**
     * @return The first class loader that is non-null:
     * <ol>
     *     <li>Thread context class loader</li>
     *     <li>Class loader of this class</li>
     *     <li>System class loader</li>
     * </ol>
     */
    public static ClassLoader defaultClassLoader() {
        var result = Thread.currentThread().getContextClassLoader();
        if (result == null) {
            result = ClassUtils.class.getClassLoader();
        }
        if (result == null) {
            result = ClassLoader.getSystemClassLoader();
        }
        assert result != null;
        return result;
    }
}