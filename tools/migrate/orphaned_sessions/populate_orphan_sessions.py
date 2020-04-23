#!/usr/bin/python

import mysql.connector
import urlparse
import collections 
import datetime
import time
import sys
import math
 
hostname = 'localhost'
username = 'root'
password = ''
database = 'esmigrate'

def events_into_days(events):
	total_duration = (events[-1][1] - events[0][1]).total_seconds()
	num_days = int(math.ceil(total_duration/86400))
	print len(events)
	print num_days
	buckets = []
	for i in xrange(num_days):
		buckets.append([])
	print buckets
	buckets[0].append(events[0])
	for event in events[1:]:
		time_past = (event[1] - events[0][1]).total_seconds()
		print time_past
		bucket = int(math.ceil(time_past/86400)) - 1
		buckets[bucket].append(event)
	
	return buckets

attributes = {}

arr_conn = mysql.connector.connect( host=hostname, user=username, passwd=password, db=database )
event_conn = mysql.connector.connect( host=hostname, user=username, passwd=password, db=database )
brow_conn = mysql.connector.connect( host=hostname, user=username, passwd=password, db=database )

#iterate through each arrival ( which are browser) look for events
arr_curr = arr_conn.cursor(buffered=True)
arr_curr.execute("select a.id, a.browser_id, a.created_at, a.arpxs_a_ref, a.gclid FROM arrivals a LEFT JOIN events e on a.id = e.arrival_id WHERE e.id IS NULL")
sessions_insert_array = []
events_insert_array = []
session_attributes_insert_array = []

for arr_row in arr_curr:
	arr_id = arr_row[0]
	arr_brow_id = arr_row[1]
	arr_created_at = arr_row[2]
	arr_arpxs_a_ref = arr_row[3]
	arr_gclid = arr_row[4]
	if arr_id < 10080:
		continue
	print 'ARRIVAL ID BEING WORKED ON {}'.format(arr_id)


	brow_curr = brow_conn.cursor(buffered=True)
	brow_curr.execute('select ip, user_agent, robot_id, created_at from browsers where id={}'.format(arr_brow_id))
	(ip,user_agent,robot_id,created_at) = brow_curr.fetchone()

	if robot_id:
        	#continue
        	sessions_insert_array.append('({},{},"{}","{}",{},"{}")'.format(arr_id,1,ip,user_agent,1, arr_created_at))

	else:
        	sessions_insert_array.append('({},{},"{}","{}",{},"{}")'.format(arr_id,1,ip,user_agent,0, arr_created_at))
        brow_curr.close()

	if arr_arpxs_a_ref:
		if len(arr_arpxs_a_ref.strip()) > 0:
                        session_attributes_insert_array.append('((SELECT id FROM sessions WHERE browser_id={} LIMIT 1),4,"{}","{}")'.format(arr_id,arr_arpxs_a_ref,arr_created_at))
	if arr_gclid:
		if len(arr_gclid.strip()) > 0:
			session_attributes_insert_array.append('((SELECT id FROM sessions WHERE browser_id={} LIMIT 1),9,"{}","{}")'.format(arr_id,arr_gclid,arr_created_at))
		
	events_insert_array.append('(10,(SELECT id FROM sessions WHERE browser_id={} LIMIT 1),0,"{}")'.format(arr_id,arr_created_at))

size = len(sessions_insert_array)/5;
sessions_part = [sessions_insert_array[i:i+size] for i  in range(0, len(sessions_insert_array), size)];

part_val = 1
for part in sessions_part:
	sessions_sql = open('sessions' + str(part_val)+'.sql','w')
	sessions_sql.write('INSERT INTO sessions(browser_id,domain_id,ip,user_agent,is_robot,created_at) VALUES {};\n'.format(",".join(part)))
	sessions_sql.close()
	part_val += 1

size = len(events_insert_array)/30;
events_part = [events_insert_array[i:i+size] for i  in range(0, len(events_insert_array), size)];

part_val = 1
for part in events_part:
	events_sql = open('events' + str(part_val)+'.sql','w')
	events_sql.write('INSERT INTO events(event_type_id,session_id,url_id,created_at) VALUES {};\n'.format(",".join(part)))
	events_sql.close()
	part_val += 1

size = len(session_attributes_insert_array)/30;
session_attributes_part = [session_attributes_insert_array[i:i+size] for i  in range(0, len(session_attributes_insert_array), size)];

part_val = 1
for part in session_attributes_part:
        session_attributes_sql = open('session_attributes' + str(part_val)+'.sql','w')
        session_attributes_sql.write('INSERT INTO session_attributes(session_id,attribute_id,value,created_at) VALUES {};\n'.format(",".join(part)))
        session_attributes_sql.close()
        part_val += 1
