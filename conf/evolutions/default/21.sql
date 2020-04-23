# --- !Ups

INSERT INTO event_types (name, event_category) VALUES
    ("Form Step 4","Form Step Submit"),
    ("Form Step 5","Form Step Submit"),
    ("Form Step 6","Form Step Submit"),
    ("Form Step 7","Form Step Submit"),
    ("Form Step 8","Form Step Submit"),
    ("Maxmind failure on page load","Page"),
    ("Landing Page Completed","Form Step Submit"),
    ("Address Completed","Form Step Submit"),
    ("Ownership Completed","Form Step Submit"),
    ("Power Bill Completed","Form Step Submit"),
    ("Power Company Completed","Form Step Submit"),
    ("Name Completed","Form Step Submit"),
    ("Email Completed","Form Step Submit"),
    ("Phone Completed","Form Step Submit"),
    ("Email Modal Completed","Form Step Submit"),
    ("Credit Score Completed","Form Step Submit");

# --- !Downs

DELETE FROM event_types WHERE name IN ("Form Step 4",
                                       "Form Step 5",
                                       "Form Step 6",
                                       "Form Step 7",
                                       "Form Step 8",
                                       "Maxmind failure on page load",
                                       "Landing Page Completed",
                                       "Address Completed",
                                       "Ownership Completed",
                                       "Power Bill Completed",
                                       "Power Company Completed",
                                       "Name Completed",
                                       "Email Completed",
                                       "Phone Completed",
                                       "Email Modal Completed",
                                       "Credit Score Completed");