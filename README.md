# props
============

[![Build Status](https://travis-ci.org/outr/props.svg?branch=master)](https://travis-ci.org/outr/props)
[![Stories in Ready](https://badge.waffle.io/outr/props.png?label=ready&title=Ready)](https://waffle.io/outr/props)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/outr/props)
[![Maven Central](https://img.shields.io/maven-central/v/com.outr/props_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.outr/props_2.12)
[![Latest version](https://index.scala-lang.org/com.outr/props/props/latest.svg)](https://index.scala-lang.org/com.outr/props/props)

Functional Reactive Properties for observing changes and retaining values

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