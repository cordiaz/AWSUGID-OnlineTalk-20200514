require 'aws-sdk'


$snsClient = Aws::SNS::Client.new(region: 'ap-southeast-1')
$sns = Aws::SNS::Resource.new(client: $snsClient)

def handler(event:, context:)
    topics = Array.new
    $sns.topics().each do |topic|
        resp = $snsClient.list_subscriptions_by_topic({topic_arn: topic.arn})
        resp.subscriptions.each do |s|
            if s.endpoint == event['email']
                confirm = 'confirmed'
                if s.subscription_arn == 'PendingConfirmation'
                    confirm = 'pending confirmation'
                end
                topics << "#{topic.arn.split(':').last} (#{confirm})"
                break
            end
        end

        # Notes: Crash on pending subscription due to invalid arn
        # topic.subscriptions().each do |s|
        #     if s.attributes['Endpoint'] == event['email']
        #         confirm = 'confirmed'
        #         if s.arn == 'PendingConfirmation'
        #             confirm = 'pending confirmation'
        #         end
        #         topics << "#{topic.arn.split(':').last} (#{confirm})"
        #         break
        #     end
        # end
    end
    topics
end
