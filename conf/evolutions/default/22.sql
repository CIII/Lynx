# --- !Ups


CREATE INDEX forms_session_id ON forms (session_id);
CREATE INDEX forms_event_id ON forms (event_id);
CREATE INDEX session_attributes_session_id ON session_attributes (session_id);
CREATE INDEX events_session_id ON events (session_id);
CREATE INDEX browsers_browser_id ON browsers (browser_id);
CREATE INDEX revenues_session_id ON revenues (session_id);


# --- !Downs

DROP INDEX forms_session_id ON forms;
DROP INDEX forms_event_id ON forms;
DROP INDEX session_attributes_session_id ON session_attributes;
DROP INDEX events_session_id ON events;
DROP INDEX browsers_browser_id ON browsers;
DROP INDEX revenues_session_id ON revenues;