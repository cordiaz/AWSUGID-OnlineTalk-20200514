// Load the AWS SDK for Node.js
var AWS = require('aws-sdk');

exports.handler =  function(event, context, callback) {
    // Set region
    AWS.config.update({region: 'ap-southeast-1'});

    // Create promise and SNS service object
    // return new AWS.SNS({apiVersion: '2010-03-31'}).listTopics({}).promise();

    var dynamoDBClient = new AWS.DynamoDB.DocumentClient();

    var params = {
        TableName : "Topics",
        ExpressionAttributeNames: {
            "#n": "name"
        },
        ProjectionExpression: "#n, arn"
    };

    dynamoDBClient.scan(params, callback);
}

