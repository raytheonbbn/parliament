# QueryBuilder

A small Java library for manipulating SPARQL queries at run time,
using Jena version 3.

Assembles and updates parameterized query snippets. Since the internal
implementation relies on Jena's Query class, this only supports
manipulating SELECT, ASK, DESCRIBE, and CONSTRUCT queries, i.e.,
SPARQL-update is not supported.

Adapted from code written by Matt Allen.

Copyright (c) 2019, 2020 Raytheon BBN Technologies Corp.
