#!/bin/bash

echo "" > events_complete.txt

for n in $(ls events_update*sql); do
	echo $n
	mysql -h localhost -u root easiersolar < $n
	echo $n >> events_complete.txt
done
