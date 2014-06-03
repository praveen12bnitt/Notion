
DROP TABLE SCRIPT;

CREATE TABLE SCRIPT (
	ScriptKey INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
	PoolKey INT NOT NULL,
	Script VARCHAR(32000),
	CONSTRAINT SCRIPT_fk1
    	foreign key ( PoolKey ) references POOL(PoolKey) on delete cascade
);

CREATE INDEX Script_idx2 on SCRIPT ( PoolKey );

