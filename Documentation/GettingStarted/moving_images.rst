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

.. _PatientNameAnonymizer:

PatientName
"""""""""""

In the second pool, we will pre-populate our Lookup data and use that in the anonymizer.  If the :tt:`PatientName` lookup fails, throw an exception thus rejecting the image.

.. code-block:: javascript

	// Lookup the PatientName
	var pn = anonymizer.lookup ( 'PatientName', tags.PatientName )
	if ( pn == null ) {
	  anonymizer.exception ( "Could not find " + tags.PatientName + " in the lookup table")
	}
	// Use the new PatientName, make sure it's the last statement
	pn

PatientID
"""""""""

PatientID will be set using a zero padded incrementing number, similar to the :ref:`prior tutorial <AnonymizationIntro>`.  Using zero padding helps with sorting images properly.

.. code-block:: javascript

   // Generate a sequence number for PatientID
   var sequenceNumberString = anonymizer.sequenceNumber ( 'PatientName', tags.PatientName )
   // Convert to an Integer
   var sequenceNumber = new java.lang.Integer ( sequenceNumberString )
   // Zero pad
   var padded = java.lang.String.format ( "%05d", sequenceNumber )
   // Final output is Second-#####
   'Second-' + sequenceNumber

AccessionNumber
"""""""""""""""

We would like our AccessionNumbers to be 5 digits long, but randomized.  Using a hash function, AccessionNumbers from multiple series will map to the same anonymized value.  So the script for AccessionNumber is simple.

.. code-block:: javascript

   // Generate a 5 digit hash value
   anonymizer.hash ( tags.AccessionNumber, 5 )


SeriesDescription and StudyDescription
""""""""""""""""""""""""""""""""""""""

By default, the CTP anonymizer removes both SeriesDescription and StudyDescription.  We would like to preserve these tags.  By returning the value stored in the ``tags`` hash map, we will restore the default value removed by CTP.  Remember the ``tags`` hash contains the values from the non-anonymized image.

.. code-block:: javascript

	// SeriesDescription
	// Preserve the value
	tags.SeriesDescription

.. code-block:: javascript

	// StudyDescription
	// Preserve the value
	tags.StudyDescription

Setting Lookup Values
^^^^^^^^^^^^^^^^^^^^^

In the :ref:`PatientName anonymization script <PatientNameAnonymizer>` we referenced a lookup value using ``anonymizer.lookup()``.  This value needs to be set prior to images arriving at this Pool.  To enter the value, follow the :tt:`Edit Lookup Values` link (http://localhost:11118/index.html#/pools/lookup/2) on the :tt:`second` Pool.  It should be empty and the start.  Click the :tt:`New PatientName` button at the top of the table and enter the following information in the dialog box:

:Key: MRA-0068
:Value: Subject-ABCDE

The lookup table should look like this:

.. figure:: /images/lookup_table.png
	:width: 768
	:align: center

	Populated Lookup table for the :tt:`second` Pool.

NB: data can also be entered in bulk by uploading a CSV file.  The CSV is verified in the browser, then transmitted to the server.

Moving Images
^^^^^^^^^^^^^

Notion is able to transfer studies directly from one pool to another.  We are going to move images from the :tt:`test` Pool into the :tt:`second` pool.  To accomplish this, visit the ``View Studies`` link under the :tt:`test` Pool (http://localhost:11118/index.html#/pools/studies/1).  Just above the table is a ``Move destination`` selection box.  Go ahead and change this to the :tt:`second` Pool which is named ``An Investigator's Pool`` (1 in figure below).  Then select the Patient with PatientID of ``MRA-0068`` (2 in the figure).  Finally, click ``Move`` in the upper right of the table (3 in the figure).

.. figure:: /images/move_images.png
	:width: 768
	:align: center

	Move images to a different Pool.



.. figure:: /images/move_completed.png
	:width: 768
	:align: center

	A status message appears at the beginning of the send, and notifies of completion of the send.

To verify the move, visit the Studies page of the :tt:`second` Pool (http://localhost:11118/index.html#/pools/studies/2).  We see the ``PatientID, PatientName, AccessionNumber and StudyDescription`` are all as we expected!

.. image:: /images/move_verified.png
	:width: 768
	:align: center

Next Steps
----------

This completes the Notion tutorial.  For further information checkout Notion's :ref:`concepts <Concepts>` and :ref:`use cases <UseCases>`.
