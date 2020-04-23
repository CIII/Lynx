#!/bin/bash

for n in $(ls sessions*sql); do
	echo $n
	mysql -h localhost -u root easiersolar < $n
	sleep 3
done

for n in $(ls session_*sql); do
        echo $n
        mysql -h localhost -u root easiersolar < $n
	sleep 3
done

for n in $(ls event*sql); do
        echo $n
        mysql -h localhost -u root easiersolar < $n
	sleep 3
done
