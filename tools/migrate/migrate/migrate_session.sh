#!/bin/bash

echo "" > sessions_complete.txt

for n in $(ls session*sql); do
	echo $n
	mysql -h localhost -u root easiersolar < $n
	echo $n >> sessions_complete.txt
done
