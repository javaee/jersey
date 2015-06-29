This is experimental module that provides JAX-RS Message Body Writer & Reader using Kryo serialization framework.

# How to use it

Add dependency to the module:

```xml
<dependency>
    <groupId>org.glassfish.jersey.media</groupId>
    <artifactId>jersey-media-kryo</artifactId>
    <version>${jersey.version}</version>
</dependency>
```

And now you can consume or produce entities (de)serialized by Kryo. Just use `application/x-kryo` MIME type, e.g.:

```java
@Path("/rest")
@Consumes("application/x-kryo")
@Produces("application/x-kryo")
public class MyResource { ... }
```
