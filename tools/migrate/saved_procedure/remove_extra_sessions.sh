#!/bin/bash

for i in {1..160}
do
	mysql -h localhost -u root easiersolar < remove_extra_sessions.sql
	sleep 5
done
