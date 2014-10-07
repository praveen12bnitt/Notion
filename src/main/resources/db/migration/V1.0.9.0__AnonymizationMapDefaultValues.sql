update ANONYMIZATIONMAP set OriginalPatientName = '' where OriginalPatientName is null;
update ANONYMIZATIONMAP set AnonymizedPatientName = '' where AnonymizedPatientName is null;

update ANONYMIZATIONMAP set OriginalPatientID = '' where OriginalPatientID is null;
update ANONYMIZATIONMAP set AnonymizedPatientID = '' where AnonymizedPatientID is null;
	
update ANONYMIZATIONMAP set OriginalAccessionNumber = '' where OriginalAccessionNumber is null;
update ANONYMIZATIONMAP set AnonymizedAccessionNumber = '' where AnonymizedAccessionNumber is null;

update ANONYMIZATIONMAP set OriginalPatientBirthDate = '' where OriginalPatientBirthDate is null;
update ANONYMIZATIONMAP set AnonymizedPatientBirthDate = '' where AnonymizedPatientBirthDate is null;

	
update ANONYMIZATIONMAPTEMP set OriginalPatientName = '' where OriginalPatientName is null;
update ANONYMIZATIONMAPTEMP set AnonymizedPatientName = '' where AnonymizedPatientName is null;

update ANONYMIZATIONMAPTEMP set OriginalPatientID = '' where OriginalPatientID is null;
update ANONYMIZATIONMAPTEMP set AnonymizedPatientID = '' where AnonymizedPatientID is null;
	
update ANONYMIZATIONMAPTEMP set OriginalAccessionNumber = '' where OriginalAccessionNumber is null;
update ANONYMIZATIONMAPTEMP set AnonymizedAccessionNumber = '' where AnonymizedAccessionNumber is null;

update ANONYMIZATIONMAPTEMP set OriginalPatientBirthDate = '' where OriginalPatientBirthDate is null;
update ANONYMIZATIONMAPTEMP set AnonymizedPatientBirthDate = '' where AnonymizedPatientBirthDate is null;


alter table ANONYMIZATIONMAP alter column OriginalPatientName not null;
alter table ANONYMIZATIONMAP alter column AnonymizedPatientName not null;

alter table ANONYMIZATIONMAP alter column OriginalPatientID not null;
alter table ANONYMIZATIONMAP alter column AnonymizedPatientID not null;
	
alter table ANONYMIZATIONMAP alter column OriginalAccessionNumber not null;
alter table ANONYMIZATIONMAP alter column AnonymizedAccessionNumber not null;

alter table ANONYMIZATIONMAP alter column OriginalPatientBirthDate not null;
alter table ANONYMIZATIONMAP alter column AnonymizedPatientBirthDate not null;

	
alter table ANONYMIZATIONMAPTEMP alter column OriginalPatientName not null;
alter table ANONYMIZATIONMAPTEMP alter column AnonymizedPatientName not null;

alter table ANONYMIZATIONMAPTEMP alter column OriginalPatientID not null;
alter table ANONYMIZATIONMAPTEMP alter column AnonymizedPatientID not null;
	
alter table ANONYMIZATIONMAPTEMP alter column OriginalAccessionNumber not null;
alter table ANONYMIZATIONMAPTEMP alter column AnonymizedAccessionNumber not null;

alter table ANONYMIZATIONMAPTEMP alter column OriginalPatientBirthDate not null;
alter table ANONYMIZATIONMAPTEMP alter column AnonymizedPatientBirthDate not null;
