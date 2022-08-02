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

@DefaultMappingTarget(SimpleEntityDisplay.class) // The default target class for the mapping
public class SimpleEntity {
    public SimpleEntity(String name, String description, String privateData) {
        this.name = name;
        this.description = description;
        this.privateData = privateData;
    }

    @MappedField // This field will be mapped to the "name" field in the target class
    private String name;

    @MappedField // This field will be mapped to the "name" field in the target class
    private String description;

    // This field will not be mapped to any field in the target class
    private String privateData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrivateData() {
        return privateData;
    }

    public void setPrivateData(String privateData) {
        this.privateData = privateData;
    }

    @Override
    public String toString() {
        return "SimpleEntity{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", privateData='" + privateData + '\'' +
                '}';
    }
}
