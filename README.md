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

### 3.3 Saving an object memory 

The libOMM library provides multiple options to render memory data persistent, each of which fulfils different goals. In the following paragraphs these options are to be expounded and compared. In short, memories can be serialized either using XML or binary format, with the latter being implemented both by native Java and a customly created algorithm.

#### 3.3.1 XML serialization 

XML is a markup language able to express hierarchical information in text format. It is therefore well suited to model an OMM with subordinated blocks. The fact that it contains plain text in a clear and specified structure also makes it human-readable and even allows for alterations directly to the file to change a memory or block – provided the changes satisfy the structure and the unit in question is reloaded afterwards. The XML format is widely used and libraries to process it are available for almost any programming language, though a parser will have to be customized to read the specified OMM structure. However, due to this structure, relying on formatting and redundancy, files in XML format are usually much larger than the actual data they comprise.
The libOMM library provides methods to save and load memories in XML format. 
To save an OMM, the static method `OMMFactory.saveOMMToXmlFile (OMM omm, File xmlFile, boolean withToC)` can be called with an existing OMM and a destination file as parameters, and a Boolean regulating whether or not the Table of Contents is to be saved with the memory. The method returns true if the operation was successful and false if there was an error while attempting to save. 
Likewise, `OMMFactory.loadOMMFromXmlFileString(String xml, URL urlSource, File fileSource, OMMSourceType sourceType)` returns an OMM constructed from the given XML String. An URL source or a File source have to be given where the memory will be stored automatically on further operation, with the source type designating which one should be used.  

#### 3.3.2 Binary serialization 

The Java framework itself offers a method of binary serialization using native mechanisms like writing to an `ObjectOutputStream` or reading from an `ObjectInputStream`.
Since the exact method of serializing and deserializing data is left to the JVM, this method offers great compatibility – different systems or Java versions, can work with serialized files, even refactored classes are handled natively. However, this also makes the format inaccessible for human readers, as the information is mostly binary code. Additionally, in order for the JVM to restore an object completely from its serialized representation, a lot of data has to be written that is not part of its actual content, such as class definition and variable names. Despite being saved in binary format, the resulting files are thus larger than could be expected by just a memory's content. 
In libOMM the native serialization and deserialization of complete OMMs can be issued via `OMMFactory.serializeOMM(OMM omm, File binFile)` and `OMMFactory.deserializeOMM(File binfile)` respectively. The former receives an existing OMM and a file destination to store it, returning true if saving was successful, the latter receives a file from which to recreate an OMM, returning the deserialized OMM on success. 
With its large organizational overhead the native binary format exceeds even XML in some instances. This is particularly apparent for empty or very small structures. Only with increasing content size does this format outperform text-based serialization methods. 

In addition to Java's native serialization another binary storage algorithm has been implemented in libOMM which focuses less on exchangeability than on the size of the resulting files, using for example an additional serialization step in which the information is zipped. As this format cannot easily be recreated without knowing exactly how it is structured, it might not be compatible with earlier versions of serialized data or with other applications (unless those explicitly implement the mechanism themselves). However, when using libOMM alone this is not necessarily required, so that the custom serialization algorithm becomes a valid choice when aiming for small size. 
Like the native algorithm it is called using methods in the OMM factory class. `OMMFactory.saveOMMToBinary(OMM omm, File binFile, boolean compressData)` will save a given OMM to the given file, returning true on success, and compress the data even more via a GZIP stream if desired, resulting in the file ending being completed by a 'z' character. `OMMFactory.loadOMMFromBinary(File binFile)` will restore the saved information, looking for that 'z' character and unzipping if necessary, and return the created OMM. 
The custom binary format outperforms all others in file size. However, it is also among the most complex to implement in other applications or purposes than the existing ones. 

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

### 3.3.4 Saving a block

Like complete memories, single blocks might be saved to a storage medium by various means, each with different strenghts and weaknesses concerning scope, readability by humans, exchangeability and file size. Blocks can be serialized using XML, JSON and both the native and custom binary serialization strategy. 

#### 3.3.4.1 XML serialization

Saving and laoding a single block in XML works similar to performing these operations with a complete OMM, with `OMMFactory.saveBlockToXML(OMMBlock block, File targetFile)` writing the block to a given destination, returning true on success, and `OMMFactory.loadBlockFromXML(File targetFile)` reading an XML String from a file and converting it to the resulting OMMBlock. 

#### 3.3.4.2 JSON serialization

JSON is a compact text-based file format originally meant for data exchange between applications. It can thus be used easily for serialization purposes. A valid JSON document is a valid JavaScript at the same time and can be evaluated as such, giving a fixed structure to the format, similar to XML in its readability, if formatted accordingly, but with less redundancy, and thus smaller size. These properties make it interesting for usage in other programming languages as well and parsers exist in almost any of the widespread ones. 
Serialization of complete memories in JSON is not supported by libOMM, as the format is only used when exchanging metadata via the REST interface.
The OMMBlock interface in libOMM provides a method to generate a JSON representation of a single block's metadata, for example for usage in web applications. It is called using `block.getJsonRepresentation()` on an existing block implementing the OMMBlock interface and returns a String containing the JSON object.
For blocks without much content JSON produces much smaller file sizes than XML due to its more concise text scaffold. For the moment, however, it is specialized on single blocks and on writing only. 

#### 3.3.4.3 Binary serialization

The native binary serialization and deserialization of single blocks is implemented analoguous to that of complete OMMs. A block can be saved using `OMMFactory.serializeBlock(OMMBlock block, File binFile)` and loaded using `OMMFactory.deserializeBlock(File binfile)`, working in the same manner as the OMM versions. Since in some cases it might be prudent not to store a block's entire content another serialization method for blocks exists: `OMMFactory.serializeBlock(OMMBlock block, File binFile, boolean savePrimaryID, boolean saveNamespace, boolean saveType, boolean saveDescription, boolean saveContributors, boolean saveFormat, boolean saveSubject, boolean savePayload, boolean saveLink)`. By setting the Boolean values either true or false the information contained in the block is filtered. Only the selected elements are stored, the rest will be left out. This allows smaller file sizes and more condensed information for the serialized data. 

The custom binary serialization strategy works accordingly: `OMMFactory.saveBlockToBinary(OMMBlock block, File binFile)` stores a complete block to a file, returning true on success, and `OMMFactory.loadBlockFromBinary(File binFile)` loads it, returning an OMMBlock. It is also possible to filter the information that is to be serialized, using a save method with Boolean filter parameters: `OMMFactory.saveBlockToBinary(OMMBlock block, File binFile, boolean savePrimaryID, boolean saveNamespace, boolean saveType, boolean saveDescription, boolean saveContributors, boolean saveFormat, boolean saveSubject, boolean savePayload, boolean saveLink)`. 

### 3.3.5 Deleting a block

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

