.. include:: ../global.rst

.. _Installation:

Installation
============

After downloading Notion and unzipping, look in the ``Notion-x.x.x`` folder (where ``x.x.x`` is the version number).  This folder contains the following items:

:tt:`Notion-x.x.x.jar`
    The Jar file with the Notion classes and runtime files bundled.

:tt:`lib`
    External Java libraries.  Must be maintained in the same directory as ``Notion-x.x.x.jar`` because the main jar file references the libraries by a relative path.

:tt:`Documentation`
    Contains this documentation, both in html and ePub.

The jar file and lib directory may be copied to any location as needed.

Running Notion
--------------

TL;DR
^^^^^

.. code-block:: bash

  java -jar Notion.jar

Point a browser at http://localhost:11118

.. _CLI:

Command line options
^^^^^^^^^^^^^^^^^^^^

Notion supports setting several command line parameters:

.. code-block:: bash

  >> java -jar Notion-1.0.0.jar  --help
  usage: Notion [options] [directory]
  options:
   -d,--db <arg>     Start the embedded DB Web server on the given port
                     (normally 8082), will not start without this option
   -h,--help         Print help and exit
   -m,--memoryDB     Start services in memory (DB only)
   -p,--port <arg>   Port to listen for DICOM traffic, default is 11117
   -r,--rest <arg>   Port to listen for REST traffic, default is 11118
  
  Start the Notion PACS system using [directory] for data storage.  If not
  specified defaults to the current working directory
  (/Users/blezek/Source/ResearchPACS).  By default the REST api is started
  on port 11118, with the web app being served at http://localhost:11118
  The DICOM listener starts on port 11117 (can be changed with a --port) and
  provides C-ECHO, C-MOVE, C-STORE and C-FIND services.  Notion serves as a
  full DICOM query / retrive SCP.
  Database administration can be handled via the bundled web interface.  By
  default, http://localhost:8082, if the  --db option is given.  It will not
  start up otherwise. The JDBC connection URL is given in the log message of
  the server.


Of particular interest is the :tt:`--db` argument which specifies a port for the server to listen on for `web access <localhost:8082>`_ to the embedded database.  Performance tuning, db maintance, etc can be performed through the web interface (http://localhost:8082 by default).

Notion listens for DICOM requests on a specific port (11117 by default) and for HTTP requests (port 11118 by default).  Configuration of DICOM is covered :ref:`elsewhere <DICOMConfig>`.  The HTTP server responds to :ref:`REST requests <REST>` and serves :ref:`Notion's webapp <Webapp>`.

