.. include:: /global.rst


Tutorial 2
==========

.. _AnonymizationIntro:

Anonymization
-------------

To turn on Anonymization, click the :tt:`Edit` icon next to the Pool info on this page http://localhost:11118/index.html#/pools/pool/1.  In the dialog box, check :tt:`Use Anonymizer`, click :tt:`Save changes` and :tt:`close`.  Now our Pool display looks somewhat different with two new sections: :tt:`CTP Configuration` and :tt:`Anonymization Rules`.  Opening up the :tt:`CTP Configuration` reveals an editor for the CTP configuration used by the pool.  The particular file is for the `CTP DICOM anonymizer <http://mircwiki.rsna.org/index.php?title=The_CTP_DICOM_Anonymizer>`_ configuration stored on the Notion server.  Editing and saving the file will change how anonymization is handled on the Notion server.

.. figure:: /images/ctp_configuration.png
   

