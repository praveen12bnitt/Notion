.. include:: /global.rst

.. _UseCases:

Use Cases
=========

.. _MultiResearcherUseCase:

Multi-Researcher / Multi-Pool
-----------------------------

Multiple researchers desire to collect anonymized images and manage access to the images via DICOM queries.  Notion supports this use case through multiple Pools, each with custom anonymization rules and configurations.  A central pool (perhaps named :tt:`public`) would be available for modalities and/or image archives to send images containing PHI.  Each researcher could "claim" images by moving them to their Pool and deleting the images from the :tt:`public` Pool when completed.  With further refinement, this process could be automated based on lookup values for each Pool.