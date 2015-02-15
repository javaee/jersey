## How to run?

To run all benchmarks execute:

`mvn clean install exec:exec` or `mvn clean install && java -jar target/benchmarks.jar`

To run specific benchmark, e.g. `JacksonBenchmark`:

`mvn clean install && java -cp target/benchmarks.jar org.glassfish.jersey.tests.performance.benchmark.JacksonBenchmark`

## Where to find more info/examples?

JMH page: http://openjdk.java.net/projects/code-tools/jmh/

JMH examples: http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/
