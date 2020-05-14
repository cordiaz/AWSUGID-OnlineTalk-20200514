/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hagionsoft.testdonkey.java;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import java.util.*;
//import com.amazonaws.services.lambda.runtime.LambdaLogger;
        
/**
 *
 * @author nik_y
 */
public class Handler implements RequestHandler<Map<String,String>, String> {
      
    public class TopicData {
//        private String id;
        private String name;
        private List<Map<String,Integer>> metrics;
        
//        public void setId(String topicId) {
//            id = topicId;
//        }
//        public String getId() {
//            return id;
//        }
        
        public void setName(String topicName) {
            name = topicName;
        }
        public String getName() {
            return name;
        }
        
        public void setMetrics(List<Map<String,Integer>> topicMetrics) {
            metrics = topicMetrics;
        }
        public List<Map<String,Integer>> getMetrics() {
            return metrics;
        }
    }
    
    @Override
    public String handleRequest(Map<String,String> event, Context context)
    {
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard()
                            .withRegion(Regions.AP_SOUTHEAST_1)
                            .build();
        
        ScanRequest scanRequest = (new ScanRequest())
                            .withTableName("Topics")
                            .withExpressionAttributeNames(new HashMap<String, String>() {{ put("#n", "name"); }})
                            .withProjectionExpression("id, #n");
        
        ScanResult scanResult = dynamoDBClient.scan(scanRequest);
        
        List<Map<String, AttributeValue>> items = scanResult.getItems();
        
        if (items != null && items.size() > 0) {
            ArrayList<MetricDataQuery> metricDataQueries = new ArrayList<MetricDataQuery>();
            
            for (Map<String, AttributeValue> item : items) {
//                String topicId = item.get("id").getS();
                String topicName = item.get("name").getS();
                
                Dimension dimension = new Dimension();
                dimension.setName("TopicName");
                dimension.setValue(topicName);
                
                ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
                dimensions.add(dimension);
                
                Metric metric = new Metric();
                metric.setDimensions(dimensions);
                metric.setMetricName("NumberOfMessagesPublished");
                metric.setNamespace("AWS/SNS");
                
                MetricStat metricStat = new MetricStat();
                metricStat.setMetric(metric);
                metricStat.setStat("Sum");
                metricStat.setPeriod(300);
                
                MetricDataQuery metricDataQuery = new MetricDataQuery();
                metricDataQuery.setMetricStat(metricStat);
                metricDataQuery.setId("t" + topicName); //Potential duplicated names
                
                metricDataQueries.add(metricDataQuery);
            }
            
            GetMetricDataRequest getMetricDataRequest = new GetMetricDataRequest();
            getMetricDataRequest.setMetricDataQueries(metricDataQueries);
            
             //Get the closest 5 minutes
            Calendar endDate = Calendar.getInstance();
            endDate.set(Calendar.MILLISECOND, 0);
            endDate.set(Calendar.SECOND, 0);
            int minute = endDate.get(Calendar.MINUTE);
            minute = (int)Math.floor(minute / 5.0) * 5;
            endDate.set(Calendar.MINUTE, minute);

            //Set start date to be 3 hours back
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(endDate.getTime());
            startDate.set(Calendar.HOUR, startDate.get(Calendar.HOUR) - 3);
            //startDate.set(Calendar.DAY_OF_MONTH, startDate.get(Calendar.DAY_OF_MONTH) - 7);

            getMetricDataRequest.setMetricDataQueries(metricDataQueries);
            getMetricDataRequest.setStartTime(startDate.getTime());
            getMetricDataRequest.setEndTime(endDate.getTime());
            getMetricDataRequest.setScanBy(ScanBy.TimestampDescending.name());
            
            AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClientBuilder.standard()
                            .withRegion(Regions.AP_SOUTHEAST_1)
                            .build();
            GetMetricDataResult getMetricDataResult = cloudWatchClient.getMetricData(getMetricDataRequest);
            
            List<TopicData> topicDataList = new ArrayList<TopicData>();
            
            for (MetricDataResult metricDataResult : getMetricDataResult.getMetricDataResults()) {
                List<Date> timestamps = metricDataResult.getTimestamps();
                List<Double> values = metricDataResult.getValues();
                
                TopicData topicData = new TopicData();
                topicData.setName(metricDataResult.getId().substring(1));
                
                List<Map<String, Integer>> topicMetrics = new ArrayList<Map<String, Integer>>();
                for (int i = 0; i < timestamps.size(); i += 1) {
                    Date timestamp = timestamps.get(i);
                    Double value = values.get(i);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(timestamp);
                    
                    HashMap<String, Integer> metric = new HashMap<String, Integer>();
                    metric.put("year", calendar.get(Calendar.YEAR));
                    metric.put("month", calendar.get(Calendar.MONTH));
                    metric.put("day", calendar.get(Calendar.DAY_OF_MONTH));
                    metric.put("hour", calendar.get(Calendar.HOUR));
                    metric.put("minute", calendar.get(Calendar.MINUTE));
                    metric.put("value", value.intValue());
                    topicMetrics.add(metric);
                }
                topicData.setMetrics(topicMetrics);
                topicDataList.add(topicData);
            }
                        
            Gson gson = new Gson();           
            
            return gson.toJson(topicDataList);
        } else {
            return "[]";
        }
    }

}
