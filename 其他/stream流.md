https://blog.csdn.net/f641385712/article/details/81513796



# Stream流

Stream流是Java 8新增，主要用于简化集合操作

Stream流使用函数式编程，对集合进行链式操作

Stream是一个Java接口

```java
//java.util.stream.Stream
public interface Stream<T> extends BaseStream<T, Stream<T>> {
    ...
}
```

## 流创建

### 有限流

由具体数据/集合对象，创建流对象，所以流的长度由具体数据量控制，称为有限流

- `Arrays.stream`

`Arrays.stream`是一个静态方法，用于将泛型数组转换为流对象

```java
//java.util.Arrays#stream(T[])
public static <T> Stream<T> stream(T[] array) {
    return stream(array, 0, array.length);
}
```

- `Stream.of`

`Stream.of`接收数组/可变长度参数列表，实际调用了`Arrays.stream`

```java
//java.util.stream.Stream#of(T...)
public static<T> Stream<T> of(T... values) {
    return Arrays.stream(values);
}
```

- `Collection.stream`

`Collection.stream`用于由当前`Collection`对象创建一个流对象

使用这个方法，包括继承Collection的接口，如：Set，List，Map，SortedSet 等等

```java
//java.util.Collection#stream
default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
}
```

### 无限流

不是由具体数据创建，而是按规则无限制创建数据，称为无限流。

而我们不可能放任数据无限创建，导致内存溢出，所以无限流通常配合`limit`方法使用

- `Stream.iterate`

`Stream.iterate`用于创建由一个基础数据出发，通过一个`UnaryOperator`的函数式对象，不停处理返回，获取的无限流

```java
//java.util.stream.Stream#iterate
public static<T> Stream<T> iterate(final T seed, final UnaryOperator<T> f) {
    Objects.requireNonNull(f);
    final Iterator<T> iterator = new Iterator<T>() {
        @SuppressWarnings("unchecked")
        T t = (T) Streams.NONE;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public T next() {
            return t = (t == Streams.NONE) ? seed : f.apply(t);
        }
    };
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
        iterator,
        Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
}

@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {

    /* 处理返回新的对象 */
    static <T> UnaryOperator<T> identity() {
        return t -> t;
    }
}
```

- `Stream.generate`

`Stream.generate`类似于`Stream.iterate`，但是它是没有对应的基础对象，所有对象获取都是通过`Supplier`函数式对象返回

特：`Stream.generate`最大数量为`Long.MAX_VALUE`，而`Stream.iterate`没有最大大小

```java
//java.util.stream.Stream#generate
public static<T> Stream<T> generate(Supplier<T> s) {
    Objects.requireNonNull(s);
    return StreamSupport.stream(
        new StreamSpliterators.InfiniteSupplyingSpliterator.OfRef<>(Long.MAX_VALUE, s), false);
}

@FunctionalInterface
public interface Supplier<T> {
    /*对象获取*/
    T get();
}
```

### **示例**

```java
public class MyTest {
    public static void main(String[] args) {
        String[] arr = {"a", "b", "c", "d"};
        //Arrays.stream()
        Arrays.stream(arr).forEach(System.out::println);
        //Stream.of()
        Stream.of(arr).forEach(System.out::println);
        //Collection.stream()
        Arrays.asList(arr).stream().forEach(System.out::println);
        //Stream.iterate
        Stream.iterate(0, i -> i + 1).limit(10).forEach(System.out::println);
        //Stream.generate
        Stream.generate(() -> "x").limit(10).forEach(System.out::println);
    }
}
```

## 操作函数

操作函数分为两种：

- 中间操作
  - 中间操作函数，将会继续返回一个stream流对象，所以我们可以使用链式操作多个中间操作函数
- 终端操作
  - 流操作的结束函数，一般返回void/一个非流的结果对象

### 中间操作函数

#### filter

通过`Predicate`函数式对象，对流数据进行过滤，返回匹配结果的流对象

```java
Stream<T> filter(Predicate<? super T> predicate);
```

#### map

通过`Function`函数式对象，对流数据进行转换，返回转换后的目标结果的流对象

```java
<R> Stream<R> map(Function<? super T, ? extends R> mapper);
```

map方法返回对象为对应泛型T，而map方法还有几个变种，默认返回固定类型对象

- `IntStream mapToInt(ToIntFunction<? super T> mapper)`
- `LongStream mapToLong(ToLongFunction<? super T> mapper)`
- `DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper)`

#### flatMap

对于`map`操作，是将流对象中单个对象处理为目标对象。

查看下例：

```java
String[] arr = {"a,b,c", "e,f,g"};
Stream<String> stream = Arrays.stream(arr);
List<String[]> collect = stream.map(item -> item.split(",")).collect(Collectors.toList());

此时结果：[["a", "b", "c"],["e", "f", "g"]]
而我们的预期结果是：["a", "b", "c","e", "f", "g"]
    
所以此时flatMap就起作用了：
String[] arr = {"a,b,c", "e,f,g"};
Stream<String> stream = Arrays.stream(arr);
List<String> collect1 = stream.map(item -> item.split(",")).flatMap(Arrays::stream).collect(Collectors.toList());   

flatMap常用参数
    Arrays::stream
    Collections::stream
```

而`flatMap`变种有：

- `IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper)`
- `LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper)`
- `DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper)`

#### distinct

数据去重，要注意：

- `distinct`去重的方式是通过`hashCode()`和`equals()`方法来获取不同的元素，所以需要元素实现`hashCode()`和`equals()`
- `distinct`处理数据时，是保持其有序性的，这样需要大量的缓存开销

```java
Stream<T> distinct()    
```

#### sorted