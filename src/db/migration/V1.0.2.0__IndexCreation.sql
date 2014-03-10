CREATE TABLE QUERY (
	QueryKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	PoolKey INT NOT NULL,
	DeviceKey INT NOT NULL,
	DestinationPoolKey INT NOT NULL,
	Status VARCHAR(250),
	CONSTRAINT QUERY_fk1
	  foreign key ( PoolKey ) references POOL(PoolKey) on delete cascade
);

CREATE INDEX QUERY_idx1 ON QUERY ( PoolKey );

CREATE TABLE QUERYITEM (
	QueryItemKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	QueryKey INT NOT NULL,
	Status VARCHAR(250),
	PatientName VARCHAR(250),
	PatientID VARCHAR(250),
	AccessionNumber VARCHAR(250),
	PatientBirthDate VARCHAR(250),
	StudyDate VARCHAR(250),
	ModalitiesInStudy VARCHAR(250),
	StudyDescription VARCHAR(250),
	CONSTRAINT QUERYITEM_fk1
	  foreign key ( QueryKey ) references QUERY(QueryKey) on delete cascade
);

CREATE INDEX QUERYITEM_idx1 ON QUERYITEM ( QueryKey );


CREATE TABLE QUERYRESULT (
	QueryResultKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	QueryItemKey INT NOT NULL,
	Status VARCHAR(250),
	DoFetch VARCHAR(16),
	StudyInstanceUID VARCHAR(250),
	PatientName VARCHAR(250),
	PatientID VARCHAR(250),
	AccessionNumber VARCHAR(250),
	PatientBirthDate VARCHAR(250),
	StudyDate VARCHAR(250),
	ModalitiesInStudy VARCHAR(250),
	StudyDescription VARCHAR(250),
	CONSTRAINT QUERYRESULT_fk1
	  foreign key ( QueryItemKey ) references QUERYITEM(QueryItemKey) on delete cascade
);

CREATE INDEX QUERYRESULT_idx1 ON QUERYRESULT ( QueryItemKey );


