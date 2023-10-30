# OntologyBundle Gradle Plugin

## Introduction

Ontology Bundle is a Gradle plugin that packages (bundles) an OWL ontology,
possibly spanning many files, into a form that makes it easy to use in a
Java-based software project. More specifically, a Gradle project that uses
this plugin takes as input a collection of RDF files (in any RDF
serialization format) containing an ontology, together with some
configuration parameters specified in the Gradle build script, and produces a
Java jar containing the following outputs:

- A single Java resource containing the union of all the ontology input
files. This is called the "human-readable ontology file."
- A second Java resource containing a processed version of the
human-readable ontology file, called the "machine-readable ontology file."
(See below for details.)
- For each RDF prefix, Java class file that contains static constants for
the terms in the associated vocabulary. For example, if your ontology defines
a class foo:MyClass, your Java code can refer to that class as Foo.MyClass
with no possibility of a misspelling of the class IRI. (See below for
details.)
- A small Java class, called `OntAccess`, with convenience methods for loading
either ontology resource. The resources may be accessed either as an
InputStream or as an in-memory Jena Model. (This Model is non-inferencing,
but the caller can easily wrap it in an inferencing Model if they wish.)
- Tests that verify the merged ontology files.

## Example Gradle Build Script

This is a minimal example `build.gradle` (written in  Groovy) to bundle an ontology:

```
plugins {
	id 'com.bbn.parliament.ontology-bundle'
}

group = 'com.my_company'
version = '1.0.0'

ontologyBundle {
	prefixes = [
		// prefix, class (blank for none), namespace
		'owl, , http://www.w3.org/2002/07/owl#',
		'rdf, , http://www.w3.org/1999/02/22-rdf-syntax-ns#',
		'rdfs, , http://www.w3.org/2000/01/rdf-schema#',
		'xsd, , http://www.w3.org/2001/XMLSchema#',
		'myont, MyOnt, http://my_company.com/my-ontology#',
	]
	ontologySources = fileTree(dir: "$projectDir/ontology-files",
		includes: [ '**/*.ttl', '**/*.rdf', '**/*.owl' ],
		exclude: '**/*-experimental*'
	)
	ontologyIri = 'http://my_company.com/my-ontology'
	generatedCodePackageName = 'com.my_company.my_ontology'
}
```

## Preparation of the Human-Readable Ontology

The human-readable ontology file is the result of merging the various input
ontology files together, with a few changes to make things tidier:

- The individual ontology declarations from the constituent ontologies are
replaced with a single declaration representing the combined ontology.
- White space is stripped from the start and end of string literals, and
statements with empty literals are deleted.
- Explicit declarations that a class is a subclass of owl:Thing are
deleted.
- Property restrictions that are not connected to another class or that are
missing their value or cardinality constraint are deleted.
- The human-readable ontology file is serialized with the most friendly
formatting options to make it as readable as possible.

## Preparation of the Machine-Readable Ontology

The machine-readable ontology file applies two additional changes to the human-readable file to prepare it to be used in a running software system.

First, values for a variety of annotation properties whose purpose is
documentation are deleted, since these are rarely used at run time. Note that
the declarations of these properties are not removed — only statements using
these properties to document the ontology elements are deleted. Also,
rdfs:label, skos:altLabel, and skos:prefLabel values are preserved, since
human-readable names for ontology elements are useful at run-time.

Second, and most importantly, the Ontology Bundle plugin attempts to replace
all blank nodes (such as property restrictions and RDF list nodes) with IRI
nodes. The goal is to ensure that if the ontology is inserted into a semantic
graph store multiple times (perhaps with minor changes), then blank nodes are
not duplicated. For instance, if an ontology containing a property
restriction blank node is inserted twice, then there will be two property
restriction in the database, which slows down inference. If an ontology is
re-inserted again and again over a period of time, this load on the inference
engine can grow prohibitively large.

The key to replacing the blank nodes with IRIs is to ensure that upon each
replacement, the same IRI is used. To do this, Ontology Bundle forms the IRI
from the cryptographic hash of the identifying property values of the
original blank node. For instance, for a property restriction the value of
the owl:onProperty statement, the value or cardinality constraint, and the
predicate used in the constraint are combined and hashed, and then a
namespace is prepended to form a new IRI that replaces the blank node. Two
additional notes about this replacement:

- The RDF prefix "fill:" is added to the machine-readable ontology with the
namespace used for the IRIs that replace the blank nodes.
- The SPARQL Update statements used to replace the blank nodes are a bit
touchy in the sense that it is not difficult to create an ontology for which
they will not work. If after running Ontology Bundle on your ontology blank
nodes remain, please file a bug report ***that includes your ontology or a
snippet of it that reproduces the problem.***

## Generation of the Namespace Classes

The Ontology Bundle plugin uses Jena's SchemaGen tool
([https://jena.apache.org/documentation/tools/schemagen.html](https://jena.apache.org/documentation/tools/schemagen.html))
to create a Java namespace class for each declared namespace whose class name
is not blank in the prefixes declaration in the Gradle build script. This tool
creates a Java class containing every declared OWL class, property, datatype,
and instance in the associated namespace. These classes can be used to
eliminate a particularly pernicious type of coding error, namely misspellings
of IRIs in the Java code. Furthermore, if you use these classes everywhere in
place of hard-coded IRIs and then change an IRI, the Java compiler will show
you exactly where you need to make corresponding changes in the code.

## Tests

OntologyBundle has been tested against the ontologies below, which comprise a useful set
of widely used actual and de facto standards. These include all of the ontologies listed
on https://www.w3.org/wiki/Good_Ontologies.

- Basic Formal Ontology (BFO)
- Creative Commons
- Dolce Ultra-Lite (DUL)
- Dublin Core
- Friend Of A Friend (FOAF)
- GeoSPARQL
- Good Relations
- MarineTLO
- Music Ontology
- OWL Time
- PROV-O
- Quantities, Units, Dimensions, and Types (QUDT)
- schema.org
- Semantically-Interlinked Online Communities (SIOC)
- Simple Knowledge Organization System (SKOS)
- Semantic Sensor Network (SSN)
- Sensor, Observation, Sample, and Actuator (SOSA)

## Ontologies to Consider Adding to Tests

- Common Core
- 'ao, , http://purl.org/ontology/ao/core#',
- 'bio, , http://purl.org/vocab/bio/0.1/',
- 'cc, , http://web.resource.org/cc/',	# related to creative commons?
- 'dcat, , http://www.w3.org/ns/dcat#',
- 'dcmitype, , http://purl.org/dc/dcmitype/',
- 'dtype, , http://www.linkedmodel.org/schema/dtype#',
- 'event, , http://purl.org/NET/c4dm/event.owl#',
- 'frbr, , http://purl.org/vocab/frbr/core#',
- 'geo, , http://www.w3.org/2003/01/geo/wgs84_pos#',
- 'keys, , http://purl.org/NET/c4dm/keys.owl#',
- 'mc, , http://www.linkedmodel.org/owl/schema/core#',
- 'nist, , http://physics.nist.gov/cuu/',
- 'oecc, , http://www.oegov.org/models/common/cc#',
- 'org, , http://www.w3.org/ns/org#',
- 'sh, , http://www.w3.org/ns/shacl#',
- 'vaem, , http://www.linkedmodel.org/schema/vaem#',
- 'vann, , http://purl.org/vocab/vann/',
- 'voaf, , http://purl.org/vocommons/voaf#',
- 'voag, , http://voag.linkedmodel.org/schema/voag#',
- 'void, , http://rdfs.org/ns/void#',
- 'vs, , http://www.w3.org/2003/06/sw-vocab-status/ns#',
- 'wot, , http://xmlns.com/wot/0.1/',

## TODO

- Add publication to https://plugins.gradle.org to the gradle script.
