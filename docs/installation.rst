Maven project dependency
========================

The Lablink configuration server's compiled Java package is available on the |MCR|_.
Use it in your local Maven_ setup by including the following dependency into your *pom.xml*:

.. code-block:: xml

   <dependency>
     <groupId>at.ac.ait.lablink</groupId>
     <artifactId>config</artifactId>
     <version>0.1.2</version>
   </dependency>

.. note:: You may have to adapt this snippet to use the latest version, please check the |MCR|_.

The dependency above is the library JAR.
To run the server from the command line, use the assembly *config-<VERSION>-jar-with-dependencies.jar*, which is produced by the build below and published with the same coordinates and the classifier ``jar-with-dependencies``.

Installation from source
========================

Installation from source requires a local **Java Development Kit** installation, for instance the `Oracle Java SE Development Kit <https://www.oracle.com/java/technologies/downloads/>`_ or the `OpenJDK <https://openjdk.java.net/>`_.
The project compiles for Java 8, so JDK 8 or a newer JDK can be used.

Check out the project and compile it with Maven_:

.. code-block:: winbatch

   git clone https://github.com/AIT-Lablink/lablink-config-server
   cd lablink-config-server
   mvnw clean package

On Windows, run ``mvnw.cmd`` instead of ``mvnw``.

This will create the JAR file *config-<VERSION>-jar-with-dependencies.jar* in subdirectory *target/assembly*.
The main class of that JAR is ``at.ac.ait.lablink.config.Config``.

Publishing to the Maven Central Repository
==========================================

A release is published with ``mvnw deploy``. The build signs the artifacts and uploads them to the `Central Portal <https://central.sonatype.com/>`_. The upload uses a Central Portal user token from your Maven ``settings.xml`` and a local GPG key. Publishing is manual: the deployment stays pending until you publish it in the portal, and the build waits until that happens.

Docker image
============

A ready-to-run image is published on `Docker Hub <https://hub.docker.com/r/ait1/lablink-config-server>`_ as ``ait1/lablink-config-server:latest``:

.. code-block:: winbatch

   docker pull ait1/lablink-config-server:latest

See :doc:`here <running>` for how to start the container.
Building the image locally with :github_blob:`docker/build.cmd <docker/build.cmd>` tags the result with the same name.


.. |MCR| replace:: Maven Central Repository
.. _MCR: https://search.maven.org/artifact/at.ac.ait.lablink/config
.. _Maven: https://maven.apache.org
