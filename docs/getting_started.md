# Overview

ShapeShift is an intelligent, annotation-based object mapper for JVM. Allowing anything from the simplest mappings to complex use-cases with transformers.

- [Installation](#installation)
    + [Maven](#maven)
    + [Gradle](#gradle)
        - [Groovy DSL](#groovy-dsl)
        - [Kotlin DSL](#kotlin-dsl)
- [Examples](#examples)
    * [Simple Example](#simple-example)
    * [Advanced Example](#advanced-example)
    * [Mapping with transformers](#mapping-with-transformers)
- [In Conclusion](#in-conclusion)

# Installation

### Maven
```xml
<dependency>
  <groupId>dev.krud</groupId>
  <artifactId>shapeshift</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Gradle
#### Groovy DSL
```groovy
implementation 'dev.krud:shapeshift:0.1.0'
```
#### Kotlin DSL
```kotlin
implementation("dev.krud:shapeshift:0.1.0")
```

# Examples

## Simple Example

In this example, we'll review the simplest use-case for ShapeShift, a simple mapping between two classes.

We start by defining two classes, our source class `SimpleEntity` and our destination class `SimpleEntityDisplay`.

```kotlin
data class SimpleEntity(
    val name: String,
    val description: String,
    val privateData: String
)
```

```kotlin
data class SimpleEntityDisplay(
    val name: String = "",
    val description: String = ""
)
```

Due to the fact that ShapeShift uses reflection behind the scenes, destination classes should have a no arg constructor. Alternatively, you can also pass already-instantiated destination objects to the `map` method.

We can now start adding our annotations to the `SimpleEntity` class. In this example, we want to map the `name` and `description` fields to the `name` and `description` fields of the `SimpleEntityDisplay` class, but not the `privateData` field. 

To achieve this, we will use the @MappedField annotation on both of these fields. Additionally, we will define `@DefaultMappingTarget` on the `SimpleEntity` class, which will indicate that all fields annotated with `@MappedField` that do not specify a target should be mapped to the `SimpleEntityDisplay` class.


```kotlin
@DefaultMappingTarget(SimpleEntityDisplay::class)
data class SimpleEntity(
    @MappedField
    val name: String,
    @MappedField
    val description: String,
    val privateData: String
)
```

To instantiate ShapeShift, do the following:
```kotlin
val shapeShift = ShapeShift()
```

Now let's write a simple test to check this scenario

```kotlin
@Test
internal fun `test simple mapping`() {
    val shapeShift = ShapeShift()
    val simpleEntity = SimpleEntity("test", "test description", "private data")
    val result = shapeShift.map<SimpleEntityDisplay>(simpleEntity)
    expectThat(result.name)
        .isEqualTo("test")
    expectThat(result.description)
        .isEqualTo("test description")
}
```

Additionally, we can also pass a destination object to the `map` method, let's write a test to check this scenario as well.

```kotlin
@Test
internal fun `test simple mapping with premade destination instance`() {
    val shapeShift = ShapeShift()
    val simpleEntity = SimpleEntity("test", "test description", "private data")
    val result = shapeShift.map(simpleEntity, SimpleEntityDisplay())
    expectThat(result.name)
        .isEqualTo("test")
    expectThat(result.description)
        .isEqualTo("test description")
}
```

You can check out the full example [here](../example/kotlin/simple-mapping)

## Advanced Example

In this more advanced example, we'll review mapping nested fields, as well as defining mappings to different targets.

Once again we start by defining our source and destination classes, and additionally a child class which will be used by both.

Starting with the child class
```kotlin
data class AdvancedChildEntity(
    val childName: String
)
```

Followed by our source class;

```kotlin
data class AdvancedEntity(
    val name: String,
    val firstChild: AdvancedChildEntity,
    val secondChild: AdvancedChildEntity
)
```

In this example, we will have two separate destination classes, `AdvancedEntityDisplay` and `ReducedAdvancedEntityDisplay`.

```kotlin
data class AdvancedEntityDisplay(
    val name: String = "",
    val firstChildName: String = "",
    val secondChildName: String = ""
)
```

```kotlin
data class ReducedAdvancedEntityDisplay(
    val name: String = "",
    val firstChildName: String = ""
)
```

Like before, let's start by adding our annotations to the source class. We will define a `@DefaultMappingTarget` annotation on the `AdvancedEntity` class, which will indicate that all fields annotated with `@MappedField` that do not specify a target should be mapped to the `AdvancedEntityDisplay` class. Seeing as we have two destinations in this example, we will have to define some of the mapping targets manually. 

We'll start by defining a simple `@MappedField` on the `name` field for both targets. We will also define a `@MappedField` annotation on the `firstChild` field, which will indicate that it should be mapped to the `firstChildName` field on the `AdvancedEntityDisplay` class, and repeat the same annotation for `secondChild`. To achieve this we will use the `mapFrom` and `mapTo` parameters. Notice that by doing this, we have extracted the `name` field from the `AdvancedEntity` class, and mapped it directly to the `*ChildName` field on the `AdvancedEntityDisplay` class. The `ReducedAdvancedEntityDisplay` class will have the same mapping as the `AdvancedEntityDisplay` class, but will omit the `secondChildName` field;

```kotlin
@DefaultMappingTarget(AdvancedEntityDisplay::class)
data class AdvancedEntity(
    @MappedField
    @MappedField(target = ReducedAdvancedEntityDisplay::class)
    val name: String,

    @MappedField(mapFrom = "childName", mapTo = "firstChildName")
    @MappedField(target = ReducedAdvancedEntityDisplay::class, mapFrom = "childName", mapTo = "firstChildName")
    val firstChild: AdvancedChildEntity,

    @MappedField(mapFrom = "childName", mapTo = "secondChildName")
    val secondChild: AdvancedChildEntity
)
```

To instantiate ShapeShift, do the following:
```kotlin
val shapeShift = ShapeShift()
```

Finally, let's write two tests to verify that our mapping is working correctly for both targets

```kotlin
@Test
@Test
fun `test advanced mapping for AdvancedEntityDisplay`() {
    val shapeShift = ShapeShift()
    val simpleEntity = AdvancedEntity(
            "test",
            AdvancedChildEntity("first child"),
            AdvancedChildEntity("second child")
    )
    val result = shapeShift.map<AdvancedEntityDisplay>(simpleEntity)
    expectThat(result.name)
            .isEqualTo("test")
    expectThat(result.firstChildName)
            .isEqualTo("first child")
    expectThat(result.secondChildName)
            .isEqualTo("second child")
}

@Test
fun `test advanced mapping for ReducedAdvancedEntityDisplay`() {
    val shapeShift = ShapeShift()
    val simpleEntity = AdvancedEntity(
            "test",
            AdvancedChildEntity("first child"),
            AdvancedChildEntity("second child")
    )
    val result = shapeShift.map<ReducedAdvancedEntityDisplay>(simpleEntity)
    expectThat(result.name)
            .isEqualTo("test")
    expectThat(result.firstChildName)
            .isEqualTo("first child")
}
```

You can check out the full example [here](../example/kotlin/advanced-mapping)

## Mapping with transformers

In this last example we will see how we can use transformers to map fields. Field Transformers are a way to transform fields from one class to another. For example, you might want to map a field from a `String` to a `List<String>` where the source field is comma delimited. In our example, we will explore this use case, as well as an implicit (default) transformation from `Date` to `Long` (milliseconds).

Like before, we start by defining our source and destination class;

```kotlin
data class SimpleEntity(
    val creationDate: Date,
    val commaDelimitedString: String, 
)
```

```kotlin
data class SimpleEntityDisplay(
    val creationDate: Long = 0,
    val stringList: List<String> = emptyList()
)
```

Let's first create our custom `StringToListTransformer`;
```kotlin
class StringToListTransformer : FieldTransformer<String, List<String>> {
    override val fromType: Class<String> = String::class.java
    override val toType: Class<List<*>> = List::class.java

    override fun transform(fromField: Field, toField: Field, originalValue: String?, fromObject: Any, toObject: Any): List<String>? {
        return originalValue?.split(",")
    }
}
```

Since we're using custom transformers, we will have to instantiate ShapeShift using `ShapeShiftBuilder` and define our two transformers. In ShapeShift, you define a transformer by providing a `TransformerRegistration` object. 

The registration object is used to define the type of the transformer, its instance, as well as its name and whether it's a default transformer. We will register the `DateToLongTransformer` as a default transformer, and the `StringToListTransformer` as a normal transformer. Note we can also provide a custom name for our transformer which we can then use in the `transformerRef` field of the `MappedField` annotation.

```kotlin
    val shapeShift = ShapeShiftBuilder()
        .withTransformer(DateToLongTransformer(), default = true)
        .withTransformer(StringToCommaSeparatedStringListTransformer())
        .build()
```

We can now add our annotations;
```kotlin
@DefaultMappingTarget(SimpleEntityDisplay::class)
data class SimpleEntity(
        @MappedField
        val creationDate: Date,

        @MappedField(transformer = StringToCommaSeparatedStringListTransformer::class, mapTo = "stringList")
        val commaDelimitedString: String
)
```

Note that we did not need to specify a transformer on `creationDate` since the `DateToLongTransformer` is a default transformer for the `Date` type with a `Long` destination type.

Let's write a test to verify that our mapping is correct;

```kotlin
@Test
fun `test mapping for SimpleEntityDisplay`() {
    val shapeShift = ShapeShiftBuilder()
        .withTransformer(DateToLongTransformer(), default = true)
        .withTransformer(StringToCommaSeparatedStringListTransformer())
        .build()
    val simpleEntity = SimpleEntity(
            Date(),
            "one,two,three"
    )
    val result = shapeShift.map<SimpleEntityDisplay>(simpleEntity)
    expectThat(result.creationDate)
            .isEqualTo(simpleEntity.creationDate.time)
    expectThat(result.stringList)
            .isEqualTo(listOf("one", "two", "three"))
}
```

You can check out the full example [here](../example/kotlin/transformer-mapping)

# In Conclusion

In this tutorial, we've covered the basics of using ShapeShift in three different scenarios. We've covered the basics and more advanced usage of using the `MappedField` annotation to map fields from one class to another, and we've covered the basics of using custom transformers to map fields from one class to another.
