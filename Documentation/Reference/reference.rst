Reference Guide
===============

.. _Concepts:

Notion Concepts
---------------


.. _Pools:
.. _Pool:

Pool
^^^^

A Pool is a collection of DICOM images, anonymization rules and Devices to manage access.

.. _Devices:
.. _Device:

Device
^^^^^^^

A Device consists of a descriptive name, `Application Entity Title <https://www.dabsoft.ch/dicom/8/C.1/>`_ (which must be unique for the Pool, and is abbreviated AETitle), Hostname, Port and a freetext Description.  The AETitle is used to restrict access to images using DICOM protocols.  The AETitle and Hostname must match incoming DICOM connections to the Pool to allow access.  Both AETitle and Hostname are matched using Java's regular expressions.  For instance ``.*`` matches any entry.

.. _Anonymization:

Anonymization
^^^^^^^^^^^^^

Anonymization is handled in two stages.  Each incoming image is processed separately by the two stages.  First, an instance of the `CTP DICOM anonymizer <http://mircwiki.rsna.org/index.php?title=The_CTP_DICOM_Anonymizer>`_ processes images first according to its configuration file that is editable via the web app (see :ref:`AnonymizationIntro` for details).  The second stage is a series of custom `Javascript <http://en.wikipedia.org/wiki/JavaScript>`_ commands that are run on specific tags.  The Javascript has access to a custom map called ``tags`` that contains the original tags of the incoming image.  Javascript also has access to an object (``anonymization``) containing several useful methods for anonymization.  The methods are:

``debug(text) / info(text) / warn(text) / error(text)``
  Print a debug / info / warn / error message to the log

``hash(text,[length])``
  Return a string containing the first ``length`` digits of the MD5 hash of ``text`` (default length is all characters)

``setValue(Type,Key,Value)``
  Store a ``Value`` for later ``lookup``.  ``Value`` is indexed by ``Type`` and ``Key``.  The previous ``Value`` is overwritten by this method

``lookup(Type,Key)``
 Lookup and return a value indexed by ``Type`` and ``Key``.  If the value does not exist, ``lookup`` returns null

``sequenceNumber(Type,Key)``
   First lookup the sequence number indexed by ``Type`` and ``Value``.  If it does not exist, generate a unique number by incrementing the ``Type`` sequence.  For instance the first time ``anonymizer.sequenceNumber('PatientName', 'Jones')`` is called, the return value is the string ``'1'``.  On the second call, ``anonymizer.sequenceNumber('PatientName', 'Jones')`` also returns the string ``'1'``, however ``anonymizer.sequenceNumber('PatientName', 'Smith')`` returns the string ``'2'``.  Sequence numbers are used to generate ``PatientName`` and ``PatientID`` tags if particular identifiers are not required.  *NB:* see the :ref:`anonymizer reference <Anonymization>` for details on prepopulating the lookup tables.

``Exception(text)``
  Throw an exception, immediately stopping any further processing of this image.  Exceptions can be used to reject images that do not have proper lookup information.  See :ref:`PatientNameAnonymizer` for an example.

The :ref:`anonymization tutorial <AnonymizationIntro>` contains more details.

.. _DICOMConfig:

DICOM Configuration
-------------------

Notion's DICOM listener supports ``C-ECHO``, ``C-FIND``, ``C-STORE`` and ``C-MOVE``.  The listening port is specified by the command line argument ``--port`` (see :ref:`CLI`) and is 11117 by default.  All other DICOM configuration is by Pools and Devices.

.. _REST:

REST
----

All Notion functions are available through a `REST interface <http://en.wikipedia.org/wiki/REST>`_ available on port 11118 by default and can be changed using the ``--rest`` argument in the :ref:`command line <CLI>`.

============================ ========== ============
URL                          Method     Description
============================ ========== ============
/rest/pool                   GET        Get a list of all pools as json
/rest/pool/                  POST       Create a new Pool
/rest/pool/:id               GET        Get details of a pool identified by ``:id``
/rest/pool/:id               PUT        Update a Pool, NB: AETitle is read only!
/rest/pool/:id               DELETE     Deletes a Pool and all data contained in the Pool, use with caution!
/rest/pool/:id/move          PUT        Move studies internally between Pools
/rest/pool/:id/ctp           GET        Get the CTP configuration for this pool
/rest/pool/:id/ctp           PUT        Update the CTP configuration for this pool
/rest/pool/:id/statistics    GET        Get statistics for this Pool
/rest/pool/:id/device        GET        Get list of Devices for this Pool
/rest/pool/:id/studies       GET        Get list of Studies for this Pool
/rest/pool/:id/lookup        GET        Get list of Lookups for this Pool
/rest/pool/:id/script        GET        Get list of Anonymization scripts for this Pool
============================ ========== ============


.. _Webapp:

Webapp
------

In addition to the REST interface, an `Ember.js <http://emberjs.com/>`_ single page web application is avaliable on the same port as the REST interface.  By default it is http://localhost:11118/index.html but the port may be changed using the ``--rest`` argument on the command line.
