import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Emits the normalized P2-C2 Public API baseline candidate from packaged JARs. */
final class FullPublicApiInventory {

    private static final String NULL_MARKED = "org.jspecify.annotations.NullMarked";

    private FullPublicApiInventory() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Usage: FullPublicApiInventory.java <artifact-id> <jar> [...]");
        }
        System.out.println("# KOIKI Phase 2 P2-C2 Public API baseline candidate v1");
        for (int index = 0; index < args.length; index += 2) {
            emitArtifact(args[index], Path.of(args[index + 1]));
        }
    }

    private static void emitArtifact(String artifactId, Path jarPath) throws Exception {
        List<Class<?>> types = publicTypes(jarPath);
        System.out.println("ARTIFACT " + artifactId);
        System.out.println("PUBLIC_TYPES " + types.size());
        Set<String> packages = new TreeSet<>();
        for (Class<?> type : types) {
            packages.add(type.getPackageName());
        }
        for (String packageName : packages) {
            Package apiPackage = Class.forName(packageName + ".package-info", false,
                            FullPublicApiInventory.class.getClassLoader())
                    .getPackage();
            System.out.println("PACKAGE " + packageName + " NULL_MARKED "
                    + hasAnnotation(apiPackage.getAnnotations(), NULL_MARKED));
        }
        for (Class<?> type : types) {
            emitType(type);
        }
    }

    private static List<Class<?>> publicTypes(Path jarPath)
            throws IOException, ClassNotFoundException {
        List<Class<?>> types = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            for (JarEntry entry : jarFile.stream()
                    .filter(candidate -> !candidate.isDirectory())
                    .filter(candidate -> candidate.getName().endsWith(".class"))
                    .filter(candidate -> !candidate.getName().equals("module-info.class"))
                    .filter(candidate -> !candidate.getName().endsWith("package-info.class"))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList()) {
                String className = entry.getName()
                        .substring(0, entry.getName().length() - ".class".length())
                        .replace('/', '.');
                if (isInternalPackage(className)) {
                    continue;
                }
                Class<?> type = Class.forName(
                        className, false, FullPublicApiInventory.class.getClassLoader());
                if (Modifier.isPublic(type.getModifiers())) {
                    types.add(type);
                }
            }
        }
        types.sort(Comparator.comparing(Class::getName));
        return types;
    }

    private static boolean isInternalPackage(String className) {
        return className.contains(".internal.") || className.endsWith(".internal");
    }

    private static void emitType(Class<?> type) {
        String kind;
        if (type.isAnnotation()) {
            kind = "annotation";
        } else if (type.isEnum()) {
            kind = "enum";
        } else if (type.isRecord()) {
            kind = "record";
        } else if (type.isInterface()) {
            kind = "interface";
        } else {
            kind = "class";
        }
        System.out.println("TYPE " + typeModifiers(type) + kind + " " + type.getName());
        if (type.isAnnotation()) {
            emitAnnotationMetadata(type);
        }
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) {
                System.out.println("ENUM " + type.getName() + " " + constant);
            }
        }
        if (type.getGenericSuperclass() != null && type.getSuperclass() != Object.class
                && !type.isEnum() && !type.isRecord()) {
            System.out.println("EXTENDS " + type.getName() + " "
                    + type.getGenericSuperclass().getTypeName());
        }
        Arrays.stream(type.getGenericInterfaces())
                .map(parent -> "IMPLEMENTS " + type.getName() + " " + parent.getTypeName())
                .sorted()
                .forEach(System.out::println);
        Arrays.stream(type.getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .map(FullPublicApiInventory::constructorSignature)
                .sorted()
                .forEach(System.out::println);
        Arrays.stream(type.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> !field.isEnumConstant() && !field.isSynthetic())
                .map(FullPublicApiInventory::fieldSignature)
                .sorted()
                .forEach(System.out::println);
        Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.isSynthetic() && !method.isBridge())
                .filter(method -> !isGeneratedEnumMethod(type, method))
                .map(FullPublicApiInventory::methodSignature)
                .sorted()
                .forEach(System.out::println);
    }

    private static String typeModifiers(Class<?> type) {
        StringBuilder result = new StringBuilder();
        if (Modifier.isAbstract(type.getModifiers()) && !type.isInterface()) {
            result.append("abstract ");
        }
        if (Modifier.isFinal(type.getModifiers()) && !type.isEnum() && !type.isRecord()) {
            result.append("final ");
        }
        if (type.isSealed()) {
            result.append("sealed ");
        }
        return result.toString();
    }

    private static void emitAnnotationMetadata(Class<?> type) {
        Retention retention = type.getAnnotation(Retention.class);
        System.out.println("META retention " + (retention == null ? "CLASS" : retention.value()));
        Target target = type.getAnnotation(Target.class);
        String targets = target == null ? "ALL" : Arrays.stream(target.value())
                .map(ElementType::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("NONE");
        System.out.println("META target " + targets);
        System.out.println("META documented " + type.isAnnotationPresent(Documented.class));
    }

    private static boolean isGeneratedEnumMethod(Class<?> type, Method method) {
        return type.isEnum()
                && ((method.getName().equals("values") && method.getParameterCount() == 0)
                || (method.getName().equals("valueOf")
                && Arrays.equals(method.getParameterTypes(), new Class<?>[] {String.class})));
    }

    private static String constructorSignature(Constructor<?> constructor) {
        return "CONSTRUCTOR " + constructor.getDeclaringClass().getName() + "("
                + parameterTypes(constructor.getAnnotatedParameterTypes(), constructor.isVarArgs())
                + ")" + exceptions(constructor.getGenericExceptionTypes());
    }

    private static String fieldSignature(Field field) {
        return "FIELD " + memberModifiers(field.getModifiers()) + annotatedType(field.getAnnotatedType())
                + " " + field.getDeclaringClass().getName() + "#" + field.getName();
    }

    private static String methodSignature(Method method) {
        Object defaultValue = method.getDefaultValue();
        String defaultText = defaultValue == null ? "" : " DEFAULT " + annotationValue(defaultValue);
        return "METHOD " + memberModifiers(method.getModifiers())
                + annotatedType(method.getAnnotatedReturnType()) + " "
                + method.getDeclaringClass().getName() + "#" + method.getName() + "("
                + parameterTypes(method.getAnnotatedParameterTypes(), method.isVarArgs()) + ")"
                + exceptions(method.getGenericExceptionTypes()) + defaultText;
    }

    private static String memberModifiers(int modifiers) {
        StringBuilder result = new StringBuilder();
        if (Modifier.isStatic(modifiers)) {
            result.append("static ");
        }
        if (Modifier.isAbstract(modifiers)) {
            result.append("abstract ");
        }
        if (Modifier.isFinal(modifiers)) {
            result.append("final ");
        }
        return result.toString();
    }

    private static String parameterTypes(AnnotatedType[] types, boolean varArgs) {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < types.length; index++) {
            String name = annotatedType(types[index]);
            if (varArgs && index == types.length - 1 && name.endsWith("[]")) {
                name = name.substring(0, name.length() - 2) + "...";
            }
            names.add(name);
        }
        return String.join(",", names);
    }

    private static String annotatedType(AnnotatedType type) {
        String value = type.toString()
                .replace("@org.jspecify.annotations.Nullable() ", "@Nullable ")
                .replace("@org.jspecify.annotations.NullnessUnspecified() ",
                        "@NullnessUnspecified ");
        return value;
    }

    private static String exceptions(java.lang.reflect.Type[] types) {
        if (types.length == 0) {
            return "";
        }
        return " THROWS " + Arrays.stream(types)
                .map(java.lang.reflect.Type::getTypeName)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
    }

    private static String annotationValue(Object value) {
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<String> values = new ArrayList<>();
            for (int index = 0; index < length; index++) {
                values.add(String.valueOf(java.lang.reflect.Array.get(value, index)));
            }
            return "[" + String.join(",", values) + "]";
        }
        return String.valueOf(value);
    }

    private static boolean hasAnnotation(Annotation[] annotations, String annotationName) {
        return Arrays.stream(annotations)
                .anyMatch(annotation -> annotation.annotationType().getName().equals(annotationName));
    }
}
