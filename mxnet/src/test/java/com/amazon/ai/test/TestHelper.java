/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazon.ai.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;

public final class TestHelper {

    private TestHelper() {}

    public static void testGetterSetters(Class<?> baseClass)
            throws IOException, ClassNotFoundException {
        List<Class<?>> list = getClasses(baseClass);
        for (Class<?> clazz : list) {
            System.out.println(clazz.getName());
            Object obj = null;
            if (clazz.isEnum()) {
                obj = clazz.getEnumConstants()[0];
            } else {
                Constructor<?>[] constructors = clazz.getConstructors();
                for (Constructor<?> con : constructors) {
                    try {
                        Class<?>[] types = con.getParameterTypes();
                        Object[] args = new Object[types.length];
                        for (int i = 0; i < args.length; ++i) {
                            args[i] = getMockInstance(types[i]);
                        }
                        con.setAccessible(true);
                        obj = con.newInstance(args);
                    } catch (ReflectiveOperationException ignore) {
                        // ignore
                    }
                }
            }
            if (obj == null) {
                continue;
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                System.out.println(clazz.getName() + "." + methodName);
                int parameterCount = method.getParameterCount();
                try {
                    if (parameterCount == 0
                            && (methodName.startsWith("get")
                                    || methodName.startsWith("is")
                                    || "toString".equals(methodName))) {
                        method.invoke(obj);
                    } else if (parameterCount == 1
                            && (methodName.startsWith("set") || "fromValue".equals(methodName))) {
                        Class<?> type = method.getParameterTypes()[0];
                        method.invoke(obj, getMockInstance(type));
                    }
                } catch (ReflectiveOperationException ignore) {
                    // ignore
                }
            }
        }
    }

    private static List<Class<?>> getClasses(Class<?> clazz)
            throws IOException, ClassNotFoundException {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        String path = url.getPath();

        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return Collections.emptyList();
        }

        List<Class<?>> classList = new ArrayList<>();

        File classPath = new File(path);
        if (classPath.isDirectory()) {
            String rootPath = classPath.getCanonicalPath();
            String[] filters = new String[] {"class"};
            Collection<File> files = FileUtils.listFiles(classPath, filters, true);
            for (File file : files) {
                String fileName = file.getCanonicalPath();
                fileName = fileName.substring(rootPath.length() + 1);
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                fileName = fileName.replace(File.separatorChar, '.');

                try {
                    classList.add(Class.forName(fileName));
                } catch (ExceptionInInitializerError ignore) {
                    // ignore
                }
            }
        } else if (path.toLowerCase().endsWith(".jar")) {
            try (JarFile jarFile = new JarFile(path)) {
                Enumeration<JarEntry> en = jarFile.entries();
                while (en.hasMoreElements()) {
                    JarEntry entry = en.nextElement();
                    String fileName = entry.getName();
                    if (fileName.endsWith(".class")) {
                        fileName = fileName.substring(0, fileName.lastIndexOf("."));
                        fileName = fileName.replace('/', '.');
                        try {
                            classList.add(Class.forName(fileName));
                        } catch (ExceptionInInitializerError ignore) {
                            // ignore
                        }
                    }
                }
            }
        }

        return classList;
    }

    private static Object getMockInstance(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == Boolean.TYPE) {
                return Boolean.TRUE;
            }
            if (clazz == Character.TYPE) {
                return '0';
            }
            if (clazz == Byte.TYPE) {
                return (byte) 0;
            }
            if (clazz == Short.TYPE) {
                return (short) 0;
            }
            if (clazz == Integer.TYPE) {
                return 0;
            }
            if (clazz == Long.TYPE) {
                return 0L;
            }
            if (clazz == Float.TYPE) {
                return 0f;
            }
            if (clazz == Double.TYPE) {
                return 0d;
            }
        }

        if (clazz.isAssignableFrom(String.class)) {
            return "";
        }

        if (clazz.isAssignableFrom(List.class)) {
            return Collections.emptyList();
        }

        if (clazz.isAssignableFrom(Set.class)) {
            return Collections.emptySet();
        }

        if (clazz.isAssignableFrom(Map.class)) {
            return Collections.emptyMap();
        }

        if (clazz.isEnum()) {
            return clazz.getEnumConstants()[0];
        }

        return null;
    }
}
