/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.annotation;

import dev.krud.shapeshift.FieldMapper;
import dev.krud.shapeshift.transformer.DefaultTransformer;
import dev.krud.shapeshift.transformer.base.FieldTransformer;

import java.lang.annotation.*;

/**
 * Indicates that an annotated field should be mapped to a field on a different model by the {@link FieldMapper}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Repeatable(MappedFields.class)
public @interface MappedField {

    /**
     * The target class to map the field to
     */
    Class<?> target() default Void.class;


    /**
     * (Optional) The field name to map the value from.
     * When used  at the field level, allows for mapping of nested values
     * If left empty at the type level, an exception will be thrown. Otherwise, the name of the field will be used.
     */
    String mapFrom() default "";

    /**
     * (Optional) The field name to map the value to.
     * If left empty, the name of the field will be used.
     */
    String mapTo() default "";

    /**
     * The {@link FieldTransformer} to use on the value
     */
    Class<? extends FieldTransformer> transformer() default DefaultTransformer.class;

    /**
     * Bean name for a defined {@link FieldTransformer} bean to use on the value
     * Supersedes {@link MappedField#transformer()} if specified
     */
    String transformerRef() default "";
}

