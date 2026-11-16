Overview
========

Each configuration is one row in the SQLite table ``LablinkConfigSimple``.
The server returns the stored text as it was saved.
The one exception is an entry whose invoke token names a Python class and method: ``/get`` then returns the string produced by that method.

.. topic:: Columns

  :*identifier*: unique key used in ``?id=``. This is the primary key.
  :*config*: stored text. This may be a properties file, a JSON document, or a Python script.
  :*invoke*: optional token ``python:<Class>:<method>``. When this column is empty, ``/get`` returns *config* unchanged.
  :*description*: short text shown in the browser listing. It is not part of the body returned to clients.

.. seealso:: See `below <#example-configuration>`_ for a complete client configuration and the connection properties it refers to.

Creating the table
==================

The ``blank`` action (see :doc:`running`) creates the table:

.. code-block:: sql

   CREATE TABLE "LablinkConfigSimple" (
     "identifier" TEXT NOT NULL UNIQUE,
     "config" TEXT NOT NULL,
     "invoke" TEXT,
     "description" TEXT NOT NULL,
     PRIMARY KEY("identifier")
   )

HTTP interface
==============

The server accepts HTTP GET on the following paths.

.. topic:: Paths

  :``/``: an HTML table of every identifier, with a description, the invoke token, and links to view and get that entry
  :``/view?id=<identifier>``: an HTML page with the description and the stored text. A Python invoke token is not executed.
  :``/get?id=<identifier>``: the body a Lablink client receives
  :``/logo.png``: the logo used by the HTML pages

A request to ``/get`` or ``/view`` without ``id`` asks for an identifier.
``/view`` reports an unknown identifier as not found.
``/get`` answers an unknown identifier with the text ``null``.

Clients refer to an entry with a URI such as ``http://localhost:10101/get?id=ait.demo.llproperties``.
That URI is what other Lablink manuals put in ``labLinkPropertiesUrl``, ``syncHostPropertiesUrl``, or the client's own ``-c`` argument.

Python invoke
=============

Set *invoke* to ``python:<Class>:<method>`` when adding an entry.
The token has three parts separated by colons.
The first part must be ``python``, and the method name must not be empty.

The stored text must be a Python script that defines ``<Class>``.
On ``/get``, the server runs the script with `Jython <https://www.jython.org/>`_, creates an instance with ``<Class>()``, calls ``<method>()`` with no arguments, and returns that call's string result.
``/view`` still shows the script.

The following script, stored with the invoke token ``python:Builder:getJson``, makes ``/get`` return a one-line JSON document.
The :doc:`examples` page adds this file with ``-i python:Builder:getJson``.

.. code-block:: python

   class Builder:
       def getJson(self):
           return '{"ClientName": "Example"}'

Example configuration
=====================

The following JSON is a typical client configuration.
Stored under the identifier ``ait.demo.client.config``, it is what a client receives from ``http://localhost:10101/get?id=ait.demo.client.config``.
Field names differ between clients; this sample follows the names used by several of the provided Lablink clients.

.. code-block:: json

   {
      "ClientDescription": "Demonstration client.",
      "ClientName": "DemoClient",
      "GroupName": "Demo",
      "ScenarioName": "DemoScenario",
      "labLinkPropertiesUrl": "http://localhost:10101/get?id=ait.demo.llproperties",
      "syncHostPropertiesUrl": "http://localhost:10101/get?id=ait.demo.sync-host.properties"
   }

The connection properties referred to by ``labLinkPropertiesUrl`` are a separate entry, stored under ``ait.demo.llproperties``.
The Lablink core reads the MQTT broker settings from this file:

.. code-block:: properties

   lowLevelComm.brokerAddress = localhost
   lowLevelComm.brokerPort = 1883
   lowLevelComm.connectionProtocol = tcp
