-- -----------------------------------------------------
-- Table SERIES
-- -----------------------------------------------------

CREATE  TABLE SERIES (
  SeriesKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY ,
  StudyKey INT NOT NULL ,
  SeriesInstanceUID VARCHAR(250) NOT NULL ,
  SeriesNumber VARCHAR(250)  ,
  Modality VARCHAR(250)  ,
  BodyPartExamined VARCHAR(250)  ,
  Laterality VARCHAR(250)  ,
  SeriesDescription VARCHAR(250)  ,
  InstitutionName VARCHAR(250)  ,
  StationName VARCHAR(250)  ,
  InstitutionalDepartmentName VARCHAR(250)  ,
  PerformingPhysicianName VARCHAR(250)  ,
  NumberOfSeriesRelatedInstances VARCHAR(250)  ,
  CreatedTime TIMESTAMP  ,
  UpdatedTime TIMESTAMP 
 );


CREATE  INDEX SeriesInstanceUID_idx on SERIES (SeriesInstanceUID ASC) ;
CREATE  INDEX study_fk_idx on SERIES (StudyKey ASC) ;
CREATE  INDEX SeriesNumber_idx on SERIES (SeriesNumber ASC) ;
CREATE  INDEX Modality_idx on SERIES (Modality ASC) ;
CREATE  INDEX BodyPartExamined_idx on SERIES (BodyPartExamined ASC) ;
CREATE  INDEX Laterality_idx on SERIES (Laterality ASC) ;
CREATE  INDEX SeriesDescription_idx on SERIES (SeriesDescription ASC) ;
CREATE  INDEX InstitutionName_idx on SERIES (InstitutionName ASC) ;
CREATE  INDEX StationName_idx on SERIES (StationName ASC) ;
CREATE  INDEX InstitutionalDepartmentName_idx on SERIES (InstitutionalDepartmentName ASC) ;
CREATE  INDEX PerformingPhysicianName_idx on SERIES (PerformingPhysicianName ASC) ;
CREATE  INDEX series_created_idx on SERIES (CreatedTime ASC) ;
CREATE  INDEX series_updated_idx on SERIES (UpdatedTime ASC);



-- -----------------------------------------------------
-- Table INSTANCE
-- -----------------------------------------------------

CREATE  TABLE INSTANCE (
  InstanceKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY ,
  SeriesKey INT NOT NULL ,
  SOPInstanceUID VARCHAR(250) NOT NULL ,
  SOPClassUID VARCHAR(250) NOT NULL ,
  InstanceNumber VARCHAR(250)  ,
  ContentDate VARCHAR(250)  ,
  ContentTime VARCHAR(250)  ,
  UpdatedTime TIMESTAMP  ,
  CreatedTime TIMESTAMP  ,
  FilePath VARCHAR(250) NOT NULL ,
  CONSTRAINT INSTANCE_ibfk_1
    FOREIGN KEY (SeriesKey )
    REFERENCES SERIES (SeriesKey ));

CREATE  INDEX SOPInstanceUID_idx on INSTANCE (SOPInstanceUID ASC) ;
CREATE  INDEX series_fk_idx on INSTANCE (SeriesKey ASC) ;
CREATE  INDEX SOPClassUID_idx on INSTANCE (SOPClassUID ASC) ;
CREATE  INDEX InstanceNumber_idx on INSTANCE (InstanceNumber ASC) ;
CREATE  INDEX ContentDate_idx on INSTANCE (ContentDate ASC) ;


-- -----------------------------------------------------
-- Table STUDY
-- -----------------------------------------------------

CREATE  TABLE STUDY (
  StudyKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY ,
  PoolKey INT NOT NULL,
  PatientID VARCHAR(250)  ,
  Patientname VARCHAR(250)  ,
  PatientBirthDate VARCHAR(250)  ,
  PatientSex VARCHAR(250)  ,
  StudyInstanceUID VARCHAR(250) NOT NULL ,
  StudyID VARCHAR(250)  ,
  StudyDate VARCHAR(250)  ,
  StudyTime VARCHAR(250)  ,
  AccessionNumber VARCHAR(250)  ,
  ReferringPhysicianName VARCHAR(250)  ,
  StudyDescription VARCHAR(250)  ,
  StudyStatus VARCHAR(16) NOT NULL ,
  UpdatedTimestamp TIMESTAMP  ,
  CreatedTimestamp TIMESTAMP 
);

CREATE  INDEX StudyInstanceUID_UNIQUE on STUDY (StudyInstanceUID ASC) ;
CREATE  INDEX study_patient_idx on STUDY (StudyInstanceUID ASC, PatientID ASC, AccessionNumber ASC) ;
CREATE  INDEX StudyID_idx on STUDY (StudyID ASC) ;
CREATE  INDEX StudyDate_idx on STUDY (StudyDate ASC) ;
CREATE  INDEX AccessionNumber_idx on STUDY (AccessionNumber ASC) ;
CREATE  INDEX ReferringPhysicianName_idx on STUDY (ReferringPhysicianName ASC) ;
CREATE  INDEX StudyDescription_idx on STUDY (StudyDescription ASC) ;
CREATE  INDEX study_created_timestamp_idx on STUDY (CreatedTimestamp ASC) ;
CREATE  INDEX study_updated_timestamp_idx on STUDY (UpdatedTimestamp ASC) ;
CREATE  INDEX study_status_idx on STUDY (StudyStatus ASC);

CREATE TABLE Pool (
  PoolKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY ,
  Name VARCHAR(250) NOT NULL,
  ApplicationEntityTitle VARCHAR(256),
  Description VARCHAR(250) NOT NULL,
  
  CONSTRAINT Unique_AETitle UNIQUE(ApplicationEntityTitle)
);

CREATE TABLE DEVICE (
  DeviceKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY ,
  PoolKey INT NOT NULL,
  ApplicationEntityTitle VARCHAR(250),
  HostName VARCHAR(250),
  Port INT
);


