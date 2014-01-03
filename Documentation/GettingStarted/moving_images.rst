.. include:: /global.rst

.. _MovingImages:

Tutorial 3
==========

Glad you have made it this far in the tutorials!  By now, you have learned :ref:`how to create Pools and Devices<CreateAPool>`, :ref:`send images via the command line <SendImagesToAPool>` and :ref:`construct a simple anonymizer <AnonymizationIntro>`.  Here we are going to demonstrate a :ref:`simple use case <MultiResearcherUseCase>` of moving images from a Pool without an anonymizer to one with an anonymizer.  This would be useful for the scenario of multiple researchers collecting images in a common pool, then transferring images to the investigator specific Pool.

Moving images
^^^^^^^^^^^^^

If you've followed the tutorials, you should have 2 studies in the :tt:`test` Pool.  If not, go back and follow the :ref:`prior tutorial <AnonymizationIntro>`.

Create a second Pool
^^^^^^^^^^^^^^^^^^^^

Just as we :ref:`created the first Pool <CreateAPool>`, now create a second Pool called :tt:`second` with the Anonymizer enabled.  Finished?  Good.  Let's construct a few different anonymization rules for some different tags.

Create Anonymization Rules
^^^^^^^^^^^^^^^^^^^^^^^^^^

Create several anonymization rules for these tags:

* :tt:`PatientName`
* :tt:`PatientID`
* :tt:`AccessionNumber`
* :tt:`SeriesDescription`
* :tt:`StudyDescription`

The new Pool should look like this (http://localhost:11118/index.html#/pools/pool/2):

.. figure:: /images/second_pool.png
	:width: 768
	:align: center

	The anonymization configuration for a second pool.  This will be the landing place for an individual investigator's images.

The scripts are:

PatientName
"""""""""""

.. code-block:: javascript

	
