[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![DOI](https://zenodo.org/badge/174502624.svg)](https://zenodo.org/badge/latestdoi/174502624)

![Gradle build](https://github.com/mipt-npm/visionforge/workflows/Gradle%20build/badge.svg)

[![Slack](https://img.shields.io/badge/slack-channel-green?logo=slack)](https://kotlinlang.slack.com/archives/CEXV2QWNM)

# DataForge Visualization Platform

## Table of Contents

* [Introduction](#introduction)
* [Requirements](#requirements)
* [Features](#features)
* [About DataForge](#about-dataforge)
* [Modules contained in this repository](#modules-contained-in-this-repository)
  * [visionforge-core](#visionforge-core)
  * [visionforge-solid](#visionforge-solid)
  * [visionforge-gdml](#visionforge-gdml)
* [Visualization for External Systems](#visualization-for-external-systems)  
* [Demonstrations](#demonstrations)
  * [Simple Example - Spatial Showcase](#simple-example---spatial-showcase)
  * [Full-Stack Application Example - Muon Monitor](#full-stack-application-example---muon-monitor-visualization)
  * [GDML Example](#gdml-example)


## Introduction

This repository contains a [DataForge](#about-dataforge)\-based framework 
used for visualization in various scientific applications. 

The main framework's use case for now is 3D visualization for particle physics experiments. 
Other applications including 2D plots are planned for the future.

The project is developed as a [Kotlin multiplatform](https://kotlinlang.org/docs/reference/multiplatform.html) 
application, currently targeting browser JavaScript and JVM.

## Requirements

JVM backend requires JDK 11 or later

## Features

The main framework's features for now include:
- 3D visualization of complex experimental set-ups
- Event display such as particle tracks, etc.
- Scales up to few hundred thousands of elements
- Camera move, rotate, zoom-in and zoom-out
- Scene graph as an object tree with property editor
- Settings export and import
- Multiple platform support

## About DataForge

DataForge is a software framework for automated scientific data processing. DataForge Visualization
Platform uses some of the concepts and modules of DataForge, including: `Meta`, `Configuration`, `Context`,
`Provider`, and some others.

To learn more about DataForge, please consult the following URLs:
 * [Kotlin multiplatform implementation of DataForge](https://github.com/mipt-npm/dataforge-core)  
 * [DataForge documentation](http://npm.mipt.ru/dataforge/) 
 * [Original implementation of DataForge](https://bitbucket.org/Altavir/dataforge/src/default/)


## Modules contained in this repository

### visionforge-core

Contains a general hierarchy of classes and interfaces useful for visualization. 
This module is not specific to 3D-visualization.

The `visionforge-core` module also includes configuration editors for JS (in `jsMain`) and JVM (in `jvmMain`).

**Class diagram:**

![](docs/images/class-diag-core.png)


### visionforge-solid

Includes common classes and serializers for 3D visualization, as well as Three.js and JavaFX implementations.

**Class diagram:**

![](docs/images/class-diag-solid.png)

##### Prototypes

One of the important features of the framework is support for 3D object prototypes (sometimes
also referred to as templates). The idea is that prototype geometry can be rendered once and reused 
for multiple objects. This helps to significantly decrease memory usage.

The `prototypes` property tree is defined in `SolidGroup` class via `PrototypeHolder` interface, and 
`SolidReference` class helps to reuse a template object.

##### Styles

`SolidGroup` has a `styleSheet` property that can optionally define styles at the Group's 
level. Styles are applied to child (descendant) objects using `Vision.styles: List<String>` property. 


### visionforge-gdml

GDML bindings for 3D visualization (to be moved to gdml project).


## Visualization for External Systems 

The `visionforge` framework can be used to visualize geometry and events from external,
non-Kotlin based systems, such as ROOT. This will require a plugin to convert data model
of the external system to that of `visionforge`. Performing such integration is a work
currently in progress.
 

## Demonstrations

The `demo` module contains several example projects (demonstrations) of using the `visionforge` framework.
They are briefly described in this section, for more details please consult the corresponding per-project
README file.

### Simple Example - Solid Showcase

Contains a simple demonstration with a grid including a few shapes that you can rotate, move camera, and so on.
Some shapes will also periodically change their color and visibility.

[More details](demo/solid-showcase/README.md)

**Example view:**

![](docs/images/spatial-showcase.png)


### Full-Stack Application Example - Muon Monitor Visualization

A full-stack application example, showing the 
[Muon Monitor](http://npm.mipt.ru/en/projects/physics#mounMonitor) experiment set-up.

[More details](demo/muon-monitor/README.md)

**Example view:**

![](docs/images/muon-monitor.png)


### GDML Example

Visualization example for geometry defined as GDML file. 

[More details](demo/gdml/README.md)

##### Example view:

![](docs/images/gdml-demo.png)


## Thanks and references
The original three.js bindings were made by [Lars Ivar Hatledal](https://github.com/markaren), but the project is discontinued right now.

All other libraries are explicitly shown as dependencies. We would like to express specific thanks to JetBrains Kotlin-JS team for consulting us during the work.
