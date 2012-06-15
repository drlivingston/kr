# Knowledge Representation and Reasoning Tools

## Overview

The KR library is for working with knowledge representations and knowledge bases.  Currently it facilitates use of RDF-based representations backed by triple-/quad- stores.  It provides a consistent clojure based way of interacting with its backing implementations, which currently include the Jena and Sesame APIs.


## Basic Setup

The primary api functions you're likely to use come from the kr-core apis:
```clj
(use 'edu.ucdenver.ccp.kr.kb)
(use 'edu.ucdenver.ccp.kr.rdf)
(use 'edu.ucdenver.ccp.kr.sparql)
```

To actually get a KB instance to work with you'll need to make sure the implementation-specific code is loaded:
```clj
(require 'edu.ucdenver.ccp.kr.sesame.kb)
;; OR
(require 'edu.ucdenver.ccp.kr.jena.kb)
```

a kb instance can then be acquired with the kb function, for example:
```clj
(kb :sesame-mem)  ; an in-memory sesame kb
```
The `kb` function can take keyword arguments such as `:sesame-mem` or `:jena-mem` or it can take names of several native jena or sesame objects or pre-constructed jena or sesame instances to create a `kb` wrapper around (e.g., a jena `Model` or a sesame `Sail`).
 

## Basic Use

Once you have a KB you can load rdf triple or files:


Query for RDF triples:


Query with triple patterns (SPARQL):


## More Details

The examples also provide details on how to interact with a KB (to appear).

More detailed uses can be found in the test cases for both the KB, RDF, and SPARQL APIs.  They are here:
https://github.com/drlivingston/kr/tree/master/kr-core/src/test/clojure/edu/ucdenver/ccp/test/kr


## Maven

releases are deployed to clojars:
```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

the core dependency is kr-core:
```xml
<dependency>
  <groupId>edu.ucdenver.ccp</groupId>
  <artifactId>kr-core</artifactId>
  <version>1.4.0</version>
</dependency>
```

but the core dependency is unnecessary if you are brining in either the sesame or jena implementations:
```xml
<dependency>
  <groupId>edu.ucdenver.ccp</groupId>
  <artifactId>kr-sesame-core</artifactId>
  <version>1.4.0</version>
</dependency>

<dependency>
  <groupId>edu.ucdenver.ccp</groupId>
  <artifactId>kr-jena-core</artifactId>
  <version>1.4.0</version>
</dependency>
```


## Acknowledgements
open sourced by: <br />
[CCP Lab][] <br />
[University of Colorado Denver][] <br />
primary developer: [Kevin Livingston][]

----


[CCP Lab]: http://compbio.ucdenver.edu/Hunter_lab/CCP_website/index.html
[University of Colorado Denver]: http://www.ucdenver.edu/
[Kevin Livingston]: https://github.com/drlivingston