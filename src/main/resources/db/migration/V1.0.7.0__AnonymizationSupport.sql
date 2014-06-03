
CREATE TABLE ANONYMIZATIONMAPTEMP (
	MapKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	PoolKey INT not null,
	
	OriginalPatientName VARCHAR(128),
	AnonymizedPatientName VARCHAR(128),

	OriginalPatientID VARCHAR(128),
	AnonymizedPatientID VARCHAR(128),
	
	OriginalAccessionNumber VARCHAR(128),
	AnonymizedAccessionNumber VARCHAR(128),

	OriginalPatientBirthDate VARCHAR(128),
    AnonymizedPatientBirthDate VARCHAR(128),
    UpdatedTimestamp TIMESTAMP default current_timestamp,
	CONSTRAINT ANONYMIZATIONMAPTEMP_fk1
    	foreign key ( PoolKey ) references POOL(PoolKey) on delete cascade

);

CREATE INDEX ANONYMIZATIONMAPTEMP_idx1 on ANONYMIZATIONMAPTEMP ( PoolKey, OriginalPatientName, AnonymizedPatientName, OriginalPatientID, AnonymizedPatientID, OriginalAccessionNumber, AnonymizedAccessionNumber, OriginalPatientBirthDate, AnonymizedPatientBirthDate );

CREATE TABLE ANONYMIZATIONMAP (
	MapKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	PoolKey INT not null,
	
	OriginalPatientName VARCHAR(128),
	AnonymizedPatientName VARCHAR(128),

	OriginalPatientID VARCHAR(128),
	AnonymizedPatientID VARCHAR(128),
	
	OriginalAccessionNumber VARCHAR(128),
	AnonymizedAccessionNumber VARCHAR(128),

	OriginalPatientBirthDate VARCHAR(128),
    AnonymizedPatientBirthDate VARCHAR(128),
    UpdatedTimestamp TIMESTAMP default current_timestamp,
	CONSTRAINT ANONYMIZATIONMAP_fk1
    	foreign key ( PoolKey ) references POOL(PoolKey) on delete cascade

);

CREATE INDEX ANONYMIZATIONMAP_idx1 on ANONYMIZATIONMAP ( PoolKey, OriginalPatientName, AnonymizedPatientName, OriginalPatientID, AnonymizedPatientID, OriginalAccessionNumber, AnonymizedAccessionNumber, OriginalPatientBirthDate, AnonymizedPatientBirthDate );

