// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package bazeltestutil;


import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.model.RunnerBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * A suite implementation that finds all JUnit 4 classes on the current classpath in or below
 * the package of the annotated class, except classes that are annotated with {@code ClasspathSuite}.
 */
public final class AllTestsInClasspathSuite extends Suite {

    /**
     * Only called reflectively. Do not use programmatically.
     */
    public AllTestsInClasspathSuite(Class<?> klass, RunnerBuilder builder) throws Throwable {
        super(builder, klass, getClasses(klass));
    }

    private static Class<?>[] getClasses(Class<?> klass) {
        TreeSet<Class<?>> result = new TreeSet<>(new TestClassNameComparator());
        for (Class<?> clazz : findClasses(klass)) {
            if (isTestClass(clazz)) {
                result.add(clazz);
            }
        }
        return result.toArray(new Class<?>[0]);
    }

    private static boolean isJunit4Test(Class<?> container) {
        if (container.isAnnotationPresent(RunWith.class)) {
            return true;
        } else {
            for (Method method : container.getMethods()) {
                if (isTestMethod(method)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean isTestMethod(Method method) {
        return Modifier.isPublic(method.getModifiers()) &&
                method.getAnnotation(Test.class) != null;
    }

    /**
     * Classes that have a {@code RunWith} annotation for {@link ClasspathSuite} or {@link
     * CustomSuite} are automatically excluded to avoid picking up the suite class itself.
     */
    private static boolean isAnnotatedWithSuite(Class<?> container) {
        RunWith runWith = container.getAnnotation(RunWith.class);
        return runWith != null && isSuiteClass(runWith.value());
    }

    private static boolean isSuiteClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        } else if (clazz == Suite.class) {
            return true;
        } else {
            return isSuiteClass(clazz.getSuperclass());
        }
    }

    /**
     * Determines if a given class is a test class.
     *
     * @param container class to test
     * @return <code>true</code> if the test is a test class.
     */
    private static boolean isTestClass(Class<?> container) {
        return isJunit4Test(container)
                && !isAnnotatedWithSuite(container)
                && Modifier.isPublic(container.getModifiers())
                && !Modifier.isAbstract(container.getModifiers());
    }

    private static class TestClassNameComparator implements Comparator<Class<?>> {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    /** Finds all classes that live in or below the given package. */
    public static TreeSet<Class<?>> findClasses(Class<?> clazz) {
        final String packageName = clazz.getPackage().getName();
        TreeSet<Class<?>> result = new TreeSet<>(new TestClassNameComparator());
        String packagePrefix = (packageName + '.').replace('/', '.');
        try {
            for (ClassInfo ci : ClassPath.from(clazz.getClassLoader()).getAllClasses()) {
                if (ci.getName().startsWith(packagePrefix)) {
                    try {
                        result.add(ci.load());
                    } catch (UnsatisfiedLinkError | NoClassDefFoundError unused) {
                        // Ignore: we're most likely running on a different platform.
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }
}