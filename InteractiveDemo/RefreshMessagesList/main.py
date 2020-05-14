import boto3
import json

def lambda_handler(event, context):     
    dynamodb = boto3.resource('dynamodb', region_name='ap-southeast-1')
    table = dynamodb.Table('Topics')
    ean = { "#n": "name", }
    pe = "id, #n, messages"
    response = table.scan(
        ExpressionAttributeNames=ean,
        ProjectionExpression=pe)
    topics = list(map(lambda item: {"id": item["id"], "name": item["name"], "messages": list(item["messages"])}, response['Items']))
    return json.dumps(topics)