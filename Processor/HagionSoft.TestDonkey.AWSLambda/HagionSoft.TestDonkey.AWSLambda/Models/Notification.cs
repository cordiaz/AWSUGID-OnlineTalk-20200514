using System;
using System.Collections.Generic;
using System.Text;

namespace HagionSoft.TestDonkey.AWSLambda.Models
{
    public class Notification
    {
        public string TopicId { get; set; }
        public string TopicName { get; set; }
        public string TopicArn { get; set; }
        public string Cron { get; set; }
        public List<string> Messages { get; set; }
    }
}
