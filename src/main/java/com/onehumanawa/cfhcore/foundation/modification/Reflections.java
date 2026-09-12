package com.onehumanawa.cfhcore.foundation.modification;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.onehumanawa.cfhcore.CFHCore;

import sun.misc.Unsafe;

@SuppressWarnings("deprecation")
final class Reflections {
    private static final Map<String, Field> FIELDS = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static final Map<Class<?>, List<Field>> FIELD_LISTS = new HashMap<>();
    private static final Unsafe UNSAFE = resolveUnsafe();

    private Reflections() {}

    static boolean set(Object target, String name, @Nullable Object value) {
        Field field = find(target.getClass(), name);
        if (field == null) {
            return false;
        }
        if (field.trySetAccessible()) {
            try {
                field.set(target, value);
                return true;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                CFHCore.LOGGER.error("[Modification] Cannot write {}#{}: {}", declaring(field), name, e.toString());
            }
        }
        try {
            writeUnsafe(target, field, value);
            return true;
        } catch (Throwable t) {
            CFHCore.LOGGER.error("[Modification] Cannot write {}#{}: {}", declaring(field), name, t.toString());
            return false;
        }
    }

    @Nullable
    static Object get(Object target, String name) {
        Field field = find(target.getClass(), name);
        if (field == null) {
            return null;
        }
        if (field.trySetAccessible()) {
            try {
                return field.get(target);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                CFHCore.LOGGER.error("[Modification] Cannot read {}#{}: {}", declaring(field), name, e.toString());
            }
        }
        try {
            return readUnsafe(target, field);
        } catch (Throwable t) {
            CFHCore.LOGGER.error("[Modification] Cannot read {}#{}: {}", declaring(field), name, t.toString());
            return null;
        }
    }

    static List<Field> fields(Class<?> type) {
        List<Field> cached = FIELD_LISTS.get(type);
        if (cached != null) {
            return cached;
        }
        List<Field> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (seen.add(field.getName())) {
                    result.add(field);
                }
            }
        }
        FIELD_LISTS.put(type, result);
        return result;
    }

    @Nullable
    static Object read(Field field, Object target) {
        if (field.trySetAccessible()) {
            try {
                return field.get(target);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                CFHCore.LOGGER.error("[Modification] Cannot read {}#{}: {}", declaring(field), field.getName(), e.toString());
            }
        }
        try {
            return readUnsafe(target, field);
        } catch (Throwable t) {
            CFHCore.LOGGER.error("[Modification] Cannot read {}#{}: {}", declaring(field), field.getName(), t.toString());
            return null;
        }
    }

    @Nullable
    static Object invoke(Object target, String name, Class<?> parameterType, @Nullable Object argument) {
        Method method = findMethod(target.getClass(), name, parameterType);
        if (method == null) {
            CFHCore.LOGGER.error("[Modification] Missing method '{}' on {}", name, target.getClass().getName());
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(target, argument);
        } catch (Throwable t) {
            CFHCore.LOGGER.error("[Modification] Cannot invoke {}#{}: {}", declaring(method), name, t.toString());
            return null;
        }
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, Class<?> parameterType) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, parameterType);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    static boolean copy(Object source, String name, Object target) {
        Field from = find(source.getClass(), name);
        Field to = from == null ? null : find(target.getClass(), name);
        if (from == null || to == null) {
            return false;
        }
        if (from.trySetAccessible() && to.trySetAccessible()) {
            try {
                to.set(target, from.get(source));
                return true;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                CFHCore.LOGGER.error("[Modification] Cannot copy {}#{}: {}", declaring(from), name, e.toString());
            }
        }
        try {
            writeUnsafe(target, to, readUnsafe(source, from));
            return true;
        } catch (Throwable t) {
            CFHCore.LOGGER.error("[Modification] Cannot copy {}#{}: {}", declaring(from), name, t.toString());
            return false;
        }
    }

    @Nullable
    private static Field find(Class<?> type, String name) {
        String key = type.getName() + "#" + name;
        Field cached = FIELDS.get(key);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(key)) {
            return null;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                FIELDS.put(key, field);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        MISSING.add(key);
        CFHCore.LOGGER.error("[Modification] Missing field '{}' on {}", name, type.getName());
        return null;
    }

    private static void writeUnsafe(Object target, Field field, @Nullable Object value) {
        long offset = UNSAFE.objectFieldOffset(field);
        Class<?> type = field.getType();
        if (type == boolean.class) {
            UNSAFE.putBoolean(target, offset, (Boolean) value);
        } else if (type == int.class) {
            UNSAFE.putInt(target, offset, (Integer) value);
        } else if (type == float.class) {
            UNSAFE.putFloat(target, offset, (Float) value);
        } else if (type == long.class) {
            UNSAFE.putLong(target, offset, (Long) value);
        } else if (type == double.class) {
            UNSAFE.putDouble(target, offset, (Double) value);
        } else {
            UNSAFE.putObject(target, offset, value);
        }
    }

    @Nullable
    private static Object readUnsafe(Object source, Field field) {
        long offset = UNSAFE.objectFieldOffset(field);
        Class<?> type = field.getType();
        if (type == boolean.class) {
            return UNSAFE.getBoolean(source, offset);
        } else if (type == int.class) {
            return UNSAFE.getInt(source, offset);
        } else if (type == float.class) {
            return UNSAFE.getFloat(source, offset);
        } else if (type == long.class) {
            return UNSAFE.getLong(source, offset);
        } else if (type == double.class) {
            return UNSAFE.getDouble(source, offset);
        }
        return UNSAFE.getObject(source, offset);
    }

    @Nullable
    private static Unsafe resolveUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Throwable t) {
            CFHCore.LOGGER.error("[Modification] Unsafe unavailable: {}", t.toString());
            return null;
        }
    }

    private static String declaring(Field field) {
        return field.getDeclaringClass().getName();
    }

    private static String declaring(Method method) {
        return method.getDeclaringClass().getName();
    }
}
