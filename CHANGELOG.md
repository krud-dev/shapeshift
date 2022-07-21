# [](https://github.com/krud-dev/shapeshift/compare/v0.4.0...v) (2022-07-21)



# [0.4.0](https://github.com/krud-dev/shapeshift/compare/v0.3.0...v0.4.0) (2022-07-21)


### Bug Fixes

* fix generic bug with DSL ([a4cdde0](https://github.com/krud-dev/shapeshift/commit/a4cdde0283970fb13c04370e1789c0838cf0b449))


### Features

* add mapCollection ([2cba93f](https://github.com/krud-dev/shapeshift/commit/2cba93fc02968e504d8e18f4e966ccca6b8c111d))
* move originalValue to the first position in MappingTransformerContext to allow for destructuring ([972656b](https://github.com/krud-dev/shapeshift/commit/972656b5b5ca45d6ba112b1d729dc9c02af1bc6c))



# [0.3.0](https://github.com/krud-dev/shapeshift/compare/v0.2.0...v0.3.0) (2022-07-18)


### Bug Fixes

* upgrade org.jetbrains.kotlin:kotlin-stdlib-jdk8 from 1.6.21 to 1.7.0 ([dea01cf](https://github.com/krud-dev/shapeshift/commit/dea01cfdb6e9c4561076b0c92dc816262ff9c5d5))


### Features

* BREAKING CHANGE: remove ability to name transformer ([1a5f5e8](https://github.com/krud-dev/shapeshift/commit/1a5f5e895e1e448d24145ee275f9b9fea27a7f00))



# [0.2.0](https://github.com/krud-dev/shapeshift/compare/v0.1.0...v0.2.0) (2022-07-10)


### Bug Fixes

* change ClassPair to own data class ([5dd5322](https://github.com/krud-dev/shapeshift/commit/5dd532231ad6238bc67240d56cbda4af566bbbf4))
* Change error message when a field is not found ([9bb080b](https://github.com/krud-dev/shapeshift/commit/9bb080b15be59be685cc99f0ae5e407e2ec444ef))


### Features

* add ad hoc transformers ([8998f34](https://github.com/krud-dev/shapeshift/commit/8998f347d89aca0aa3aaf70689a073f473eace01))
* add conditions ([fb67786](https://github.com/krud-dev/shapeshift/commit/fb677863ed29151006389942738fce72b928f71d))
* add decorator registration ([794390f](https://github.com/krud-dev/shapeshift/commit/794390f2287f1977dbf0118c203fe8e9ab581f9f))
* add decorators ([43572f9](https://github.com/krud-dev/shapeshift/commit/43572f9f8c0507a4d38c9935c1d9bcbf2f9976cd))
* add default transformers ([4b855ea](https://github.com/krud-dev/shapeshift/commit/4b855ea2e13d5295367b3701f6dade3f2663dac3))
* add programmatic resolver ([a61d0c6](https://github.com/krud-dev/shapeshift/commit/a61d0c64e8353d5fcc65a9f01265197ab4efd578))
* add resolver abstraction ([efa1f31](https://github.com/krud-dev/shapeshift/commit/efa1f31545d9745c92dc6e92ef71ddbed49778c4))
* change decorators and transformers to use a context object ([757e8fa](https://github.com/krud-dev/shapeshift/commit/757e8fad7ecb4e2cf279568225c618723c9b2f1d))
* change transformers list to set ([c5841b6](https://github.com/krud-dev/shapeshift/commit/c5841b69414f28af8386625a650061c5517f2d9b))
* change way transformers are registered ([7238424](https://github.com/krud-dev/shapeshift/commit/7238424d234e0350c6c19eb76f3674271d827770))
* remove concrete type from field transformers ([c9b8f8d](https://github.com/krud-dev/shapeshift/commit/c9b8f8d27ca486c52d63f977a288a59c297229ac))
* **spring:** add support for bean decorators ([2cbcdd4](https://github.com/krud-dev/shapeshift/commit/2cbcdd450551f6826979f07f93c17b00c64c1c4b))



# [0.1.0](https://github.com/krud-dev/shapeshift/compare/v0.0.1...v0.1.0) (2022-05-20)


### Bug Fixes

* rename ShapeShiftCustomizer to ShapeShiftBuilderCustomizer ([a614b08](https://github.com/krud-dev/shapeshift/commit/a614b08d1b818ee0618bc922dd4001588ac9f7d3))
* upgrade org.jetbrains.kotlin:kotlin-stdlib-jdk8 from 1.6.20 to 1.6.21 ([7d008c2](https://github.com/krud-dev/shapeshift/commit/7d008c272bf3373e5da91f6a8ac68fb700bacc01))


### Features

* add inline map method for convenience ([b6670c4](https://github.com/krud-dev/shapeshift/commit/b6670c47011d47617f232e51d9e2a628d8143816))
* add map method which receives an already instantiated object ([8efa6cd](https://github.com/krud-dev/shapeshift/commit/8efa6cdc5cddfd198635585f6446234cc4580c5b))
* add ShapeShiftCustomizer ([17d6572](https://github.com/krud-dev/shapeshift/commit/17d6572a8ad8e1e33a448a0ab921ecbfe2c081cf))



## [0.0.1](https://github.com/krud-dev/shapeshift/compare/9585331c42d8ced5db7bce17fd38bdc364d990d7...v0.0.1) (2022-05-19)


### Bug Fixes

* add missing shapeshift dependency to the spring boot starter ([8686f56](https://github.com/krud-dev/shapeshift/commit/8686f564e7196dbc19fb8b6089fd0df3341bb72c))
* fix bug with toPath handling with qualifier ([2a2b4de](https://github.com/krud-dev/shapeshift/commit/2a2b4de401fd670828492c4aa13e784c5393ca78))
* fix bug with transformers of primitive types not unboxing ([409da96](https://github.com/krud-dev/shapeshift/commit/409da964c5c5e4f757efc70143e946fd4e7471ee))
* misc renames, misc visibility modifiers ([1c87f0d](https://github.com/krud-dev/shapeshift/commit/1c87f0d1c154f9c9066c5f0a154cccd7008d486d))
* remove unused reflection method ([5d4d964](https://github.com/krud-dev/shapeshift/commit/5d4d9648d611a91df4d27a11dbdb122af1cac981))
* rename FieldMapper to ShapeShift ([58d725c](https://github.com/krud-dev/shapeshift/commit/58d725cf866ad54a7e1216c2ffdd3977bdfafa8b))
* upgrade org.slf4j:slf4j-api from 1.7.32 to 1.7.36 ([dae7c41](https://github.com/krud-dev/shapeshift/commit/dae7c41d88feafa963fbddcea66d8a80af8193c7))


### Features

* add ShapeShiftBuilder ([73bb37f](https://github.com/krud-dev/shapeshift/commit/73bb37f5142282fdb317b0c19defc7a920c53bef))
* add withTransformer to ShapeShift builder without TransformerRegistration ([fd70454](https://github.com/krud-dev/shapeshift/commit/fd70454deb34071bce54b22a8a0aa213a066f352))
* change FieldTransformer from, to types to vals ([5c8ad83](https://github.com/krud-dev/shapeshift/commit/5c8ad83744d0e05f01313ba2c0049fafdad20965))
* kotlin refactor ([9585331](https://github.com/krud-dev/shapeshift/commit/9585331c42d8ced5db7bce17fd38bdc364d990d7))
* throw exception if transformer was not found ([a0120f8](https://github.com/krud-dev/shapeshift/commit/a0120f8d798d4361d61bd729cf526622cdec1643))



