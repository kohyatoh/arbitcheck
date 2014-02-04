# ArbitCheck

ArbitCheck is a property-based testing tool for Java.
Properties are described s a parameterized tests and then ArbitCheck generates random arguments and runs the tests.
ArbitCheck offers higher automation compared to other QuickCheck-style tools - ArbitCheck combines random method call to create random value, rather than using user-defined Arbitrary.

## Build
```
ant
```

## Usage
First, add `src/main/scripts` to your path.

```
arbitcheck.sh -cp your_class_path your_property
```

will perform random testing on your property.
`your_property` must be a full-qualified method name annotated with `@Check`.

`arbitcheck.sh` reports the test results and generates re-runnable JUnit test codes.

To compile and run these JUnit tests, you may invoke

```
arbitcheck-test-compile.sh -cp your_class_path
arbitcheck-test-run.sh -cp your_class_path
```

