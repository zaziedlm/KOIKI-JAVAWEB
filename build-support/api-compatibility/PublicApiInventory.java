import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Emits the normalized, reviewable Public API inventory for the two C3 artifacts. */
final class PublicApiInventory {

    private PublicApiInventory() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Usage: PublicApiInventory.java <artifact-label> <jar> [<artifact-label> <jar> ...]");
        }

        System.out.println("# KOIKI Phase 1a C3 Public API inventory v1");
        for (int index = 0; index < args.length; index += 2) {
            emitArtifact(args[index], Path.of(args[index + 1]));
        }
    }

    private static void emitArtifact(String artifactId, Path jarPath) throws Exception {
        System.out.println("ARTIFACT " + artifactId);
        for (Class<?> type : publicTypes(jarPath)) {
            emitType(type);
        }
    }

    private static List<Class<?>> publicTypes(Path jarPath) throws IOException, ClassNotFoundException {
        List<Class<?>> types = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            for (JarEntry entry : jarFile.stream()
                    .filter(candidate -> !candidate.isDirectory())
                    .filter(candidate -> candidate.getName().endsWith(".class"))
                    .filter(candidate -> !candidate.getName().equals("module-info.class"))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList()) {
                String className = entry.getName()
                        .substring(0, entry.getName().length() - ".class".length())
                        .replace('/', '.');
                Class<?> type = Class.forName(className, false, PublicApiInventory.class.getClassLoader());
                if (Modifier.isPublic(type.getModifiers())) {
                    types.add(type);
                }
            }
        }
        types.sort(Comparator.comparing(Class::getName));
        return types;
    }

    private static void emitType(Class<?> type) {
        if (type.isAnnotation()) {
            System.out.println("TYPE annotation " + type.getName());
            emitAnnotationMetadata(type);
        } else if (type.isEnum()) {
            System.out.println("TYPE enum " + type.getName());
            for (Object constant : type.getEnumConstants()) {
                System.out.println("ENUM " + type.getName() + " " + constant);
            }
        } else if (type.isInterface()) {
            System.out.println("TYPE interface " + type.getName());
        } else {
            String finalModifier = Modifier.isFinal(type.getModifiers()) ? " final" : "";
            System.out.println("TYPE class" + finalModifier + " " + type.getName());
        }

        Arrays.stream(type.getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .map(PublicApiInventory::constructorSignature)
                .sorted()
                .forEach(System.out::println);
        Arrays.stream(type.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> !field.isEnumConstant())
                .filter(field -> !field.isSynthetic())
                .map(PublicApiInventory::fieldSignature)
                .sorted()
                .forEach(System.out::println);
        Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !method.isSynthetic())
                .filter(method -> !isGeneratedEnumMethod(type, method))
                .map(PublicApiInventory::methodSignature)
                .sorted()
                .forEach(System.out::println);
    }

    private static void emitAnnotationMetadata(Class<?> type) {
        Retention retention = type.getAnnotation(Retention.class);
        System.out.println("META retention " + (retention == null ? "CLASS" : retention.value()));

        Target target = type.getAnnotation(Target.class);
        String targets = target == null
                ? "ALL"
                : Arrays.stream(target.value())
                        .map(ElementType::name)
                        .sorted()
                        .reduce((left, right) -> left + "," + right)
                        .orElse("NONE");
        System.out.println("META target " + targets);
        System.out.println("META documented " + type.isAnnotationPresent(Documented.class));
    }

    private static boolean isGeneratedEnumMethod(Class<?> type, Method method) {
        if (!type.isEnum()) {
            return false;
        }
        return (method.getName().equals("values") && method.getParameterCount() == 0)
                || (method.getName().equals("valueOf")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[] {String.class}));
    }

    private static String constructorSignature(Constructor<?> constructor) {
        return "CONSTRUCTOR " + constructor.getDeclaringClass().getName()
                + "(" + parameterTypes(constructor.getParameterTypes(), constructor.isVarArgs()) + ")";
    }

    private static String fieldSignature(Field field) {
        return "FIELD " + modifiers(field.getModifiers()) + typeName(field.getType()) + " "
                + field.getDeclaringClass().getName() + "#" + field.getName();
    }

    private static String methodSignature(Method method) {
        return "METHOD " + modifiers(method.getModifiers()) + typeName(method.getReturnType()) + " "
                + method.getDeclaringClass().getName() + "#" + method.getName()
                + "(" + parameterTypes(method.getParameterTypes(), method.isVarArgs()) + ")";
    }

    private static String modifiers(int modifiers) {
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

    private static String parameterTypes(Class<?>[] parameterTypes, boolean varArgs) {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < parameterTypes.length; index++) {
            String name = typeName(parameterTypes[index]);
            if (varArgs && index == parameterTypes.length - 1) {
                name = name.substring(0, name.length() - 2) + "...";
            }
            names.add(name);
        }
        return String.join(",", names);
    }

    private static String typeName(Class<?> type) {
        return type.getTypeName();
    }
}
