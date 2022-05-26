![Maven Central](https://img.shields.io/maven-central/v/dev.krud/shapeshift)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/dev.krud/shapeshift?server=https%3A%2F%2Fs01.oss.sonatype.org&label=snapshot)

[![CircleCI](https://img.shields.io/circleci/build/github/krud-dev/shapeshift/master)](https://circleci.com/gh/krud-dev/shapeshift/tree/master)
[![Codecov](https://img.shields.io/codecov/c/gh/krud-dev/shapeshift?token=1EG9H9RK5Q)](https://codecov.io/gh/krud-dev/shapeshift)
[![GitHub](https://img.shields.io/github/license/krud-dev/shapeshift)](https://github.com/krud-dev/shapeshift/blob/master/LICENSE)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](https://github.com/krud-dev/shapeshift/issues)
# ShapeShift

A Kotlin library for intelligent object mapping and conversion between objects.

- [Getting Started](#getting-started)
- [Installation](#installation)
    * [Maven](#maven)
    * [Gradle](#gradle)
        + [Groovy DSL](#groovy-dsl)
        + [Kotlin DSL](#kotlin-dsl)
- [Requirements](#requirements)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)

## Getting Started

Find our full getting started guide [here](docs/getting_started.md).

## Installation

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

## Requirements

* Minimum supported Kotlin version: 1.6.X
* Minimum supported Java version: 1.8

## Examples

The [example](example/) directory contains several independent scenarios for common use cases of this library.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change. See [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License
ShapeShift is licensed under the [MIT](https://choosealicense.com/licenses/mit/) license. For more information, please see the [LICENSE](LICENSE) file.