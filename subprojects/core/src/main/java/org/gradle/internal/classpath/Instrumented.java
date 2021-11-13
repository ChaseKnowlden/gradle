/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.classpath;

import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.codehaus.groovy.runtime.callsite.AbstractCallSite;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteArray;
import org.codehaus.groovy.runtime.wrappers.Wrapper;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Instrumented {
    private static final Listener NO_OP = new Listener() {
        @Override
        public void systemPropertyQueried(String key, @Nullable Object value, String consumer) {
        }

        @Override
        public void envVariableQueried(String key, @Nullable String value, String consumer) {
        }

        @Override
        public void externalProcessStarted(String command, String consumer) {
        }
    };

    private static final AtomicReference<Listener> LISTENER = new AtomicReference<>(NO_OP);

    public static void setListener(Listener listener) {
        LISTENER.set(listener);
    }

    public static void discardListener() {
        LISTENER.set(NO_OP);
    }

    // Called by generated code
    @SuppressWarnings("unused")
    public static void groovyCallSites(CallSiteArray array) {
        for (CallSite callSite : array.array) {
            switch (callSite.getName()) {
                case "getProperty":
                    array.array[callSite.getIndex()] = new SystemPropertyCallSite(callSite);
                    break;
                case "properties":
                    array.array[callSite.getIndex()] = new SystemPropertiesCallSite(callSite);
                    break;
                case "getInteger":
                    array.array[callSite.getIndex()] = new IntegerSystemPropertyCallSite(callSite);
                    break;
                case "getLong":
                    array.array[callSite.getIndex()] = new LongSystemPropertyCallSite(callSite);
                    break;
                case "getBoolean":
                    array.array[callSite.getIndex()] = new BooleanSystemPropertyCallSite(callSite);
                    break;
                case "getenv":
                    array.array[callSite.getIndex()] = new GetEnvCallSite(callSite);
                    break;
                case "exec":
                    array.array[callSite.getIndex()] = new ExecCallSite(callSite);
                    break;
                case "execute":
                    array.array[callSite.getIndex()] = new ExecuteCallSite(callSite);
                    break;
                case "start":
                case "startPipeline":
                    array.array[callSite.getIndex()] = new ProcessBuilderStartCallSite(callSite);
                    break;
            }
        }
    }

    // Called by generated code.
    public static String systemProperty(String key, String consumer) {
        return systemProperty(key, null, consumer);
    }

    // Called by generated code.
    public static String systemProperty(String key, @Nullable String defaultValue, String consumer) {
        String value = System.getProperty(key);
        LISTENER.get().systemPropertyQueried(key, value, consumer);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    // Called by generated code.
    public static Properties systemProperties(String consumer) {
        return new AccessTrackingProperties(System.getProperties(), (k, v) -> {
            LISTENER.get().systemPropertyQueried(convertToString(k), convertToString(v), consumer);
        });
    }

    // Called by generated code.
    public static Integer getInteger(String key, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Integer.getInteger(key);
    }

    // Called by generated code.
    public static Integer getInteger(String key, int defaultValue, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Integer.getInteger(key, defaultValue);
    }

    // Called by generated code.
    public static Integer getInteger(String key, Integer defaultValue, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Integer.getInteger(key, defaultValue);
    }

    // Called by generated code.
    public static Long getLong(String key, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Long.getLong(key);
    }

    // Called by generated code.
    public static Long getLong(String key, long defaultValue, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Long.getLong(key, defaultValue);
    }

    // Called by generated code.
    public static Long getLong(String key, Long defaultValue, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Long.getLong(key, defaultValue);
    }

    // Called by generated code.
    public static boolean getBoolean(String key, String consumer) {
        LISTENER.get().systemPropertyQueried(key, System.getProperty(key), consumer);
        return Boolean.getBoolean(key);
    }

    // Called by generated code.
    public static String getenv(String key, String consumer) {
        String value = System.getenv(key);
        LISTENER.get().envVariableQueried(key, value, consumer);
        return value;
    }

    // Called by generated code.
    public static Map<String, String> getenv(String consumer) {
        return new AccessTrackingEnvMap((key, value) -> LISTENER.get().envVariableQueried(convertToString(key), value, consumer));
    }

    public static Process exec(Runtime runtime, String command, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(command, consumer);
        return runtime.exec(command);
    }

    public static Process exec(Runtime runtime, String[] command, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return runtime.exec(command);
    }

    public static Process exec(Runtime runtime, String command, String[] envp, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(command, consumer);
        return runtime.exec(command, envp);
    }

    public static Process exec(Runtime runtime, String[] command, String[] envp, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return runtime.exec(command, envp);
    }

    public static Process exec(Runtime runtime, String command, String[] envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(command, consumer);
        return runtime.exec(command, envp, dir);
    }

    public static Process exec(Runtime runtime, String[] command, String[] envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return runtime.exec(command, envp, dir);
    }

    public static Process execute(String command, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(command, consumer);
        return ProcessGroovyMethods.execute(command);
    }

    public static Process execute(String[] command, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return ProcessGroovyMethods.execute(command);
    }

    public static Process execute(List<?> command, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return ProcessGroovyMethods.execute(command);
    }

    public static Process execute(String command, String[] envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(command, consumer);
        return ProcessGroovyMethods.execute(command, envp, dir);
    }

    public static Process execute(String command, List<?> envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(command, consumer);
        return ProcessGroovyMethods.execute(command, envp, dir);
    }

    public static Process execute(String[] command, String[] envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return ProcessGroovyMethods.execute(command, envp, dir);
    }

    public static Process execute(String[] command, List<?> envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return ProcessGroovyMethods.execute(command, envp, dir);
    }

    public static Process execute(List<?> command, String[] envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return ProcessGroovyMethods.execute(command, envp, dir);
    }

    public static Process execute(List<?> command, List<?> envp, File dir, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(command), consumer);
        return ProcessGroovyMethods.execute(command, envp, dir);
    }

    public static Process start(ProcessBuilder builder, String consumer) throws IOException {
        LISTENER.get().externalProcessStarted(joinCommand(builder.command()), consumer);
        return builder.start();
    }

    @SuppressWarnings("unchecked")
    public static List<Process> startPipeline(List<ProcessBuilder> pipeline, String consumer) throws IOException {
        try {
            for (ProcessBuilder builder : pipeline) {
                LISTENER.get().externalProcessStarted(joinCommand(builder.command()), consumer);
            }
            Object result = ProcessBuilder.class.getMethod("startPipeline", List.class).invoke(null, pipeline);
            return (List<Process>) result;
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new NoSuchMethodError("Cannot find method ProcessBuilder.startPipeline");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException("Unexpected exception thrown by ProcessBuilder.startPipeline", e);
            }
        }
    }

    private static Object unwrap(Object obj) {
        if (obj instanceof Wrapper) {
            return ((Wrapper) obj).unwrap();
        }
        return obj;
    }

    private static String convertToString(Object arg) {
        if (arg instanceof CharSequence) {
            return ((CharSequence) arg).toString();
        }
        return (String) arg;
    }

    private static String joinCommand(String[] command) {
        return String.join(" ", command);
    }

    private static String joinCommand(List<?> command) {
        return command.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    public interface Listener {
        /**
         * @param consumer The name of the class that is reading the property value
         */
        void systemPropertyQueried(String key, @Nullable Object value, String consumer);

        /**
         * Invoked when the code reads the environment variable.
         *
         * @param key the name of the variable
         * @param value the value of the variable
         * @param consumer the name of the class that is reading the variable
         */
        void envVariableQueried(String key, @Nullable String value, String consumer);

        void externalProcessStarted(String command, String consumer);
    }

    private static class IntegerSystemPropertyCallSite extends AbstractCallSite {
        public IntegerSystemPropertyCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object call(Object receiver, Object arg) throws Throwable {
            if (receiver.equals(Integer.class)) {
                return getInteger(arg.toString(), array.owner.getName());
            } else {
                return super.call(receiver, arg);
            }
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2) throws Throwable {
            if (receiver.equals(Integer.class)) {
                return getInteger(arg1.toString(), (Integer) unwrap(arg2), array.owner.getName());
            } else {
                return super.call(receiver, arg1, arg2);
            }
        }
    }

    private static class LongSystemPropertyCallSite extends AbstractCallSite {
        public LongSystemPropertyCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object call(Object receiver, Object arg) throws Throwable {
            if (receiver.equals(Long.class)) {
                return getLong(arg.toString(), array.owner.getName());
            } else {
                return super.call(receiver, arg);
            }
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2) throws Throwable {
            if (receiver.equals(Long.class)) {
                return getLong(arg1.toString(), (Long) unwrap(arg2), array.owner.getName());
            } else {
                return super.call(receiver, arg1, arg2);
            }
        }
    }

    private static class BooleanSystemPropertyCallSite extends AbstractCallSite {
        public BooleanSystemPropertyCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object call(Object receiver, Object arg) throws Throwable {
            if (receiver.equals(Boolean.class)) {
                return getBoolean(arg.toString(), array.owner.getName());
            } else {
                return super.call(receiver, arg);
            }
        }
    }

    private static class SystemPropertyCallSite extends AbstractCallSite {
        public SystemPropertyCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object call(Object receiver, Object arg) throws Throwable {
            if (receiver.equals(System.class)) {
                return systemProperty(arg.toString(), array.owner.getName());
            } else {
                return super.call(receiver, arg);
            }
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2) throws Throwable {
            if (receiver.equals(System.class)) {
                return systemProperty(arg1.toString(), convertToString(arg2), array.owner.getName());
            } else {
                return super.call(receiver, arg1, arg2);
            }
        }
    }

    private static class SystemPropertiesCallSite extends AbstractCallSite {
        public SystemPropertiesCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object callGetProperty(Object receiver) throws Throwable {
            if (receiver.equals(System.class)) {
                return systemProperties(array.owner.getName());
            } else {
                return super.callGetProperty(receiver);
            }
        }
    }

    private static class GetEnvCallSite extends AbstractCallSite {
        public GetEnvCallSite(CallSite prev) {
            super(prev);
        }

        @Override
        public Object call(Object receiver) throws Throwable {
            if (receiver.equals(System.class)) {
                return getenv(array.owner.getName());
            }
            return super.call(receiver);
        }

        @Override
        public Object call(Object receiver, Object arg1) throws Throwable {
            if (receiver.equals(System.class) && arg1 instanceof CharSequence) {
                return getenv(convertToString(arg1), array.owner.getName());
            }
            return super.call(receiver, arg1);
        }
    }

    /**
     * The call site for {@code Runtime.exec}.
     */
    private static class ExecCallSite extends AbstractCallSite {
        public ExecCallSite(CallSite prev) {
            super(prev);
        }

        @Override
        public Object call(Object receiver, Object arg1) throws Throwable {
            if (receiver instanceof Runtime) {
                if (arg1 instanceof CharSequence) {
                    return exec((Runtime) receiver, convertToString(arg1), array.owner.getName());
                } else if (arg1 instanceof String[]) {
                    return exec((Runtime) receiver, (String[]) arg1, array.owner.getName());
                }
            }
            return super.call(receiver, arg1);
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2) throws Throwable {
            if (receiver instanceof Runtime && isCommand(arg1) && arg2 instanceof String[]) {
                if (arg1 instanceof CharSequence) {
                    return exec((Runtime) receiver, convertToString(arg1), (String[]) arg2, array.owner.getName());
                } else if (arg1 instanceof String[]) {
                    return exec((Runtime) receiver, (String[]) arg1, (String[]) arg2, array.owner.getName());
                }
            }
            return super.call(receiver, arg1, arg2);
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2, Object arg3) throws Throwable {
            if (receiver instanceof Runtime && isCommand(arg1) && arg2 instanceof String[] && arg3 instanceof File) {
                if (arg1 instanceof CharSequence) {
                    return exec((Runtime) receiver, convertToString(arg1), (String[]) arg2, array.owner.getName());
                } else if (arg1 instanceof String[]) {
                    return exec((Runtime) receiver, (String[]) arg1, (String[]) arg2, array.owner.getName());
                }
            }
            return super.call(receiver, arg1, arg2, arg3);
        }

        private boolean isCommand(Object arg) {
            return arg instanceof CharSequence || arg instanceof String[];
        }
    }

    /**
     * The call site for Groovy's {@code String.execute}, {@code String[].execute}, and {@code List.execute}. This also handles {@code ProcessGroovyMethods.execute}.
     */
    private static class ExecuteCallSite extends AbstractCallSite {
        public ExecuteCallSite(CallSite prev) {
            super(prev);
        }

        @Override
        public Object call(Object receiver) throws Throwable {
            if (isCommand(receiver)) {
                return callInternal(receiver);
            }
            return super.call(receiver);
        }

        @Override
        public Object callStatic(Class receiver, Object arg1) throws Throwable {
            if (receiver.equals(ProcessGroovyMethods.class) && isCommand(arg1)) {
                return callInternal(arg1);
            }
            return super.callStatic(receiver, arg1);
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2) throws Throwable {
            if (isCommand(receiver) && isEnvp(arg1) && arg2 instanceof File) {
                return callInternal(receiver, arg1, (File) arg2);
            }
            return super.call(receiver);
        }

        @Override
        public Object callStatic(Class receiver, Object arg1, Object arg2, Object arg3) throws Throwable {
            if (receiver.equals(ProcessGroovyMethods.class) && isCommand(arg1) && isEnvp(arg2) && arg3 instanceof File) {
                return callInternal(arg1, arg2, (File) arg3);
            }
            return super.callStatic(receiver, arg1, arg2, arg3);
        }

        private Object callInternal(Object command) throws Exception {
            if (command instanceof CharSequence) {
                return execute(convertToString(command), array.owner.getName());
            } else if (command instanceof String[]) {
                return execute((String[]) command, array.owner.getName());
            } else if (command instanceof List) {
                return execute((List<?>) command, array.owner.getName());
            }
            throw new IllegalArgumentException("Invalid command type " + command.getClass());
        }

        private Object callInternal(Object command, Object envp, File dir) throws Exception {
            if (envp instanceof String[]) {
                return callInternal(command, (String[]) envp, dir);
            }
            if (envp instanceof List) {
                return callInternal(command, (List<?>) envp, dir);
            }
            throw new IllegalArgumentException("Invalid envp type " + envp.getClass());
        }

        private Object callInternal(Object command, String[] envp, File dir) throws Exception {
            if (command instanceof CharSequence) {
                return execute(convertToString(command), envp, dir, array.owner.getName());
            } else if (command instanceof String[]) {
                return execute((String[]) command, envp, dir, array.owner.getName());
            } else if (command instanceof List) {
                return execute((List<?>) command, envp, dir, array.owner.getName());
            }
            throw new IllegalArgumentException("Invalid argument type " + command.getClass());
        }

        private Object callInternal(Object command, List<?> envp, File dir) throws Exception {
            if (command instanceof CharSequence) {
                return execute(convertToString(command), envp, dir, array.owner.getName());
            } else if (command instanceof String[]) {
                return execute((String[]) command, envp, dir, array.owner.getName());
            } else if (command instanceof List) {
                return execute((List<?>) command, envp, dir, array.owner.getName());
            }
            throw new IllegalArgumentException("Invalid argument type " + command.getClass());
        }

        private boolean isCommand(Object arg) {
            return arg instanceof CharSequence || arg instanceof String[] || arg instanceof List;
        }

        private boolean isEnvp(Object arg) {
            return arg instanceof String[] || arg instanceof List;
        }
    }

    /**
     * The call site for {@code ProcessBuilder.start} and {@code ProcessBuilder.startPipeline}.
     */
    private static class ProcessBuilderStartCallSite extends AbstractCallSite {
        public ProcessBuilderStartCallSite(CallSite prev) {
            super(prev);
        }

        @Override
        public Object call(Object receiver) throws Throwable {
            if (receiver instanceof ProcessBuilder) {
                // ProcessBuilder.start()
                return start((ProcessBuilder) receiver, array.owner.getName());
            }
            return super.call(receiver);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object callStatic(Class receiver, Object arg1) throws Throwable {
            if (receiver.equals(ProcessBuilder.class) && arg1 instanceof List) {
                // ProcessBuilder.startPipeline(List<ProcessBuilder> pbs)
                return startPipeline((List<ProcessBuilder>) arg1, array.owner.getName());
            }
            return super.callStatic(receiver, arg1);
        }
    }
}
