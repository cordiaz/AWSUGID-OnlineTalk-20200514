package main

import (
	"fmt"
	"context"	
	"errors"
    "github.com/aws/aws-sdk-go/aws"
    "github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/sns"
	"github.com/aws/aws-lambda-go/lambda"
)

type Subscription struct {
	Topics []string `json:"topics"`
	Email string `json:"email"`
}

func HandleRequest(ctx context.Context, subscription Subscription) (string, error) {
	sess, err := session.NewSession(&aws.Config{
		Region: aws.String("ap-southeast-1")},
	)
	if err != nil {
        fmt.Println(err.Error())
        return "error", err
	}
	if len(subscription.Topics) < 1 {
		return "error", errors.New("Invalid topics")
	}
	if len(subscription.Email) < 1 {
		return "error", errors.New("Invalid email")
	}

	svc := sns.New(sess)

	for i := 0; i < len(subscription.Topics); i++ {

		sInput := &sns.SubscribeInput{
			Endpoint: aws.String(subscription.Email),
			Protocol: aws.String("email"),
			TopicArn: aws.String(subscription.Topics[i]),
		}

		svc.Subscribe(sInput)
	}
		
	return "success", nil
}

func main() {
	lambda.Start(HandleRequest)
}