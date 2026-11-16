Invoking the server from the command line
==========================================

The runnable JAR is produced as described in :doc:`installation`.
Every invocation selects an action with ``-a``.

.. code-block:: winbatch

   SET CONFIG_JAR=target\assembly\config-<VERSION>-jar-with-dependencies.jar

   java -jar %CONFIG_JAR% -a run -d configurations.db -p 10101

Starting the server
===================

``run`` starts the HTTP server and keeps running until the process is stopped.

``-d`` is the path of the SQLite database.
When it is omitted, the server uses ``configurations.db`` in the working directory.

``-p`` is the TCP port.
When it is omitted, the server listens on ``10101``.

Once the server is running, open http://localhost:10101 in a web browser.
The page lists the configurations stored in the database.
The meaning of ``/``, ``/view``, and ``/get`` is described in :doc:`configuration`.

Running with Docker
===================

The image ``ait1/lablink-config-server:latest`` (see :doc:`installation`) starts the server with ``-a run`` and reads the database from ``/app/config.db`` inside the container.
Publish port ``10101`` and mount the SQLite file at that path.

On Windows:

.. code-block:: winbatch

   docker run --rm -p 10101:10101 -v C:\path\to\configurations.db:/app/config.db ait1/lablink-config-server:latest

On Linux:

.. code-block:: bash

   docker run --rm -p 10101:10101 -v /path/to/configurations.db:/app/config.db ait1/lablink-config-server:latest

The server is then available at http://localhost:10101.

Arguments after the image name replace the default ``-d /app/config.db``.
To listen on another port, publish that port and pass both the database path and ``-p``.

On Windows:

.. code-block:: winbatch

   docker run --rm -p 20202:20202 -v C:\path\to\configurations.db:/app/config.db ait1/lablink-config-server:latest -d /app/config.db -p 20202

On Linux:

.. code-block:: bash

   docker run --rm -p 20202:20202 -v /path/to/configurations.db:/app/config.db ait1/lablink-config-server:latest -d /app/config.db -p 20202

Managing the database
=====================

The actions below exit after they finish.
Each of them requires ``-d``.
The database file must already exist, except for ``blank``, which creates it.
``-v`` is always a path to a text file.
The server reads that file and stores its contents.

Creating a database
-------------------

``blank`` creates a new database and the table ``LablinkConfigSimple``.
The command stops if the file already exists.

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a blank -d configurations.db

Adding an entry
---------------

``add`` inserts one configuration.
``-k`` is the identifier, ``-v`` is the file to store, and ``-n`` is the description shown in the browser listing.
``-i`` is optional and sets the invoke token described in :doc:`configuration`.

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a add -d configurations.db -k ait.demo.llproperties -v ll.properties -n "Lablink connection properties"

Replacing the stored text
-------------------------

``update`` replaces the stored text of an existing identifier.
The description and the invoke token are left unchanged.

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a update -d configurations.db -k ait.demo.llproperties -v ll.properties

Deleting an entry
-----------------

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a delete -d configurations.db -k ait.demo.llproperties

Listing entries
---------------

``list`` prints each matching entry, including the stored text.
Without ``-k``, every entry is printed.

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a list -d configurations.db
   java -jar %CONFIG_JAR% -a list -d configurations.db -k ait.demo.llproperties

``summary`` prints the identifier, the invoke token, and the description, without the stored text.

.. code-block:: winbatch

   java -jar %CONFIG_JAR% -a summary -d configurations.db

Help
====

Running the JAR without ``-a`` prints the usage text.

Logging
=======

The server writes a debug log to the console and to ``lablink-config-server.log`` in the working directory.
Each start of the process replaces that file.
In the Docker image the working directory is ``/app``, so the log file stays inside the container unless that path is mounted.
