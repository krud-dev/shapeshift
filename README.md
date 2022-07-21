<br/>

<p align="center">
  <a href="https://github.com/chakra-ui/chakra-ui">
    <img src=".assets/logo.png" alt="ShapeShift logo" width="250" />
  </a>
</p>
<h1 align="center">ShapeShift️</h1>

<div align="center">
A Kotlin library for intelligent object mapping and conversion between objects.
<br/>
<br/>

![Maven Central](https://img.shields.io/maven-central/v/dev.krud/shapeshift)
[![CircleCI](https://img.shields.io/circleci/build/github/krud-dev/shapeshift/master)](https://circleci.com/gh/krud-dev/shapeshift/tree/master)
[![Codecov](https://img.shields.io/codecov/c/gh/krud-dev/shapeshift?token=1EG9H9RK5Q)](https://codecov.io/gh/krud-dev/shapeshift)
[![GitHub](https://img.shields.io/github/license/krud-dev/shapeshift)](https://github.com/krud-dev/shapeshift/blob/master/LICENSE)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](https://github.com/krud-dev/shapeshift/issues)

</div>

- [Overview](#overview)
- [Quickstart](#quickstart)
- [Documentation](#documentation)
- [Installation](#installation)
    * [Maven](#maven)
    * [Gradle](#gradle)
        + [Groovy DSL](#groovy-dsl)
        + [Kotlin DSL](#kotlin-dsl)
- [Requirements](#requirements)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## Overview

ShapeShift is a Kotlin first object mapping library. We have built ShapeShift because we wanted a simple to use, minimal boiler plate mapping engine, that is also flexible and supports the most advanced use cases.

Built with Kotlin in mind, ShapeShift was designed around its ecosystem and best practices. The library has 2 main tools for mapping:

* Annotations - Fully featured annotation based mapping, just add annotations to your objects and ShapeShift handles the rest. Including using custom field transformers, conditional mapping, advanced object decoration and much more.
* DSL - A Kotlin DSL allowing you to define the relations between objects. This allows you to map objects you can't change (or don't want to), like objects from 3rd party libraries. Additionally you can define inline transformations, conditions and decorations, enabling deep customization and very advanced mapping.

ShapeShift main features:

* Custom field transformers
* Default transformers
* Deep mapping
* Multiple mapping targets
* Conditional mapping
* Mapping decorators
* Seamless spring integration
* Native Android support


## Quickstart

### DSL Example
```kotlin
// Source Class
data class Source(
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate
)

// Target Class
data class Target(
    var firstName: String = "",
    var lastName: String = "",
    var birthYear: Int = 0
)

fun main() {
    /**
     * Initialize ShapeShift with a mapping definition from From to To
     */
    val shapeShift = ShapeShiftBuilder()
        .withMapping<Source, Target> {
            // Map firstName
            Source::firstName mappedTo Target::firstName
            // Map lastName
            Source::lastName mappedTo Target::lastName
            // Map birthDate to birthYear with a transformation function
            Source::birthDate mappedTo Target::birthYear withTransformer { (originalValue) ->
                originalValue?.year
            }
        }
        .build()

    // Initialize Source
    val source = Source("John", "Doe", LocalDate.of(1980, 1, 1))
    // Perform the mapping
    val result = shapeShift.map<Source, Target>(source)
    // Returns: To(firstName=John, lastName=Doe, birthYear=1980)
}
```

### Annotation Example
```kotlin
// Source Class
@DefaultMappingTarget(Target::class)
data class Source(
    @MappedField
    val firstName: String,
    @MappedField
    val lastName: String,
    @MappedField(mapTo = "birthYear", transformer = LocalDateToYearTransformer::class)
    val birthDate: LocalDate
)

// Target Class
data class Target(
    var firstName: String = "",
    var lastName: String = "",
    var birthYear: Int = 0
)

// Define the transformer which will transform the local date to a year
class LocalDateToYearTransformer : MappingTransformer<LocalDate, Int> {
    override fun transform(context: MappingTransformerContext<out LocalDate>): Int? {
        return context.originalValue?.year
    }
}

fun main() {
    /**
     * Initialize ShapeShift and register the transformer
     */
    val shapeShift = ShapeShiftBuilder()
        .withTransformer(LocalDateToYearTransformer())
        .build()

    // Initialize Source
    val source = Source("John", "Doe", LocalDate.of(1980, 1, 1))
    // Perform the mapping
    val result = shapeShift.map<Source, Target>(source)
    // Returns: To(firstName=John, lastName=Doe, birthYear=1980)
}
```
## Documentation

To learn how to get started with **ShapeShift**, visit the official documentation website. You'll find in-depth documentation, tips and guides to help you get up and running.

<p>
  <a href="https://shapeshift.krud.dev/">
    <img alt="Visit ShapeShift documentation" src=".assets/documentation.png" width="240" />
  </a>
</p>

## Installation

### Maven
```xml
<dependency>
  <groupId>dev.krud</groupId>
  <artifactId>shapeshift</artifactId>
  <version>0.3.0</version>
</dependency>
```

### Gradle
#### Groovy DSL
```groovy
implementation 'dev.krud:shapeshift:0.3.0'
```
#### Kotlin DSL
```kotlin
implementation("dev.krud:shapeshift:0.3.0")
```

## Requirements

* Minimum supported Kotlin version: 1.6.X
* Minimum supported Java version: 1.8

## Examples

The [example](example/) directory contains several independent scenarios for common use cases of this library.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change. See [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License
ShapeShift is licensed under the [MIT](https://choosealicense.com/licenses/mit/) license. For more information, please see the [LICENSE](LICENSE) file.