Prerequisites
=============

The walkthrough uses the runnable JAR described in :doc:`installation`.
Each step also shows the same action with the Docker image ``ait1/lablink-config-server:latest``.

The image entrypoint already passes ``-a run``.
Creating and editing the database therefore overrides that entrypoint and mounts the working directory at ``/work``.
That directory is where ``demo.db``, ``ll.properties``, and ``client.json`` live.
In the Windows commands, replace ``C:\path\to\examples`` with that directory.
Run the Linux commands from that directory.
The single quotes around ``/app/lib/*`` stop the shell from expanding the wildcard. The JVM inside the container expands it.

Creating a database
===================

Create an empty database in the working directory:

.. code-block:: winbatch

   SET CONFIG_JAR=target\assembly\config-<VERSION>-jar-with-dependencies.jar

   java -jar %CONFIG_JAR% -a blank -d demo.db

With the Docker image on Windows:

.. code-block:: winbatch

   SET DOCKER_JAVA=docker run --rm --entrypoint java -v C:\path\to\examples:/work -w /work ait1/lablink-config-server:latest -cp /app/lib/* at.ac.ait.lablink.config.Config

   %DOCKER_JAVA% -a blank -d demo.db

On Linux:

.. code-block:: bash

   docker_java() {
     docker run --rm --entrypoint java -v "$PWD":/work -w /work ait1/lablink-config-server:latest -cp '/app/lib/*' at.ac.ait.lablink.config.Config "$@"
   }

   docker_java -a blank -d demo.db

This creates ``demo.db`` and the table ``LablinkConfigSimple``.
The command stops if ``demo.db`` already exists.

Adding configurations
=====================

Save the connection properties and the client JSON from :doc:`configuration` as ``ll.properties`` and ``client.json``.
Then add both files:

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a add -d demo.db -k ait.demo.llproperties -v ll.properties -n "Lablink connection properties"
   java -jar %CONFIG_JAR% -a add -d demo.db -k ait.demo.client.config -v client.json -n "Example client configuration"

With the Docker image on Windows:

.. code-block:: winbatch

   %DOCKER_JAVA% -a add -d demo.db -k ait.demo.llproperties -v ll.properties -n "Lablink connection properties"
   %DOCKER_JAVA% -a add -d demo.db -k ait.demo.client.config -v client.json -n "Example client configuration"

On Linux:

.. code-block:: bash

   docker_java -a add -d demo.db -k ait.demo.llproperties -v ll.properties -n "Lablink connection properties"
   docker_java -a add -d demo.db -k ait.demo.client.config -v client.json -n "Example client configuration"

``-v`` is the path of the file to store, and ``-n`` is the description shown later in the browser.
Check the result without printing the stored text:

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a summary -d demo.db

With the Docker image on Windows:

.. code-block:: winbatch

   %DOCKER_JAVA% -a summary -d demo.db

On Linux:

.. code-block:: bash

   docker_java -a summary -d demo.db

The listing contains ``ait.demo.llproperties`` and ``ait.demo.client.config``.

Adding an entry that runs Python
================================

Save the ``Builder`` script from :doc:`configuration` as ``builder.py``.
``-i`` sets the invoke token ``python:Builder:getJson``.
``/get`` will call ``Builder.getJson()`` and return that string.
``/view`` still shows ``builder.py``.

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a add -d demo.db -k ait.demo.generated.config -v builder.py -n "Configuration built by Python" -i python:Builder:getJson

With the Docker image on Windows:

.. code-block:: winbatch

   %DOCKER_JAVA% -a add -d demo.db -k ait.demo.generated.config -v builder.py -n "Configuration built by Python" -i python:Builder:getJson

On Linux:

.. code-block:: bash

   docker_java -a add -d demo.db -k ait.demo.generated.config -v builder.py -n "Configuration built by Python" -i python:Builder:getJson

Starting the server
===================

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a run -d demo.db

With the Docker image, publish port ``10101`` and point ``-d`` at the database inside the mounted directory.
Arguments after the image name replace the default ``-d /app/config.db``.

On Windows:

.. code-block:: winbatch

   docker run --rm -p 10101:10101 -v C:\path\to\examples:/work ait1/lablink-config-server:latest -d /work/demo.db

On Linux:

.. code-block:: bash

   docker run --rm -p 10101:10101 -v "$PWD":/work ait1/lablink-config-server:latest -d /work/demo.db

Open http://localhost:10101.
The page lists the three identifiers, and each row links to VIEW and GET.

VIEW opens the stored text in the browser, for example http://localhost:10101/view?id=ait.demo.client.config.
GET returns the body a Lablink client would load, at http://localhost:10101/get?id=ait.demo.client.config.
That URI is the value other clients put in ``labLinkPropertiesUrl`` when the identifier is ``ait.demo.llproperties``, or pass with ``-c`` when the identifier is their own configuration.

For the Python entry, VIEW shows the script at http://localhost:10101/view?id=ait.demo.generated.config.
GET runs it and returns ``{"ClientName": "Example"}`` from http://localhost:10101/get?id=ait.demo.generated.config.

Using the server from the other examples
========================================

The example setups in the other Lablink repositories start this same server against a prepared database.
In the `lablink-examples <https://github.com/AIT-Lablink/lablink-examples>`_ repository, ``scripts/0_config/run_config.cmd`` runs ``-a run`` on ``examples-config.db`` and leaves the server at http://localhost:10101.
