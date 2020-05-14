package com.hagionsoft.testdonkey

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.sns.AmazonSNSClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hagionsoft.testdonkey.apis.NotificationAPI
import com.hagionsoft.testdonkey.models.Notification
import kotlinx.android.synthetic.main.activity_add.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.collections.HashMap


class AddActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var accessToken: String
    private lateinit var idToken: String

    private lateinit var topicName: TextInputEditText
    private lateinit var spinner: Spinner
    private lateinit var cronInputLayout: TextInputLayout
    private lateinit var cronInput: TextInputEditText

    private val retrofit = Retrofit.Builder()
                                .baseUrl("https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()

    private val notificationAPI: NotificationAPI = retrofit.create(
        NotificationAPI::class.java)

    private var cron: String = createCron(spinnerPosition = 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        topicName = findViewById(R.id.topicName)

        intent.getStringExtra(MainActivity.EXTRA_ACCESS_TOKEN)?.let {
            accessToken = it
        }

        intent.getStringExtra(MainActivity.EXTRA_ID_TOKEN)?.let {
            idToken = it
        }

        spinner = findViewById(R.id.frequencySpinner)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.frequency_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = this

        cronInputLayout = findViewById(R.id.cronInputLayout)
        cronInput = findViewById(R.id.cronInput)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_add, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.saveAction -> {
                GlobalScope.launch {
                    //saveTopic()
                    addNotification()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveTopic() {
        val wif = WebIdentityFederationSessionCredentialsProvider(
            idToken,
            null,
            "arn:aws:iam::404276529491:role/TestDonkeyAndroidRole"
        )

        val name = topicName.text.toString()

        //Create topic
        val snsClient = AmazonSNSClient(wif)
        snsClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        val snsResult = snsClient.createTopic(name)

        //Create schedule
        //No CloudWatch Events and Lambda API for Android! 悔しい。。。(Ծ‸ Ծ)
        //Retrofit + API Gateway + Lambda to the rescue!


        //Save metadata
        val dynamoDBClient = AmazonDynamoDBClient(wif)
        dynamoDBClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        val item = HashMap<String, AttributeValue>()
        item["id"] = AttributeValue().withS(UUID.randomUUID().toString())
        item["name"] = AttributeValue().withS(name)
        item["arn"] = AttributeValue().withS(snsResult.topicArn)

        dynamoDBClient.putItem("Topics", item)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun addNotification() {
        val name = topicName.text.toString()
        if (spinner.selectedItemPosition > 5) {
            cron = cronInput.text.toString()
        }

        val notification = Notification(TopicName = name, Cron = cron)

        val call = notificationAPI.createNotification(notification)

        val response = call?.execute()
        response?.let { res ->
            Log.d("TestDonkey", "Add: ${ res.isSuccessful }")
            Log.d("TestDonkey", "Body: ${ res.body() }")
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun createCron(spinnerPosition: Int): String {
        return when(spinnerPosition) {
            0 -> { //Daily
                "cron(0 0 * * ? *)" //rate(1 day)
            }
            1 -> { //Weekly (every Monday)
                "cron(0 0 ? * MON *)"
            }
            2 -> { //Monthly (every 1st)
                "cron(0 0 1 * ? *)"
            }
            3 -> { //Every 5 minutes
                "cron(0/5 * * * ? *)" //rate(5 minutes)
            }
            4 -> { //Every 10 minutes
                "cron(0/10 * * * ? *)" //rate(10 minutes)
            }
            5 -> { //Every 15 minutes
                "cron(0/15 * * * ? *)" //rate(15 minutes)
            }
            else -> ""
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position < 6) {
            cron = createCron(position)
            cronInputLayout.visibility = View.GONE
        } else {
            cronInputLayout.visibility = View.VISIBLE
        }
    }
}
