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

sessions_sql = open('sessions.sql','w')
events_update_sql_ct = 1
events_in_sql = 0
events_update_sql = open('events_update_' + str(events_update_sql_ct) + '.sql', 'w')

#iterate through each arrival ( which are browser) look for events
arr_curr = arr_conn.cursor(buffered=True)
arr_curr.execute("select id, browser_id, created_at from arrivals")
sessions_insert_array = []
events_update_array = []
for arr_row in arr_curr:
	if(events_in_sql > 20000):
		events_update_sql.close()
		events_update_sql_ct += 1
		events_update_sql = open('events_update_' + str(events_update_sql_ct) + '.sql', 'w')
		events_in_sql = 0
	arr_id = arr_row[0]
	arr_brow_id = arr_row[1]
	arr_created_at = arr_row[2]
	if arr_id < 10080:
		continue
	print 'ARRIVAL ID BEING WORKED ON {}'.format(arr_id)

	#sort events found by created at date
	event_curr = event_conn.cursor(buffered=True)
	event_curr.execute('select id, created_at from events where arrival_id={} order by created_at asc'.format(arr_id))

	events = []
	for event_row in event_curr:
		events.append(event_row)
	if len(events) < 1:
		continue
#	start_time = datetime.strptime(events[0][1], "%d/%m/%Y %H:%M:%S")
#	end_time = datetime.strptime(events[-1][1], "%d/%m/%Y %H:%M:%S")
	duration = (events[-1][1] - events[0][1]).total_seconds()

	print 'PROCESSING EVENTS - START TIME {} END TIME {}'.format(events[0][1], events[-1][1])
	if duration > 86400:
		print 'PROCESSING EVENTS - NEED TO HAVE MULTIPLE SESSIONS {}'.format(duration)
		buckets = events_into_days(events)
		start_time = events[0][1] 
		for bucket in xrange(len(buckets)):
			#create a session based on days(buckets away) from first day/bucket
			time = start_time + datetime.timedelta(days=bucket)
			brow_curr = brow_conn.cursor(buffered=True)
                        brow_curr.execute('select ip, user_agent, robot_id, created_at from browsers where id={}'.format(arr_brow_id))
                        (ip,user_agent,robot_id,created_at) = brow_curr.fetchone()
			#robot
			if len(buckets[bucket]) == 0:
				continue
                        if robot_id:
                                #continue
                                sessions_insert_array.append('({},{},"{}","{}",{},"{}")'.format(arr_id,1,ip,user_agent,1, time))

                        else:
                                sessions_insert_array.append('({},{},"{}","{}",{},"{}")'.format(arr_id,1,ip,user_agent,0, time)) 
                        brow_curr.close()
			events_update_sql.write('UPDATE events SET session_id=(SELECT id FROM sessions WHERE browser_id={} AND created_at="{}" LIMIT 1) WHERE id IN ({});\n'
				.format(arr_id,time,",".join(str(x) for (x,y) in buckets[bucket])))
                events_in_sql += len(events)
	else:
		print 'PROCESSING EVENTS - DO NOT NEED TO HAVE MULTIPLE SESSIONS'
		#create session per arrival

		if arr_brow_id:
			print 'PROCESSING ARRIVALS HAS BROWSER ID'
			brow_curr = brow_conn.cursor(buffered=True)
			brow_curr.execute('select ip, user_agent, robot_id, created_at from browsers where id={}'.format(arr_brow_id))
			(ip,user_agent,robot_id,created_at) = brow_curr.fetchone()

			if robot_id:
                                #continue
                                sessions_insert_array.append('({},{},"{}","{}",{},"{}")'.format(arr_id,1,ip,user_agent,1, events[0][1]))

			else:
				sessions_insert_array.append('({},{},"{}","{}",{},"{}")'.format(arr_id,1,ip,user_agent,0, events[0][1]))
			brow_curr.close()
		else:
			print 'PROCESSING ARRIVALS NO BROWSER ID'
			continue
			sessions_insert_array.append('({},{},"unknown","unknown",{},"{}")'.format(arr_id,1,0, created_at))

		events_update_sql.write('UPDATE events SET session_id=(SELECT id FROM sessions WHERE browser_id={} LIMIT 1) WHERE id IN ({});\n'.format(arr_id,",".join(str(x) for (x,y) in events)))
		events_in_sql += len(events)
		
size = len(sessions_insert_array)/5;
sessions_part = [sessions_insert_array[i:i+size] for i  in range(0, len(sessions_insert_array), size)];

part_val = 1
for part in sessions_part:
	sessions_sql = open('sessions' + str(part_val)+'.sql','w')
	sessions_sql.write('INSERT INTO sessions(browser_id,domain_id,ip,user_agent,is_robot,created_at) VALUES {};\n'.format(",".join(part)))
	sessions_sql.close()
	part_val += 1
