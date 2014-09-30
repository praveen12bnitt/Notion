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
