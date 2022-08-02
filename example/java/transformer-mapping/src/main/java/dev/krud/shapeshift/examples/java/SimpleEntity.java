/*
 * Copyright KRUD 2022
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.krud.shapeshift.examples.java;

import dev.krud.shapeshift.resolver.annotation.DefaultMappingTarget;
import dev.krud.shapeshift.resolver.annotation.MappedField;

import java.util.Date;

@DefaultMappingTarget(SimpleEntityDisplay.class) // The default target class for the mapping
public class SimpleEntity {
    @MappedField // This field will be mapped to the creationDate field of the target class, and use the DateToLongTransformer implicitly
    private Date creationDate;
    @MappedField(transformer = StringToCommaSeparatedStringListTransformer.class, mapTo = "stringList") // This field will be mapped to the stringList field of the target class, and use the StringToCommaSeparatedStringListTransformer as it is explicitly specified
    private String commaDelimitedString;

    public SimpleEntity(Date creationDate, String commaDelimitedString) {
        this.creationDate = creationDate;
        this.commaDelimitedString = commaDelimitedString;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCommaDelimitedString() {
        return commaDelimitedString;
    }

    public void setCommaDelimitedString(String commaDelimitedString) {
        this.commaDelimitedString = commaDelimitedString;
    }

    @Override
    public String toString() {
        return "SimpleEntity{" +
                "creationDate=" + creationDate +
                ", commaDelimitedString='" + commaDelimitedString + '\'' +
                '}';
    }
}
