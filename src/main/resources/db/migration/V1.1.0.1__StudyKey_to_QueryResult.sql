
-- Default the StudyKey to NULL.  Indicates no study has been associated with this query
ALTER TABLE QUERYRESULT add column StudyKey int with NULL;
