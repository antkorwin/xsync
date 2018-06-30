# XSync Library


## What is it

XSync is a thread-safe mutex factory, that provide 
ability to synchronize by the value of the object(not by the object).

And you can use it for all type of object which you need.

![XSync mutex behavior](http://antkorwin.com/concurrency/diag-0672834a7737bb323990aabe3bcb5ce6.png)

You can read more about this library here: 
[Synchronization by the instance of the class](http://antkorwin.com/concurrency/synchronization_by_instance.html) 

## Add dependencies 

You need to add next dependencies:

```xml
<dependency>
	<groupId>com.github.antkorwin</groupId>
	<artifactId>xsync</artifactId>
	<version>0.4</version>
</dependency>
```

And repository:

```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
```

## Create the XSync instance that your need

```java
@Configuration
public class XSyncConfig {
   
    @Bean
    public XSync<Integer> intXSync(){
        return new XSync<>();
    }
    
    @Bean
    public XSync<String> xSync(){
        return new XSync<>();
    }
}
```

## Use it

```java
@Autowired
private XSync<String> xSync;

@Test
public void testLock() throws InterruptedException {
    // Arrange
    NonAtomicInt variable = new NonAtomicInt(0);
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    // Act
    executorService.submit(() -> {
        System.out.println("firstThread started.");
        xSync.execute(new String("key"), () -> {
            System.out.println("firstThread took a lock");
            sleep(2);
            variable.increment();
            System.out.println("firstThread released a look");
        });
    });

    executorService.submit(() -> {
        sleep(1);
        System.out.println("secondThread started.");
        xSync.execute(new String("key"), () -> {
            System.out.println("secondThread took a lock");

            // Assert
            Assertions.assertThat(variable.getValue()).isEqualTo(1);
            sleep(1);
            variable.increment();
            System.out.println("secondThread released a look");
        });
    });

    executorService.awaitTermination(5, TimeUnit.SECONDS);

    // Assert
    Assertions.assertThat(variable.getValue()).isEqualTo(2);
} 

``` 


You can find a project with examples here: [github.com/antkorwin/xsync-example](https://github.com/antkorwin/xsync-example)