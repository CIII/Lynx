#!/usr/bin/python

import mysql.connector
import urlparse
import collections 

#hostname = 'easiersolar-cluster-1.cluster-copdec0azp7r.us-west-2.rds.amazonaws.com'
#username = 'easiersolar'
#password = '3Si3rS0laR1'
#database = 'easiersolar'
hostname = 'localhost'
username = 'root'
password = ''
database = 'easiersolar'

attributes = {}

#populate the attribute dicitionary
def convert(data):
    if isinstance(data, basestring):
        return str(data)
    elif isinstance(data, collections.Mapping):
        return dict(map(convert, data.iteritems()))
    elif isinstance(data, collections.Iterable):
        return type(data)(map(convert, data))
    else:
        return data

def populate_attributes_dictionary(conn) :
    cur = conn.cursor()

    cur.execute( "SELECT name, id FROM attributes" )

    for (name, id) in cur:
        attributes[name.strip()] = id

#parse params into dictionary
def parse_params(url) :
    return dict(urlparse.parse_qsl(urlparse.urlsplit(url).query))

#loop through each event, select only id, session id , url id
conn = mysql.connector.connect( host=hostname, user=username, passwd=password, db=database )
session_att_conn = mysql.connector.connect( host=hostname, user=username, passwd=password, db=database )
populate_attributes_dictionary(conn)
attributes = convert(attributes)

cur = conn.cursor(buffered=True)
cur.execute("select session_id, url_id from events where session_id > 0 group by session_id, url_id")

print 'Processing {} sessions/url pairs'.format(cur.rowcount)
i = 0
for(session_id, url_id) in cur:
    if (i%100 == 0):
        print 'Processed {} sessions/url pairs'.format(i)
    url_conn = mysql.connector.connect( host=hostname, user=username, passwd=password, db=database )
    url_cur = url_conn.cursor()
    url_cur.execute('select url from urls where id={}'.format(url_id))
    for(row) in url_cur:
        url = row[0].replace('&amp;','&')
        params = parse_params(url)
	if len(params.items()) > 1:
		print url
		print params
	else:
		continue
        for att, val in params.items():
            try:
                att_id = attributes[att.strip()]
		print att_id
                session_att_cur = session_att_conn.cursor(buffered=True)
                session_att_cur.execute('select * from session_attributes where session_id={} and attribute_id={}'.format(session_id, att_id))
                if session_att_cur.rowcount :
                    print 'Skipping insert for attribute {}'.format(att)
                else :
                    session_att_cur.execute('insert into session_attributes(session_id,attribute_id,value,created_at,updated_at) values ("{}","{}","{}",NOW(),NOW())'.format(session_id,att_id,val))
                    session_att_conn.commit()
                session_att_cur.close()
            except KeyError:
                print 'Found rogue attribute {}'.format(att)
    url_conn.close()
    i = i + 1
