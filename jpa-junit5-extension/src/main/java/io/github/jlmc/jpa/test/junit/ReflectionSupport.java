package io.github.jlmc.jpa.test.junit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ReflectionSupport {

    static Optional<Object> invokeMethod(Object delegate, String methodName) {
        return getMethodWithNoArgs(delegate.getClass(), methodName)
                .map(method -> invoke(delegate, method));
    }

    private static Object invoke(Object delegate, Method method) throws RuntimeException {
        try {
            return method.invoke(delegate);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static Optional<Method> getMethodWithNoArgs(Class<?> type, String methodName) {
        return getAllMethodsInHierarchy(type)
                .stream()
                .filter(method -> methodName.equals(method.getName()) && method.getParameterTypes().length == 0)
                .findFirst();
    }

    static Set<Method> getAllMethodsInHierarchy(Class<?> objectClass) {
        Set<Method> allMethods = new HashSet<>();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        Method[] methods = objectClass.getMethods();
        if (objectClass.getSuperclass() != null) {
            Class<?> superClass = objectClass.getSuperclass();
            Set<Method> superClassMethods = getAllMethodsInHierarchy(superClass);
            allMethods.addAll(superClassMethods);
        }
        allMethods.addAll(Arrays.asList(declaredMethods));
        allMethods.addAll(Arrays.asList(methods));
        return allMethods;
    }
}
