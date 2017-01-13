# reactify

[![Build Status](https://travis-ci.org/outr/reactify.svg?branch=master)](https://travis-ci.org/outr/reactify)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/759324d19db5496dbd9867b4a113c806)](https://www.codacy.com/app/matthicks/reactify?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=outr/reactify&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/759324d19db5496dbd9867b4a113c806)](https://www.codacy.com/app/matthicks/reactify?utm_source=github.com&utm_medium=referral&utm_content=outr/reactify&utm_campaign=Badge_Coverage)
[![Stories in Ready](https://badge.waffle.io/outr/reactify.png?label=ready&title=Ready)](https://waffle.io/outr/reactify)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/reactify)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.outr/reactify_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.outr/reactify_2.12)
[![Latest version](https://index.scala-lang.org/outr/reactify/reactify/latest.svg)](https://index.scala-lang.org/outr/reactify)

Functional Reactive Properties for observing changes and retaining values

## Setup

reactify is published to Sonatype OSS and Maven Central currently supporting Scala and Scala.js on 2.11 and 2.12.

Configuring the dependency in SBT simply requires:

```
libraryDependencies += "com.outr" %% "reactify" % "1.3.4"
```

or for Scala.js or cross-building:

```
libraryDependencies += "com.outr" %%% "reactify" % "1.3.4"
```

## Concepts

This framework is intentionally meant to be a simplistic take on properties and functional reactive concepts. There are
only four specific classes that really need be understood to take advantage of the framework:

- Observable - As the name suggests it is a simple trait that fires values and may be observed by listeners.
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
import com.outr.reactify._
```

### Creating Props

As discussed in the concepts there are only four major classes in Props (`Observable`, `Channel`, `Val`, and `Var`). Of
those classes, unless you are creating a custom `Observable` you will probably only deal with the latter three.

Creating instances is incredibly simple:

```
val myChannel = Channel[String]()         // Creates a Channel that receives Strings
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
new value of `15` (`myVar + 5`). As a result, the listener we attached above will output:

```
myVal = 15
```

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

## Versions

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