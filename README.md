# reactify

[![Build Status](https://travis-ci.org/outr/reactify.svg?branch=master)](https://travis-ci.org/outr/reactify)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/759324d19db5496dbd9867b4a113c806)](https://www.codacy.com/app/matthicks/reactify?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=outr/reactify&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/759324d19db5496dbd9867b4a113c806)](https://www.codacy.com/app/matthicks/reactify?utm_source=github.com&utm_medium=referral&utm_content=outr/reactify&utm_campaign=Badge_Coverage)
[![Stories in Ready](https://badge.waffle.io/outr/reactify.png?label=ready&title=Ready)](https://waffle.io/outr/reactify)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/reactify)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.outr/reactify_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.outr/reactify_2.12)
[![Latest version](https://index.scala-lang.org/outr/reactify/reactify/latest.svg)](https://index.scala-lang.org/outr/reactify)

The first and only true Functional Reactive Programming framework for Scala.

## Justification

How can we say it's the first true FRP framework for Scala? Simple, because it is. In all other frameworks they add special
framework-specific functions to do things like math (ex. adding two variables together), collection building (ex. a
special implementation of `:::` to concatenate two variables containing lists), or similar mechanisms to Scala's built-in
collection manipulation (ex. `map`). These are great and mostly fill in the gaps necessary to solve your problems. But
the goal for Reactify was a bit loftier. We set out to create a system that actually allows you to use ANY Scala
functionality just like you would normally without any special magic (like Scala.rx's special operations require:
https://github.com/lihaoyi/scala.rx#additional-operations).

In Reactify you just write code like you normally would and as used `Var`s and `Val`s change the reactive properties they
have been assigned to will update as well. If you need a bit more clarification on just what the heck we mean, jump ahead
to the [More AdvancedExamples](https://github.com/outr/reactify#more-advanced-examples).

## Setup

reactify is published to Sonatype OSS and Maven Central currently supporting Scala and Scala.js on 2.11, 2.12, and 2.13.

Configuring the dependency in SBT simply requires:

```
libraryDependencies += "com.outr" %% "reactify" % "2.2.0"
```

or for Scala.js / cross-building:

```
libraryDependencies += "com.outr" %%% "reactify" % "2.2.0"
```

## Concepts

This framework is intentionally meant to be a simplistic take on properties and functional reactive concepts. There are
only four specific classes that really need be understood to take advantage of the framework:

- Observable - As the name suggests it is a simple trait that fires values and may be observed by Observers.
- Channel - The most simplistic representation of an Observable, simply provides a public `:=` to fire events. No state
is maintained.
- Val - Exactly as it is in Scala, this is a final variable. What is defined at construction is immutable. However, the
contents of the value, if they are `Observable` may change the ultimate value of this, so it is is `Observable` itself
and holds state.
- Var - Similar to `Val` except it is mutable as it mixes in `Channel` to allow setting of the current value.

`Val` and `Var` may hold formulas with `Observables`. These `Observables` are listened to when assigned so the wrapping
`Val` or `Var` will also fire an appropriate event. This allows complex values to be built off of other variables.

## Using

### Imports

Props is a very simple framework and though you'll likely want access to some of the implicit conversions made available
in the package, everything can be had with a single import:

```
import reactify._
```

### Creating Props

As discussed in the concepts there are only four major classes in Props (`Observable`, `Channel`, `Val`, and `Var`). Of
those classes, unless you are creating a custom `Observable` you will probably only deal with the latter three.

Creating instances is incredibly simple:

```
val myChannel = Channel[String]           // Creates a Channel that receives Strings
val myVar = Var[Int](5)                   // Creates a Var containing the explicit value `5`
val myVal = Val[Int](myVar + 5)           // Create a Val containing the sum of `myVar` + `5`
```

### Listening for Changes

This would all be pretty pointless if we didn't have the capacity to listen to changes on the values. Here we're going
to listen to `myVal` and `println` the new value when it changes:

```
myVal.attach { newValue =>
  println(s"myVal = $newValue")
}
```

### Modifying the Value

Since `myVal` is a `Val` it is immutable itself, but its value is derived from the formula `myVar + 5`. This means that
a change to `myVar` will cause the value of `myVal` to change as a result:

```
myVar := 10
```

The above code modifies `myVar` to have the new value of `10`. This will also cause `myVal` to re-evaluate and have the
new value of `15` (`myVar + 5`). As a result, the observer we attached above will output:

```
myVal = 15
```

### Derived Values

You can do clever things like define a value that is derived from other values:

```
val a = Var("One")
val b = Var("Two")
val c = Var("Three")

val list = Val(List(a, b, c))
list()      // Outputs List("One", "Two", "Three")

a := "Uno"
b := "Dos"
c := "Tres"

list()      // Outputs List("Uno", "Dos", "Tres")
```

### More Advanced Examples

This is all pretty neat, but it's the more complex scenarios that show the power of what you can do with Reactify:

#### Complex Function with Conditional

```scala
val v1 = Var(10)
val v2 = Var("Yes")
val v3 = Var("No")
val complex = Val[String] {
    if (v1 > 10) {
      v2
    } else {
      v3
    }
}
```

Any changes to `v1`, `v2`, or `v3` will fire a change on `complex` and the entire inlined function will be re-evaluated.

#### Multi-Level Observation

A much more advanced scenario is when you have a `Var` that contains a `class` that has a `Var` and you want to keep track
of the resulting value no matter what the first `Var`'s instance is currently set to.

Consider the following two classes:

```scala
class Foo {
  val active: Var[Boolean] = Var(false)
}
class Bar {
  val foo: Var[Option[Foo]] = Var[Option[Foo]](None)
}
```

A `Bar` has a `Var` `foo` that holds an `Option[Foo]`. Now, say I have a `Var[Option[Bar]]`:

```scala
val bar: Var[Option[Bar]] = Var[Option[Bar]](None)
```

If we want to determine `active` on `Foo` we have several layers of mutability between the optional `bar` `Var`, the
optional `foo` `Var`, and then the ultimate `active` `Var` in `Foo`. For a one-off we could do something like:

```scala
val active: Boolean = bar().flatMap(_.foo().map(_.active())).getOrElse(false)
```

This would give us `true` only if there is a `Bar`, `Bar` has a `Foo`, and `active` is true. But, if we want to listen
for when that changes at any level (`Bar`, `Foo`, and `active`) it should be just as easy. Fortunately with Reactify it
is:

```scala
val active: Val[Boolean] = Val(bar().flatMap(_.foo().map(_.active())).getOrElse(false))
```

Yep, it's that easy. Now if I set `bar` to `Some(new Bar)` then `foo := Some(new Foo)` on that, and finally set `active`
to true on `Foo` my `active` `Val` will fire that it has changed. Reactify will monitor every level of the `Var`s and
automatically update itself and fire when the resulting value from the function above has changed.

```scala
// Monitor the value change
active.attach { b =>
  ... do something ...    
}

// Set Bar
val b = new Bar
bar := Some(b)

// Set Foo
val f = new Foo
b.foo := Some(f)

// Set active
f.active := true
```

With Reactify you don't have to do any magic in your code, you just write Scala code the way you always have and let
Reactify handle the magic.

### Channels

As we saw above, `Var` and `Val` retain the state of the value assigned to them. A `Channel` on the other hand is like a
`Var` (in that you can set values to it), but no state is retained. This is useful for representing firing of events or
some other action that is meant to be observed but not stored.

## Nifty Features

### Dependency Variables

The core functionality is complete and useful, but we can build upon it for numeric values that are dependent on other
numeric values or numeric values that may have multiple representations. For example, consider a graphical element on
screen. It may have a `left` position for the X value originating on the left side of the element, but if we want to
right-align something we have to make sure we account for the width in doing so and vice-versa for determining the right
edge. We can simplify things by leveraging a `Dep` instance to represent it:

```
val width: Var[Double] = Var(0.0)

val left: Var[Double] = Var(0.0)
val center: Dep[Double, Double] = Dep(left, width / 2.0)
val right: Dep[Double, Double] = Dep(left, width)
```

Notice we've even added a `center` representation. These are dependent on `left` but their value is derived from a
formula based on `left` and `width`. Of course, if representing the value alone were all we care about then a simple
`Val(left + width)` could be used as our `right` value, but we also want to be able to modify `center` or `right` and
have it properly reflect in `left`. Any changes made to `Dep` will properly update the variable it depends on `left` in
this case. See `DepsSpec` for more detailed examples.

`Dep` also supports conversions between different types as well, but must have an implicit `DepConnector` available to
handle the conversions.

### Binding

As of 1.6 you can now do two-way binding incredibly easily:

```scala
val a = Var[String]("Hello")
val b = Var[String]("World")
val binding = a bind b
```

By default this will assign the value of `a` to `b` and then changes to either will propagate to the other. If you want
to detach the binding:

```scala
binding.detach()
```

This will disconnect the two to operate independently of each other again.

You can also do different typed binding:

```scala
implicit val s2i: String => Int = (s: String) => Integer.parseInt(s)
implicit val i2s: Int => String = (i: Int) => i.toString

val a = Var[String]("5")
val b = Var[Int](10)
a bind b
```

We need implicits to be able to convert between the two, but now changes to one will propagate to the other.

## Versions

### Features for 2.2.0 (Released 2017.09.10)

* [X] Refactoring of Listener to Observer for better naming convention (breaking changes)
* [X] Code cleanup

### Features for 2.1.0 (Released 2017.07.28)

* [X] Features for mapping from one Observable to another
    * [X] map
    * [X] collect
* [X] Cross-build with Scala 2.13.0

### Features for 2.0.0 (Released 2017.06.15)

* [X] Complete re-write of internal and recursive state system for better modularity and stability
* [X] Observable builder
* [X] Observable from Future
* [X] RecursionMode to better clarify how hierarchical recursion should work
    * [X] Static mode for function-less properties
    * [X] None mode for functional but non-recursive
    * [X] RetainPreviousValue for single-level recursion (new default for performance)
    * [X] Full for complete multi-level recursion
* [X] Transaction system
* [X] Val.dirty and Var.dirty for on-update re-validation of functional reactive properties for performance intensive functionality
* [X] Better recursive integration
* [X] Better cyclical detection

### Features for 1.6.0 (Released 2017.06.01)

* [X] Binding support
    * [X] Different type binding

### Features for 1.5.0 (Released 2017.04.12)

* [X] Stop Propagation feature
* [X] Listener
* [X] Prioritization support
* [X] Support for chaining Observables to a single listener

### Features for 1.4.0 (Released 2017.02.26)

* [X] Complete reworking of internals to support more advanced usage
* [X] Removal of Macros as they were creating serious limitations
* [X] Simplified package naming
* [X] Introduced ultra light-weight `Prop` as a simpler bare minimum alternative to `Val`
* [X] Better cleanup and re-introduction of `StateChannel`, `State`, and `Channel` as better interface representations.

### Features for 1.3.0 (Released 2017.01.09)

* [X] Utilization of Macros instead of runtime Observable detection

### Features for 1.2.0 (Released 2016.12.27)

* [X] Syntactic sugar for dealing with collections in a `StateChannel`.
* [X] `Observable.changes` to conveniently see old and new values on change.
* [X] Convenience mutation `mod` method in `StateChannel`.

### Features for 1.1.0 (Released 2016.12.18)

* [X] Dep for dependency representation

### Features for 1.0.0 (Released 2016.11.15)

* [X] Channel, Val, and Var functionality
* [X] Observable functionality
* [X] Convenience implicits to convert from `Val` and `Var` to the value
* [X] Support for value-defined Observable dependencies (Observable State classes used in the makeup of variables are monitored for changes)