CREATE TABLE CONNECTOR (
	ConnectorKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	Name varchar(255) NOT NULL UNIQUE,
	Description varchar(255),

	QueryPoolKey INT NOT NULL ,
	QueryDeviceKey INT NOT NULL,
	
	DestinationPoolKey INT NOT NULL,
	CONSTRAINT
	  CONNECTOR_fk1 foreign key (QueryPoolKey) references POOL(PoolKey) on delete cascade,
	CONSTRAINT
	  CONNECTOR_fk2 foreign key (DestinationPoolKey) references POOL(PoolKey) on delete cascade,
	CONSTRAINT
	  CONNECTOR_fk3 foreign key (QueryDeviceKey) references DEVICE(DeviceKey) on delete cascade
);