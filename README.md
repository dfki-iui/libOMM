# Java Object Memory Library
Object Memories allow physical artefacts to keep a history of their statuses, interactions and changes over a lifetime. With the growing availability of easy-to-deploy and cheap data storages such as RFID or NFC chips such a refinement of products has become comparatively affordable and possible applications are starting to arise. Whether a product's temperature is to be monitored, yielded emission, changing components or manipulations issued by other artefacts, in an object memory all relevant data can be stored and accessed at will. 

---

1 [About object memories](#ch1)

2 [How to use this code](#ch2)

3 [How to use the OMS](#ch3)

---

## 1 <a name="ch1"> About object memories </a>
The Object Memories follow the guidelines laid down by the [W3C Object Memory Modeling Incubator Group]( http://www.w3.org/2005/Incubator/omm/). 

An object memory is a digital data repository linked to a physical artefact. The exact means of the linking between memory and artefact are not preassigned, an object might hold its own data on a storage unit or carry an identifier that can be used to access the memory externally. A memory can be accessed and changed by different agents, such as humans, machines or other artefacts that might use or process it in any way. All changes made to the physical object are represented digitally and can be logged over time. 

The memory format is XML based. However, through this library a memory can be accessed as a Java object. This allows an object memory server application to load all memories known to it from their XML representation on start-up and then provide them via various means, such as a web application or a RESTful interface. Regardless of the access method this library allows to create, delete and manipulate memory data, such as adding new information or overwriting old values. 

## 2 <a name="ch2"> How to use this code </a>
This repository contains all necessary Java source files for working object memories. It is advised to implement a server or similar application to access these memories, such as the [Object Memoty Server (OMS)](https://github.com/dfki-iui/oms) which is available for free download.

In **libomm** you will find classes modelling object memories and their contents as defined in the Incubator Group's [Final Report](http://www.w3.org/2005/Incubator/omm/XGR-omm-20111026/). These are imported in many classes of the **oms** project which, in turn, provides classes to run and access the Object Memory Server.

### 2.1 Requirements
Apart from the Java JDK Version 8 Update 45 or higher (downloadable at http://java.com/) the project depends on some external libraries to run. It utilises Maven, so everything necessary can be found in the POM files. However, the following table provides an overview of all libraries needed to use the object memory project.

| Library | Version| 
| :------- | :------: |
| [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/download_cli.cgi) | 1.2 |
| [Apache Commons Codec]( https://commons.apache.org/proper/commons-codec/download_codec.cgi) | 20041127.091804|
| [Apache Commons IO](https://commons.apache.org/proper/commons-io/download_io.cgi) | 2.4 |
| [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/download_lang.cgi) | 3.3.2|
| [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/download_validator.cgi) | 1.4.0|
| [Apache Log4j]( https://logging.apache.org/log4j/1.2/download.html) | 1.2.17|
| [Apache Santuario](http://santuario.apache.org/download.html) | 2.0.3|
| [Eclipse Jetty](http://download.eclipse.org/jetty/) | 9.3.0.M2|
| [ez-vcard](https://code.google.com/p/ez-vcard/wiki/Downloads) | 0.9.6 |
| [Jackson Core ASL](http://grepcode.com/snapshot/repo1.maven.org/maven2/org.codehaus.jackson/jackson-core-asl/1.9.13) | 1.9.13 |
| [Jackson Mapper ASL](http://grepcode.com/snapshot/repo1.maven.org/maven2/org.codehaus.jackson/jackson-mapper-asl/1.9.13) | 1.9.13 |
| [Jetty ALPN](https://github.com/jetty-project/jetty-alpn) | 8.1.3.v20150130 |
| [JSON](https://github.com/douglascrockford/JSON-java) | 20141113 |
| [JSoup](http://jsoup.org/download) | 1.7.3 |
| [JUnit]( https://github.com/junit-team/junit) | 4.12 |
| [Restlet]( http://restlet.com/downloads/current/) | 3.0-M1 |
| [SLF4J](http://www.slf4j.org/download.html) | 1.7.10 |
Make sure to download and import all necessary libraries manually if you are not using Maven to handle dependencies.

In order to fully capitalise on these libraries, however, you will most likely need to write your own clients and other applications in a language and environment of your choice to map your real world circumstances to digital memories and vice versa, thus creating an elaborate cyberphysical environment.  

## 3 <a name="ch3">How to use libOMM</a>

### 3.1 Memory creation and setup

A new RESTful object memory is created by calling a creation method of `de.dfki.omm.impl.OMMFactory`: 
```java
OMMFactory.createOMMViaOMSRestInterface(url, header, owner);
``` 
The given header and owner decide how the memory can be accessed and will be included into the memory’s info file on the hard disk. To clarify the parameters:

* `url` is a String containing the address of a memory server, extended to the node responsible for memory creation, such as: http://localhost:10082/mgmt/createMemory if the OMS is running on a local machine using the standard port. 

* `header` is an object implementing the interface de.dfki.omm.interfaces.OMMHeader in which all necessary information about the object memory is stored, with at least the memory’s ID (its RESTful address on the server). An example header might be created like this: 
```java
URLType headerID = new URLType (new URL ("http://localhost:10082/rest/mymemory"));
OMMHeaderImpl header = (OMMHeaderImpl) OMMHeaderImpl.create(headerID, null);
```

* `owner` is a memory block containing information about the memory’s owner (who is equipped with certain access rights). Such an owner block might be created as follows:
```java
String owner = OMMFactory.createOMMOwnerStringFromUsernamePassword (“owner’s clear name”, “owner’s user name”, “owner’s password”); 
OMMBlock ownerBlock = OMMFactory.createOMMOwnerBlock (header, ownerString);
```



### 3.2 Accessing an object memory 

In order to access a memory via Java code there needs to be a representation of the object memory to access. This can be created by calling `new OMMRestImpl(url, mode)`, with the parameters:
* `url`, a String which contains a RESTful address, such as http://localhost:10082/rest/memoryName, where the memory name may vary and maps to the memory’s address on the server. 
* `mode` is a `de.dfki.omm.types.OMMRestAccessMode` and can take one of three values: `CompleteDownloadLimitedLifetime`, `CompleteDownloadUnlimited` and `SingleAccess`. 


## 3.3 Handling memory blocks

A memory consists of smaller units, called blocks. In Java, these blocks all implement the interface OMMBlock and can be added, manipulated and deleted using the OMMRestImpl as created in the preceding chapter. 

### 3.3.1 Creating a block

To create a simple memory block the create-method in `de.dfki.omm.impl.OMMBlockImpl` can be used. All necessary data is given as parameters at creation, although its fields can be changed later. Therefore, the method’s signature is rather long: 
```java
OMMBlockImpl block = (OMMBlockImpl) OMMBlockImpl.create (id, primaryID, namespace, type, title, description, contributors, creator, format, subject, payload, payloadElement, link, linkHash);
```

* `id` is a String containing the block’s ID, likely a unique alphanumeric identifier.  
* `primaryID` is an object which implements the `de.dfki.omm.types.TypedValue` interface in order to represent the ID of the object memory containing this block. The block’s and the memory’s id combined form a unique address. 
* `namespace` is an URI of the namespace to which this block is assigned, defining format and contents of this block.
* `type` represents the block’s Dublin Core based type as an URL (for example "http://purl.org/dc/dcmitype/Dataset").
* `title` is a `de.dfki.omm.types.OMMMultiLangText`, providing a different title string for different languages. This allows special treatment depending on localization. 
* `description` is a `de.dfki.omm.types.OMMMultiLangText`, providing a different description string for different languages. This allows special treatment depending on localization.
* `contributors` is a `de.dfki.omm.types.OMMEntityCollection` which contains all contributors to this block and the time of their contribution (each one represented as `de.dfki.omm.types.OMMEntity`). 
* `creator` represents the first contributor and the time of its creation as an `OMMEntity`. 
* `format` determines the block’s format as a `de.dfki.omm.types.OMMFormat`. 
* `subject` is a `de.dfki.omm.types.OMMSubjectCollection` which further classifies this block’s contents as part of an ontology.
* `payload` provides the data stored in this block as a TypedValue. 
* `payloadElement` provides the data stored in this block as an Element. This can be useful when reading blocks or their contents directly from XML.
* `link` is a TypedValue providing the possibility of storing a data externally, for example on a server. Instead of an actual payload the block then holds a link to its contents. 
* `linkHash` is a String containing a link’s hash. It is only necessary if the block is a linked block without local payload. 

Most of these parameters are optional and can thus be null, as it is possible to change them later. Exceptions are id, title, and creator (although OMMEntity provides a method to create a dummy entity if needed). Also, namespace and format cannot both be null at the same time. 

### 3.3.2 Adding a block to a memory

Once created, the block can be added to a given OMMRestImpl by calling 
```java
OMMRestImpl.addBlock(block, addingEntity);
```
where block is the newly created block to add and addingEntity is the OMMEntity who is contributing this block. The call returns a `de.dfki.omm.tools.OMMActionResultType` which can be either OK (block was successfully added) or one of four error codes: `BlockNotExistent`, `BlockWithSameIDExists`, `Forbidden` and `UnknownError`. 
From now on the block resides in the memory until it is somehow manipulated or deleted. 

### 3.3.3 Manipulating a block

To (re)gain access to a specific block, it can be requested from the OMMRestImpl. Calling 
```java
OMMRestImpl.getAllBlocks();
```
returns a `Collection<OMMBlock>` containing all blocks of this memory, while 
```java
OMMRestImpl.getAllBlockIDs();
```
will yield a `List<String>` with the IDs of all the blocks. The latter can be useful when trying to receive one block, as 
```java
OMMRestImpl.getBlock(blockID)
``` 
requires a valid blockID as a String as parameter. 

Each object implementing the interface OMMBlock will grant access to its fields as described above. Reading access is implemented for all of them. Except for mandatory data, each one can also easily be overwritten with new data or removed completely. 

### 3.3.4 Deleting a block

Analogous to its addition to a memory, a simple block can also be removed. This can be done by providing an `OMMBlock` instance and calling 
```java
OMMRestImpl.removeBlock(block, removingEntity);
```
or by providing the block ID as string by calling
```java
OMMRestImpl.removeBlock(blockID, removingEntity);
```

While the former needs to know the specific block to delete from the OMM, the latter only provides a String of its ID. Both methods return an `OMMActionResultType`, which can take the same values as when adding a new block, depending on the success of the deletion.

## 3.4 Memory deletion

Unless its deletion is disabled a memory can be deleted, removing all its contents in the process. Just like object memory creation this is achieved by calling a method of `OMMFactory`, namely `OMMFactory.deleteOMMRestFromOMS(…)` which is overloaded to support various parameters. 

* ```deleteOMMRestFromOMS(url)``` receives an `URL` holding the memory’s RESTful address, returns true if the deletion was successful, false otherwise.
* ```deleteOMMRestFromOMS(omm)``` directly receives the `OMMRestImpl` to delete, returns null if the deletion was successful, omm otherwise. This can thus be used to overwrite an existing reference to a OMM.

Although after its deletion the memory’s contents will be lost, its reference may still be functional. It is therefore advised to use the second variant if operating with object memory references, as a deleted memory can be overwritten in the process:

```java
omm = OMMFactory.deleteOMMRestFromOMS(omm);
```

