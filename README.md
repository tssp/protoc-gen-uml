# protoc-gen-uml

[![Build Status](https://travis-ci.org/tssp/protoc-gen-uml.svg?branch=master)](https://travis-ci.org/tssp/protoc-gen-uml)

[Protocol buffer compiler plugin](https://developers.google.com/protocol-buffers/docs/reference/other) for UML diagrams. 

When dealing with protocol buffers in your project, it is often quite difficult to get a proper overview - or better the essence of the data model, especially when you have many types.
I am not an UML evangelist, but at least for data models it seems to be a very valuable tool (though there may be other valuable areas).
Loosely based on "a picture is worth than a thousand words" I strongly believe a generated class diagram for your proto messages and enums (services are not yet supported) allows you
and your clients a faster understanding of your model.

The basic idea of protoc-gen-uml is to plugin into `protoc` and generate a new representation that can be understood by different UML tools, such as PlantUML.  

Of course one could generate - let's say - Java classes with protoc and then again generate a class diagram out of the generated classes.
But at least for me it never felt like having a good reflection of the proto model. 

Currently the compiler plugin only supports [PlantUML](http://plantuml.com/) format. Other output formats are planned (e.g. [UMLGraph](http://www.umlgraph.org/) or [DOT](http://graphviz.org/)).

<img src='http://g.gravizo.com/g?
@startuml;

hide empty methods;
hide empty fields;
skinparam classBackgroundColor %23EEEEEE;
skinparam classArrowColor black;
skinparam classBorderColor black;
skinparam packageBackgroundColor white;
skinparam packageBorderColor black;

package io.coding.me.schema.util {;
 class Date {;
  year: Int;
  month: Int;
  day: Int;
 };

};
package io.coding.me.schema.database {;
 class Database {;
  albums: io.coding.me.schema.music.Album [*];
 };
 Database -- io.coding.me.schema.music.Album;

 class Index {;
  albums_by_year: Map<Int,Database>;
 };
 Index -- Database;

};

package io.coding.me.schema.music {;
 class Musician {;
  name: String;
 };

 class Band {;
  name: String;
  members: Musician [*];
 };
 Band -- Musician;

 class Album {;
  name: String;
  genre: Album::Genre;
  release_date: io.coding.me.schema.util.Date;
  interpret: Album::Interpret;
 };
 Album -- Album::Genre;
 Album -- io.coding.me.schema.util.Date;
 Album -- Album::Interpret;

 class Album::Interpret << oneOf >> {;
  band: Band;
  musician: Musician;
 };
 Album::Interpret -- Band;
 Album::Interpret -- Musician;

 enum Album::Genre {;
  ALTERNATIVE_ROCK;
  BLUES;
  METAL;
  INDIE_ROCK;
 };

};

@enduml
'>

## Usage 

```bash

# Checkout latest version from Github
git clone https://github.com/tssp/protoc-gen-uml .

# Package binaries
cd protoc-gen-uml
sbt universal:stage

# Make binaries available in your current path
export PATH=$(pwd)/target/universal/stage/bin:$PATH

# Compile protos
protoc --uml_out=/tmp -I src/test/resources/sample-protos/complex/ src/test/resources/sample-protos/complex/*.proto

# Creates output file in /tmp/
```

The model file itself is not that useful, but converting it to a graphic finally makes it more readable (ensure to have PlantUML installed):

```bash
plantuml /tmp/complete_model.puml
```

## Versions

The plugin is developed and tested with PB version 3. Currently it is not in a state which is releasable.

## Type Transformation

### General

In general messages and enumerations get transformed directly to class and enum entities inside UML. 
Each scalar field type of a message get transformed to its Java counterpart (for the sake of convenience).
All non-scalar field types become message types.

### Nested Types

Nested types and its field are be treated as descriped above, except their type name. The name gets prefixed with its enclosing type's name separated by double colon. 

### OneOf Types

To properly reflect the idea of oneOf-fields, the get transformed to a separate class with a special stereotype. 
The fields that are part of the oneOf-group are moved to the new type. Type name of the new class is a camel-cased variant of the original field name prefixed with the enclosing type as described above.

### Map Types

Maps are treated as simple field types with two parameters.  Even though protoc creates artificial nested types for maps, they are omitted. 

### Multiplicity

TBD

## Plans and Roadmap

* Support for rich documentation (diagrams + written words)
* Make model transformer open for user-defined extensions and options
* Switch from internal if/else output formatting to a more powerful template engine


## Related Projects

* [protoc-gen-doc](https://github.com/estan/protoc-gen-doc) is a very good documentation generator for protocol buffers
* [ScalaPB](https://github.com/trueaccord/ScalaPB) is the execellent Scala code generator for protocol buffers
