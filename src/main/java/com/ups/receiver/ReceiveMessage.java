package com.ups.receiver;

import java.io.IOException;
import java.util.UUID;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qvd.model.MQMessage;

@Component
public class ReceiveMessage 
{

	private static final String QUEUE_NAME = "JSON_QUEUE";

	@JmsListener(destination = QUEUE_NAME)
	public void receiveMessage(String msg) {
		System.out.println("Received :" + msg);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			MQMessage mqMessage=objectMapper.readValue(msg, MQMessage.class);
			String subScriptionId=mqMessage.getHeaderSegment().getSubscriptionId();
			String requestType=mqMessage.getHeaderSegment().getRequestType();


			CouchbaseEnvironment environment=DefaultCouchbaseEnvironment.builder().connectTimeout(10000)
					.build();
			Cluster cluster = CouchbaseCluster.create(environment,"127.0.0.1");

			cluster.authenticate("Administrator", "admin123");

			Bucket bucket = cluster.openBucket("example");

			//String id = UUID.randomUUID().toString();

			JsonDocument doc = JsonDocument.create(subScriptionId,JsonObject.fromJson(msg));


			if(requestType.equals("C"))
			{
				bucket.upsert(doc);
			}
			else if(requestType.equals("D"))
			{
				JsonDocument loaded= bucket.get(subScriptionId);
				if(loaded!=null)
				{

					bucket.remove(loaded);
				}


			}


			// Just close a single bucket
			bucket.close();

			// Disconnect and close all buckets
			cluster.disconnect();


		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
