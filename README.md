# Clojure API for RDF and SPARQL

The Knowledge Representation and Reasoning Tools library enables easy Clojure use of RDF and SPARQL, provinging a unified interface for both Jena and Sesame.  (KR can be extended for other APIs and underlying triplestores.)


## Overview

Currently it facilitates use of RDF-based representations backed by triple-/quad- stores.  It provides a consistent clojure based way of interacting with its backing implementations, which currently include the Jena and Sesame APIs. The library enables easy working with knowledge representations and knowledge bases, and provides support for some common tasks including forward-chaining and reification.

[Release Notes]

update: see the note on [Sesame Versions]


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

kb's need some help knowing what the namespace mappings are, the server mappings can be brought down from a third party kb by calling `(synch-ns-mappings my-kb)` or you can add a few:
```clj
(register-namespaces my-kb
                     '(("ex" "http://www.example.org/") 
                       ("rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                       ("foaf" "http://xmlns.com/foaf/0.1/")))
;;the return value is the new modified kb - hang onto it
```

## Basic Use

Once you have a KB you can load rdf triple or files:
```clj
  ;;in parts
  (add my-kb 'ex/KevinL 'rdf/type 'ex/Person)
  ;;as a triple
  (add my-kb '(ex/KevinL foaf/name "Kevin Livingston"))
```

Query for RDF triples:
```clj
(ask-rdf my-kb nil nil 'ex/Person)
;;true

(query-rdf my-kb nil nil 'ex/Person)
;;((ex/KevinL rdf/type ex/Person))
```

Query with triple patterns (SPARQL):
```clj
(query my-kb '((?/person rdf/type ex/Person)
               (?/person foaf/name ?/name)
               (:optional ((?/person foaf/mbox ?/email)))))
;;({?/name "Kevin Livingston", ?/person ex/KevinL})
```

## More Details

The examples also provide details on how to interact with a KB, with run-able poms:
https://github.com/drlivingston/kr/tree/master/kr-examples

These include examples of connecting to a remote repository and a local in-memory repository.


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
  <version>1.4.17</version>
</dependency>
```

but the core dependency is unnecessary if you are brining in either the sesame or jena implementations:
```xml
<dependency>
  <groupId>edu.ucdenver.ccp</groupId>
  <artifactId>kr-sesame-core</artifactId>
  <version>1.4.17</version>
</dependency>

<dependency>
  <groupId>edu.ucdenver.ccp</groupId>
  <artifactId>kr-jena-core</artifactId>
  <version>1.4.17</version>
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
[Sesame Versions]:https://github.com/drlivingston/kr/wiki/versions-and-sesame
[Release Notes]:https://github.com/drlivingston/kr/wiki/Release-notes
