[![Build Status](https://travis-ci.com/antkorwin/xsync.svg?branch=master)](https://travis-ci.com/antkorwin/xsync)
[![codecov](https://codecov.io/gh/antkorwin/xsync/branch/master/graph/badge.svg)](https://codecov.io/gh/antkorwin/xsync)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.antkorwin/xsync/badge.svg)](https://search.maven.org/search?q=g:com.antkorwin%20AND%20a:xsync)

# XSync Library


## What is it

XSync is a thread-safe mutex factory, that provide 
ability to synchronize by the value of the object(not by the object).

And you can use it for all type of objects which you need.

![XSync mutex behavior](http://antkorwin.com/concurrency/diag-0672834a7737bb323990aabe3bcb5ce6.png)

You can read more about this library here: 
[Synchronized by the value of the object](http://antkorwin.com/concurrency/synchronization_by_instance.html) 

## Add dependencies 

You need to add the next dependencies:

```xml
<dependency>
    <groupId>com.antkorwin</groupId>
    <artifactId>xsync</artifactId>
    <version>1.0</version>
</dependency>
```

If you use the XSync without Spring Framework or SpringBoot, 
you will need to add the spring framework as dependency(since version - 3.2):

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>5.0.7.RELEASE</version>  
    <scope>compile</scope>
</dependency>
```

## Create the XSync instance 

You can create XSync instances parametrized by the type of key which you need.
For example we create two XSync instances for Integer and String keys and made it as Spring beans:

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


### Simple example 
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

Result of this test:

![result](http://antkorwin.com/concurrency/lock_test.png)


### Example of usage in a banking system 

You can read more details about this example in my article: [Synchronized by the value of the object]
(http://antkorwin
.com/concurrency/synchronization_by_value.html)

A business logic, that we need to synchronize:

```java
public class PaymentService {

    ...

    @Autowired
    private XSync<UUID> xSync;

    public void withdrawMoney(UUID userId, int amountOfMoney) {
        xSync.execute(userId, () -> {  
            Result result = externalCashBackService.evaluateCashBack(userId, amountOfMoney); 
            accountService.transfer(userId, amountOfMoney + result.getCashBackAmount()); 
            externalCashBackService.cashBackComplete(userId, result.getCashBackAmount()); 
        });
    }
}
```

And places of usages:

```java
public void threadA() {
    paymentService.withdrawMoney(UUID.fromString("11111111-2222-3333-4444-555555555555"), 1000);
}


public void threadB() {
    paymentService.withdrawMoney(UUID.fromString("11111111-2222-3333-4444-555555555555"), 5000);
}
```


### Samples on github

You can find a project with examples here: [github.com/antkorwin/xsync-example](https://github.com/antkorwin/xsync-example)