
-- Create a GROUP
create table GROUPS (
  GroupKey int NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  Name varchar(255) NOT NULL,
  Description varchar(1024)
);

-- Create a mapping between USERS and GROUP
create table USERGROUP (
  UserGroupKey int NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  UserKey int NOT NULL CONSTRAINT USERGROUP_fk1 REFERENCES USERS ON DELETE CASCADE,
  GroupKey int NOT NULL CONSTRAINT USERGROUP_fk2 REFERENCES GROUPS ON DELETE CASCADE
);
create index USERGROUP_idx1 on USERGROUP ( UserKey, GroupKey );

create table GROUPROLE (
  GroupRoleKey int NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  GroupKey int NOT NULL CONSTRAINT GROUPROLE_fk1 REFERENCES GROUPS ON DELETE CASCADE,
  PoolKey int NOT NULL CONSTRAINT GROUPROLE_fk2 REFERENCES POOL ON DELETE CASCADE,
  IsPoolAdmin boolean,
  IsCoordinator boolean
);
create index GROUPROLE_idx1 on GROUPROLE ( GroupKey, PoolKey );  

alter table USERS add column IsAdmin boolean;


