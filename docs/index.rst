****************************
Lablink configuration server
****************************

.. meta::
   :description lang=en: Lablink configuration server

This package provides the Lablink configuration server.
The server stores configuration text in a SQLite database and serves it over HTTP.
Lablink clients load their configuration from this server, typically through a URI of the form ``http://localhost:10101/get?id=<identifier>``.

The server listens on port ``10101`` by default.
A web browser pointed at the server lists every stored configuration.
``/view?id=`` shows the stored text for one identifier.
``/get?id=`` returns the body a client receives.
When an entry names a Python class and method, ``/get`` returns the text produced by that method instead of the stored script.

Installation
============

Find information about the installation of the Lablink configuration server :doc:`here <installation>`.

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Installation

   /installation

Running the server
==================

Find basic instructions for running the server :doc:`here <running>`.

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Running the server

   /running

Configuration
=============

Find the reference for the configuration database and the HTTP interface :doc:`here <configuration>`.

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Configuration

   /configuration

Examples
========

Find a step-by-step example of creating a database and serving it :doc:`here <examples>`.

.. toctree::
   :maxdepth: 2
   :hidden:
   :caption: Examples

   /examples

Version history
===============

:0.1.0: initial release
:0.1.1: deployment to Maven Central and dependency updates
:0.1.2: serve the viewer logo over HTTP, and a further Log4j update
