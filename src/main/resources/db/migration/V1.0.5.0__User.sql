
create table USERS (
  UserKey int NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  Username varchar(512) NOT NULL,
  email varchar(512) NOT NULL,
  uid varchar(255) NOT NULL,
  Password varchar(512),
  Salt varchar(64),
  Activated boolean,
  ActivationHash varchar(256),
  CONSTRAINT users_email UNIQUE ( username )
  );

  create index users_index_hash on USERS ( ActivationHash );
  create index users_index_email on USERS ( email );
  create index users_index_uid on USERS ( uid );
  
  