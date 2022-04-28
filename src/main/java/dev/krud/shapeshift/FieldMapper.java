/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift;

import dev.krud.shapeshift.annotation.DefaultMappingTarget;
import dev.krud.shapeshift.annotation.MappedField;
import dev.krud.shapeshift.annotation.ObjectFieldPair;
import dev.krud.shapeshift.dto.EntityStructureDTO;
import dev.krud.shapeshift.transformer.DefaultTransformer;
import dev.krud.shapeshift.transformer.base.FieldTransformer;
import dev.krud.shapeshift.util.ReflectionUtils;
import kotlin.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldMapper {

    private static final Map<Class<?>, Class<?>> PRIMITIVES;
    private static final String NODE_DELIMITER = ".";
    private static Map<Pair<Class<?>, Class<?>>, EntityStructureDTO> entityStructures = new HashMap<>();
    private static Map<Class<?>, Map<String, Field>> entityFieldMaps = new HashMap<>();

    static {
        PRIMITIVES = new HashMap<>();
        PRIMITIVES.put(boolean.class, Boolean.class);
        PRIMITIVES.put(byte.class, Byte.class);
        PRIMITIVES.put(short.class, Short.class);
        PRIMITIVES.put(char.class, Character.class);
        PRIMITIVES.put(int.class, Integer.class);
        PRIMITIVES.put(long.class, Long.class);
        PRIMITIVES.put(float.class, Float.class);
        PRIMITIVES.put(double.class, Double.class);
    }

    private Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, FieldTransformer> fieldTransformersByRef = new HashMap<>();
    private Map<Class<? extends FieldTransformer>, FieldTransformer> fieldTransformersByType = new HashMap<>();
    private Map<Pair<Class<?>, Class<?>>, FieldTransformer> defaultTransformers = new HashMap();
    private Map<Pair<Class<?>, Class<?>>, FieldTransformer> defaultTransformersCache = new HashMap();

    public void registerTransformer(String ref, FieldTransformer transformer) {
        fieldTransformersByRef.put(ref, transformer);
        fieldTransformersByType.put(transformer.getClass(), transformer);
        if (transformer.isDefault()) {
            registerDefaultTransformer(transformer);
        }
    }

    public void registerTransformer(Class<? extends FieldTransformer> clazz, FieldTransformer transformer) {
        fieldTransformersByType.put(clazz, transformer);
        if (transformer.isDefault()) {
            registerDefaultTransformer(transformer);
        }
    }

    public void registerDefaultTransformer(FieldTransformer transformer) {
        registerDefaultTransformer(transformer, transformer.fromType(), transformer.toType());
    }

    public void registerDefaultTransformer(FieldTransformer transformer, Class<?> fromType, Class<?> toType) {
        Pair key = new Pair<>(boxPrimitiveClazz(fromType), boxPrimitiveClazz(toType));
        FieldTransformer existing = defaultTransformers.get(key);
        if (existing != null) {
            throw new IllegalStateException("Cannot register default transformer for pair [ " + key + " ] - already registered by [ " + existing.getClass().getName() + " ]");
        }

        defaultTransformers.put(key, transformer);
    }

    public <T> T processMappedFields(Object object, Class<T> toClazz) {
        T toObject = ReflectionUtils.instantiate(toClazz);
        processMappedFields(object, toObject);
        return toObject;
    }

    public <T> void processMappedFields(Object object, T toObject) {

        EntityStructureDTO es = getEntityStructure(object.getClass(), toObject.getClass());

        for (MappedField typeAnnotation : es.getTypeAnnotations()) {
            if (typeAnnotation.mapFrom().trim().isEmpty()) {
                throw new RuntimeException("mapFrom can not be empty when used at a type level");
            }

            String fromPath = typeAnnotation.mapFrom();

            if (fromPath.toLowerCase().startsWith(object.getClass().getSimpleName().toLowerCase() + NODE_DELIMITER)) {
                fromPath = fromPath.substring(fromPath.indexOf(NODE_DELIMITER) + 1);
            }

            String toPath = typeAnnotation.mapTo();

            if (toPath.toLowerCase().startsWith(toObject.getClass().getSimpleName().toLowerCase() + NODE_DELIMITER)) {
                toPath = toPath.substring(toPath.indexOf(NODE_DELIMITER) + 1);
            }

            processMappedField(typeAnnotation, object, toObject, fromPath, toPath);
        }

        for (Map.Entry<Field, List<MappedField>> entry : es.getAnnotations().entrySet()) {
            for (MappedField annotation : entry.getValue()) {
                String fromPath = "";
                if (annotation.mapFrom().isEmpty()) {
                    fromPath = entry.getKey().getName();
                } else {
                    if (!annotation.mapFrom().startsWith(entry.getKey().getName() + NODE_DELIMITER)) {
                        fromPath = entry.getKey().getName() + NODE_DELIMITER + annotation.mapFrom();
                    } else {
                        fromPath = annotation.mapFrom();
                    }
                }
                processMappedField(annotation, object, toObject, fromPath, annotation.mapTo());
            }
        }
    }

    private void processMappedField(MappedField annotation, Object fromObject, Object toObject, String fromPath, String toPath) {
        if (fromPath == null || fromPath.trim().isEmpty()) {
            throw new RuntimeException("fromPath cannot be null or empty on class " + fromObject.getClass().getSimpleName());
        }

        ObjectFieldPair fromPair = getFieldByPath(fromPath, fromObject, SourceType.FROM);
        if (fromPair == null) {
            return;
        }

        toPath = toPath.isEmpty() ? fromPair.getField().getName() : toPath;
        ObjectFieldPair toPair = getFieldByPath(toPath, toObject, SourceType.TO);

        FieldTransformer transformer = getTransformer(annotation, fromPair, toPair);
        mapField(fromPair, toPair, transformer);
    }

    private ObjectFieldPair getFieldByPath(String path, Object object, SourceType type) {
        if (object == null) {
            if (type == SourceType.FROM) {
                return null;
            }
        }


        List<String> nodes = Stream.of(path.split("\\" + NODE_DELIMITER)).collect(Collectors.toList());


        Field field = getField(nodes.get(0), object.getClass());
        Objects.requireNonNull(field, "Field " + nodes.get(0) + " not found on class " + object.getClass().getSimpleName());
        if (nodes.size() == 1) {
            return new ObjectFieldPair(object, field, boxPrimitiveClazz(field.getType()));
        }

        nodes.remove(0);

        field.setAccessible(true);

        Object subObject = ReflectionUtils.getValue(field, object);

        if (subObject == null && type == SourceType.TO) {
            try {
                subObject = ReflectionUtils.instantiate(field.getType());
                ReflectionUtils.setValue(field, object, subObject);
            } catch (IllegalStateException e) {
                throw new RuntimeException("Could not instantiate " + field.getName() + " of type " + field.getType().getSimpleName() + " on class " + object.getClass().getSimpleName());
            }
        }

        return getFieldByPath(String.join(NODE_DELIMITER, nodes), subObject, type);
    }

    private Class<?> boxPrimitiveClazz(Class<?> clazz) {
        if (PRIMITIVES.containsKey(clazz)) {
            return PRIMITIVES.get(clazz);
        }
        return clazz;
    }


    private void mapField(ObjectFieldPair fromPair, ObjectFieldPair toPair, FieldTransformer transformer) {
        fromPair.getField().setAccessible(true);
        toPair.getField().setAccessible(true);

        Object value = ReflectionUtils.getValue(fromPair.getField(), fromPair.getObject());
        if (transformer != null) {
            value = transformer.transform(fromPair.getField(), toPair.getField(), value, fromPair.getObject(), toPair.getObject());
        }

        if (value != null) {
            try {
                ReflectionUtils.setValue(toPair.getField(), toPair.getObject(), value);
            } catch (Exception e) {
                IllegalStateException newException = new IllegalStateException("Could not map value " + fromPair.getField().getName() + " of class " + fromPair.getObject().getClass().getSimpleName() + " to " + toPair.getField().getName() + " of class" + toPair.getObject().getClass().getSimpleName());
                newException.initCause(e);
                throw newException;
            }
        }
    }

    private FieldTransformer getTransformer(MappedField annotation, ObjectFieldPair fromPair, ObjectFieldPair toPair) {
        Pair transformationPair = new Pair(fromPair.getType(), toPair.getType());
        log.trace("Attempting to find transformer for transformation pair [ " + transformationPair + " ]");
        FieldTransformer transformer = null;
        log.trace("Checking transformerRef field");
        if (!annotation.transformerRef().isEmpty()) {
            log.trace("transformerRef is not empty with value [ " + annotation.transformerRef() + " ]");
            transformer = fieldTransformersByRef.get(annotation.transformerRef());
            log.trace("Found transformer of type [ " + transformer.getClass().getName() + " ]");
        }

        if (transformer == null) {
            log.trace("Checking transformer field");
            if (annotation.transformer() == DefaultTransformer.class) {
                log.trace("Transformer is DefaultTransformer, attempting to find a default transformer");
                Pair<Class<?>, Class<?>> key = new Pair(fromPair.getType(), toPair.getType());

                FieldTransformer defaultTransformer;
                if (!defaultTransformersCache.containsKey(key)) {
                    defaultTransformer = getDefaultTransformer(key);
                    defaultTransformersCache.put(key, defaultTransformer);
                } else {
                    defaultTransformer = defaultTransformersCache.get(key);
                }

                if (defaultTransformer != null) {
                    log.trace("Found a default transformer of type [ " + defaultTransformer.getClass().getName() + " ]");
                    return defaultTransformer;
                }

                return null;
            }

            transformer = fieldTransformersByType.get(annotation.transformer());
            if (transformer == null) {
                transformer = ReflectionUtils.instantiate(annotation.transformer());
                fieldTransformersByType.put(annotation.transformer(), transformer);
            }
        }

        return transformer;
    }

    private FieldTransformer getDefaultTransformer(Pair<Class<?>, Class<?>> key) {
        FieldTransformer defaultTransformer = defaultTransformers.get(key);
        if (defaultTransformer == null) {
            defaultTransformer = defaultTransformers.entrySet()
                    .stream()
                    .filter((entry) -> {
                        Pair<Class<?>, Class<?>> pair = entry.getKey();
                        Class first = pair.getFirst();
                        Class second = pair.getSecond();
                        return first.isAssignableFrom(key.getFirst()) && second.isAssignableFrom(key.getSecond());
                    })
                    .map(entry -> entry.getValue())
                    .findFirst()
                    .orElse(null);
        }
        return defaultTransformer;
    }

    private EntityStructureDTO getEntityStructure(Class<?> fromClass, Class<?> toClass) {
        EntityStructureDTO entityStructureDTO = entityStructures.get(new Pair<Class<?>, Class<?>>(fromClass, toClass));
        if (entityStructureDTO != null) {
            return entityStructureDTO;
        }

        Map<Field, List<MappedField>> annotations = new HashMap<>();
        List<MappedField> typeAnnotations = new ArrayList<>();
        Class clazz = fromClass;
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            DefaultMappingTarget defaultMappingTarget = (DefaultMappingTarget) clazz.getDeclaredAnnotation(DefaultMappingTarget.class);
            Class<?> defaultFromClass = defaultMappingTarget == null ? Void.class : defaultMappingTarget.value();

            typeAnnotations.addAll(Arrays.stream((MappedField[]) clazz.getDeclaredAnnotationsByType(MappedField.class))
                    .filter(mappedField -> {
                        try {
                            return isOfType(defaultFromClass, mappedField.target(), toClass);
                        } catch (IllegalStateException e) {
                            throw new RuntimeException("Could not create entity structure for <" + fromClass.getSimpleName() + ", " + toClass.getSimpleName() + ">: " + e.getMessage());
                        }

                    })
                    .collect(Collectors.toList())
            );

            for (Field field : fields) {
                List<MappedField> availableAnnotations = Arrays.asList(field.getDeclaredAnnotationsByType(MappedField.class));
                availableAnnotations = availableAnnotations.stream()
                        .filter(mappedField -> {
                            try {
                                return isOfType(defaultFromClass, mappedField.target(), toClass);
                            } catch (IllegalStateException e) {
                                throw new IllegalStateException("Could not create entity structure for <" + fromClass.getSimpleName() + ", " + toClass.getSimpleName() + ">: " + e.getMessage());
                            }

                        })
                        .collect(Collectors.toList());

                annotations.put(field, availableAnnotations);
            }

            clazz = clazz.getSuperclass();
        }

        entityStructureDTO = new EntityStructureDTO(typeAnnotations, annotations);

        entityStructures.put(new Pair<Class<?>, Class<?>>(fromClass, toClass), entityStructureDTO);
        return entityStructureDTO;
    }

    private Map<String, Field> getFieldsMap(Class<?> clazz) {
        if (entityFieldMaps.get(clazz) != null) {
            return entityFieldMaps.get(clazz);
        }

        Map<String, Field> fieldsMap = new HashMap<>();
        Class classToGetFields = clazz;
        while (classToGetFields != null) {
            Field[] fields = classToGetFields.getDeclaredFields();
            for (Field field : fields) {
                fieldsMap.put(field.getName(), field);
            }
            classToGetFields = classToGetFields.getSuperclass();
        }

        entityFieldMaps.put(clazz, fieldsMap);

        return fieldsMap;
    }

    private Field getField(String name, Class<?> clazz) {
        return getFieldsMap(clazz).get(name);
    }

    private boolean isOfType(Class<?> defaultFromClass, Class<?> fromClass, Class<?> toClass) {
        if (fromClass == Void.class) {
            if (defaultFromClass == Void.class) {
                throw new IllegalStateException("No mapping target or default mapping target specified");
            }

            fromClass = defaultFromClass;
        }

        return fromClass.isAssignableFrom(toClass);
    }

    private enum SourceType {
        TO, FROM
    }
}
