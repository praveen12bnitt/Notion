.. ResearchPACS documentation master file, created by
   sphinx-quickstart on Tue Nov 12 11:25:06 2013.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. include:: global.rst

Notion: a PACS for researchers
==============================

This project exists to create a low-overhead PACS system for researchers in radiology.  Notion provides several useful facilities for a department including multiple, independant "pools" of images, restrictions on access to images stored in pools, anonymization, simple installation, web interface for administration.


Why use Notion?
---------------

If you have a need to:

- store DICOM images, but do not want/have a dedicated research PACS
- anonymize DICOM images
- map Names, IDs and Accession numbers during anonymization
- maintain separation of image data across projects
- scale to 100's of independant research projects

Why *not* use Notion?
---------------------

If you:

- already have a research PACS
- do not need to anonymize data
- do not care about isolation between research projects
- are happy using manual anonymization tools

There are other Open Source / free PACS systems available including 

- Conquest_
- orthanc_
- OsiriX_
- DCM4CHE_ (Notion is based on dcm4che)
- ClearCanvas_

.. _Conquest: http://ingenium.home.xs4all.nl/dicom.html
.. _orthanc: http://code.google.com/p/orthanc/
.. _OsiriX: http://www.osirix-viewer.com/
.. _DCM4CHE: http://www.dcm4che.org/
.. _ClearCanvas: http://www.clearcanvas.ca/

Depending on needs, one of the other systems may be a better fit.

Contents:
=========

.. toctree::
   :maxdepth: 2

   GettingStarted/installation
   GettingStarted/getting_started
   GettingStarted/anonymization
   GettingStarted/moving_images
   GettingStarted/use_cases
   Reference/reference
   Development/development

.. Reference/kitchen_sink


