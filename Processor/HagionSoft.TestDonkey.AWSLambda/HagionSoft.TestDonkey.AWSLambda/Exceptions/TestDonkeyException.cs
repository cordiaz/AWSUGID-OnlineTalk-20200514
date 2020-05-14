using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

namespace HagionSoft.TestDonkey.AWSLambda.Exceptions
{
    public class TestDonkeyException : Exception
    {
        public TestDonkeyException() : base() { }

        public TestDonkeyException(string message) : base(message) { }

        public TestDonkeyException(string message, Exception innerException) : base(message, innerException) { }

        public TestDonkeyException(SerializationInfo info, StreamingContext context) : base(info, context) { }
    }
}
