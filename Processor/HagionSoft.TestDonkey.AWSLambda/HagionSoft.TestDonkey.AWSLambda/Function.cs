using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Amazon;
using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using Amazon.Lambda.Core;
using Amazon.SimpleNotificationService;
using Amazon.SimpleNotificationService.Model;
using HagionSoft.TestDonkey.AWSLambda.Exceptions;
using HagionSoft.TestDonkey.AWSLambda.Models;

// Assembly attribute to enable the Lambda function's JSON input to be converted into a .NET class.
[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.SystemTextJson.LambdaJsonSerializer))]

namespace HagionSoft.TestDonkey.AWSLambda
{
    public class Function
    {
        
        /// <summary>
        /// A simple function that takes a string and does a ToUpper
        /// </summary>
        /// <param name="input"></param>
        /// <param name="context"></param>
        /// <returns></returns>
        public string FunctionHandler(Models.Topic input, ILambdaContext context)
        {
            if (input == null || string.IsNullOrWhiteSpace(input.TopicId))
            {
                throw new ArgumentNullException("Input required");
            }

            var key = new Dictionary<string, AttributeValue>();
            key.Add("id", new AttributeValue() { S = input.TopicId });

            var dynamoDbClient = new AmazonDynamoDBClient(RegionEndpoint.APSoutheast1);
            var item = dynamoDbClient.GetItemAsync("Topics", key).Result.Item;

            var topicName = item.GetValueOrDefault("name").S;
            var topicArn = item.GetValueOrDefault("arn").S;
            var messages = item.GetValueOrDefault("messages")?.SS;

            var message = string.Empty;
            if (messages != null && messages.Count > 0)
            {
                if (messages.Count > 1)
                {
                    var random = new Random();
                    var randomIndex = random.Next(0, messages.Count - 1);
                    message = messages[randomIndex];
                }
                else
                {
                    message = messages[0];
                }
            }
            else
            {
                return "failed";
            }

            var snsClient = new AmazonSimpleNotificationServiceClient(RegionEndpoint.APSoutheast1);
            var publishRequest = new PublishRequest();
            publishRequest.Message = message;
            publishRequest.Subject = $"AWSUGID Test Email Notification [Topic: {topicName}]";
            publishRequest.TopicArn = topicArn;
            var publishResponse = snsClient.PublishAsync(publishRequest).Result;
            if (publishResponse.HttpStatusCode != HttpStatusCode.OK)
            {
                throw new TestDonkeyException(publishResponse.HttpStatusCode.ToString());
            }

            return "success";
        }
    }
}
