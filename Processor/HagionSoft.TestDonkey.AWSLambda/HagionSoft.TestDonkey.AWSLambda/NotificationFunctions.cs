using Amazon;
using Amazon.CloudWatchEvents;
using Amazon.CloudWatchEvents.Model;
using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using Amazon.Lambda;
using Amazon.Lambda.APIGatewayEvents;
using Amazon.Lambda.Core;
using Amazon.SimpleNotificationService;
using Amazon.SimpleNotificationService.Model;
using HagionSoft.TestDonkey.AWSLambda.Exceptions;
using HagionSoft.TestDonkey.AWSLambda.Models;
using System;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Threading;

namespace HagionSoft.TestDonkey.AWSLambda
{
    public class NotificationFunctions
    {
        public string Add(Notification input, ILambdaContext context)
        {
            if (input == null)
            {
                throw new ArgumentNullException("Input required");
            }

            //var topicId = Guid.NewGuid().ToString();
            var region = RegionEndpoint.APSoutheast1;

            //Create topic in SNS
            var snsClient = new AmazonSimpleNotificationServiceClient(region);

            var createTopicRequest = new CreateTopicRequest();
            createTopicRequest.Name = input.TopicName;
            var createTopicResponse = snsClient.CreateTopicAsync(createTopicRequest).Result;
            if (createTopicResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't add topic");
            }

            //Create rule in CloudWatch Events
            var cloudWatchEventClient = new AmazonCloudWatchEventsClient(region);

            var putRuleRequest = new PutRuleRequest();
            putRuleRequest.ScheduleExpression = input.Cron;
            putRuleRequest.Name = $"testdonkey-rule-{ input.TopicId }";
            putRuleRequest.State = RuleState.ENABLED;

            var putRuleResponse = cloudWatchEventClient.PutRuleAsync(putRuleRequest).Result;
            if (putRuleResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't add rule");
            }

            //Set target for CloudWatch Events 
            var putTargetsRequest = new PutTargetsRequest();
            putTargetsRequest.Rule = putRuleRequest.Name;
            putTargetsRequest.Targets = new List<Target> { new Target() {
                                            Arn = "arn:aws:lambda:ap-southeast-1:404276529491:function:TestDonkeyLambda",
                                            Id = $"testdonkey-target-{ input.TopicId }",
                                            Input = $"{{\"TopicId\":\"{ input.TopicId }\"}}" } };

            var putTargetsResponse = cloudWatchEventClient.PutTargetsAsync(putTargetsRequest).Result;
            if (putTargetsResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                var deleteRuleRequest = new DeleteRuleRequest();
                deleteRuleRequest.Name = putRuleRequest.Name;
                cloudWatchEventClient.DeleteRuleAsync(deleteRuleRequest);
                throw new TestDonkeyException("Can't add target");
            }

            //Add permission to accept CloudWatch Events trigger for Lambda
            var lambdaClient = new AmazonLambdaClient(region);

            var addPermissionRequest = new Amazon.Lambda.Model.AddPermissionRequest();
            addPermissionRequest.FunctionName = "TestDonkeyLambda";
            addPermissionRequest.Action = "lambda:InvokeFunction";
            addPermissionRequest.StatementId = $"testdonkey-lambda-{ input.TopicId }";
            addPermissionRequest.Principal = "events.amazonaws.com";
            addPermissionRequest.SourceArn = putRuleResponse.RuleArn;

            var addPermissionResponse = lambdaClient.AddPermissionAsync(addPermissionRequest).Result;
            if (addPermissionResponse.HttpStatusCode != HttpStatusCode.Created)
            {
                throw new TestDonkeyException("Can't add permission");
            }

            //Save notification to DynamoDB
            var dynamoDBClient = new AmazonDynamoDBClient(region);

            var item = new Dictionary<string, AttributeValue>();
            item.Add("id", new AttributeValue() { S = input.TopicId });
            item.Add("name", new AttributeValue() { S = input.TopicName });
            item.Add("arn", new AttributeValue() { S = createTopicResponse.TopicArn });
            item.Add("cron", new AttributeValue() { S = input.Cron });

            var putItemResponse = dynamoDBClient.PutItemAsync("Topics", item).Result;
            if (putItemResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't add item");
            }

            return "success";
        }

        public string Update(Notification input, ILambdaContext context)
        {
            var region = RegionEndpoint.APSoutheast1;

            //Update rule in CloudWatch Events
            var cloudWatchEventClient = new AmazonCloudWatchEventsClient(region);

            var putRuleRequest = new PutRuleRequest();
            putRuleRequest.ScheduleExpression = input.Cron;
            putRuleRequest.Name = $"testdonkey-rule-{ input.TopicId }";
            putRuleRequest.State = RuleState.ENABLED;

            var putRuleResponse = cloudWatchEventClient.PutRuleAsync(putRuleRequest).Result;
            if (putRuleResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't update rule");
            }

            //Update notification in DynamoDB
            var dynamoDBClient = new AmazonDynamoDBClient(region);

            var key = new Dictionary<string, AttributeValue>();
            key.Add("id", new AttributeValue() { S = input.TopicId });

            var updatedItem = new Dictionary<string, AttributeValueUpdate>();
            updatedItem.Add("cron", new AttributeValueUpdate(new AttributeValue() { S = input.Cron }, AttributeAction.PUT));
            if (input.Messages == null || input.Messages.Count < 1)
            {
                updatedItem.Add("messages", new AttributeValueUpdate(null, AttributeAction.DELETE));
            }
            else
            {
                updatedItem.Add("messages", new AttributeValueUpdate(new AttributeValue() { SS = input.Messages }, AttributeAction.PUT));
            }
            

            var putItemResponse = dynamoDBClient.UpdateItemAsync("Topics", key, updatedItem).Result;
            if (putItemResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't update item");
            }

            return "success";
        }

        public APIGatewayProxyResponse Delete(APIGatewayProxyRequest input, ILambdaContext context)
        {
            var region = RegionEndpoint.APSoutheast1;

            //Assume ?topic_id=...
            var topicId = input.QueryStringParameters["topic_id"];
            if (string.IsNullOrWhiteSpace(topicId))
            {
                throw new TestDonkeyException("id is required");
            }

            Remove(new Notification() { TopicId = topicId }, context);

            //https://aws.amazon.com/blogs/compute/developing-net-core-aws-lambda-functions/
            var response = new APIGatewayProxyResponse
            {
                StatusCode = (int)HttpStatusCode.OK,
                Body = "\"success\"",
                Headers = new Dictionary<string, string>
                {
                    { "Content-Type", "application/json" }
                }
            };

            return response;
        }

        public string Remove(Notification input, ILambdaContext context)
        {
            var region = RegionEndpoint.APSoutheast1;

            var topicId = input.TopicId;

            //Get item from DynamoDB
            var key = new Dictionary<string, AttributeValue>();
            key.Add("id", new AttributeValue() { S = topicId });

            var dynamoDBClient = new AmazonDynamoDBClient(region);
            var getItemResponse = dynamoDBClient.GetItemAsync("Topics", key).Result;
            if (getItemResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't find item");
            }

            var item = getItemResponse.Item;

            var topicArn = item.GetValueOrDefault("arn").S;

            //Remove permission to accept CloudWatch Events trigger for Lambda
            var lambdaClient = new AmazonLambdaClient(region);

            var removePermissionRequest = new Amazon.Lambda.Model.RemovePermissionRequest();
            removePermissionRequest.FunctionName = "TestDonkeyLambda";
            removePermissionRequest.StatementId = $"testdonkey-lambda-{ topicId }";

            var removePermissionResponse = lambdaClient.RemovePermissionAsync(removePermissionRequest).Result;
            if (removePermissionResponse.HttpStatusCode != HttpStatusCode.NoContent)
            {
                throw new TestDonkeyException("Can't remove permission");
            }

            //Remove target for CloudWatch Events
            var cloudWatchEventClient = new AmazonCloudWatchEventsClient(region);

            var removeTargetsRequest = new RemoveTargetsRequest();
            removeTargetsRequest.Ids = new List<string> { $"testdonkey-target-{ topicId }" };
            removeTargetsRequest.Rule = $"testdonkey-rule-{ topicId }";

            var removeTargetsResponse = cloudWatchEventClient.RemoveTargetsAsync(removeTargetsRequest).Result;
            if (removeTargetsResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't remove target");
            }

            //Delete rule in CloudWatch Events
            var deleteRuleRequest = new DeleteRuleRequest();
            deleteRuleRequest.Name = removeTargetsRequest.Rule;

            var deleteRuleResponse = cloudWatchEventClient.DeleteRuleAsync(deleteRuleRequest).Result;
            if (deleteRuleResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't delete rule");
            }

            //Remove subscribers from SNS
            var snsClient = new AmazonSimpleNotificationServiceClient(region);

            var listSubscriptionsByTopicRequest = new ListSubscriptionsByTopicRequest();
            listSubscriptionsByTopicRequest.TopicArn = topicArn;

            ListSubscriptionsByTopicResponse listSubscriptionsByTopicResponse = null;

            do
            {
                listSubscriptionsByTopicResponse = snsClient.ListSubscriptionsByTopicAsync(listSubscriptionsByTopicRequest).Result;
                if (listSubscriptionsByTopicResponse.HttpStatusCode != HttpStatusCode.OK)
                {
                    throw new TestDonkeyException("Can't list subscriptions");
                }

                if (listSubscriptionsByTopicResponse.Subscriptions != null && listSubscriptionsByTopicResponse.Subscriptions.Count > 0)
                {
                    foreach (var subscription in listSubscriptionsByTopicResponse.Subscriptions)
                    {
                        if (!subscription.SubscriptionArn.Equals("pendingconfirmation", StringComparison.OrdinalIgnoreCase))
                        {
                            snsClient.UnsubscribeAsync(subscription.SubscriptionArn).GetAwaiter().GetResult();
                        }                        
                    }
                }

                listSubscriptionsByTopicRequest.NextToken = listSubscriptionsByTopicResponse.NextToken;

                Thread.Sleep(1_000); //Wait for 1 second. Throttle: 100 transactions per second (TPS)
            } while (!string.IsNullOrWhiteSpace(listSubscriptionsByTopicResponse.NextToken));

            //Delete topic from SNS 
            var deleteTopicResponse = snsClient.DeleteTopicAsync(topicArn).Result;
            if (deleteTopicResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't delete topic");
            }

            //Delete item from DynamoDB
            var dynamoDBDeleteItemResponse = dynamoDBClient.DeleteItemAsync("Topics", key).Result;
            if (dynamoDBDeleteItemResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException("Can't delete item");
            }

            return "success";
        }
    }
}
