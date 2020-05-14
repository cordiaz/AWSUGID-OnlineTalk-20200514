package com.hagionsoft.testdonkey

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate
import com.amazonaws.services.sns.AmazonSNSClient
import com.hagionsoft.testdonkey.adapters.SectionsPagerAdapter
import com.hagionsoft.testdonkey.apis.NotificationAPI
import com.hagionsoft.testdonkey.models.Notification
import com.hagionsoft.testdonkey.models.Topic
import com.hagionsoft.testdonkey.viewmodels.EditViewModel
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.collections.HashMap

class EditActivity : AppCompatActivity() {

    private lateinit var accessToken: String
    private lateinit var idToken: String
    private lateinit var topic: Topic

    private val retrofit = Retrofit.Builder()
                            .baseUrl("https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

    private val notificationAPI: NotificationAPI = retrofit.create(
        NotificationAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sectionsPagerAdapter =
            SectionsPagerAdapter(
                this,
                supportFragmentManager
            )
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        intent.getStringExtra(MainActivity.EXTRA_ACCESS_TOKEN)?.let {
            accessToken = it
        }

        intent.getStringExtra(MainActivity.EXTRA_ID_TOKEN)?.let {
            idToken = it
        }

        intent.getParcelableExtra<Topic>(TopicActivity.EXTRA_TOPIC)?.let {
            topic = it
        }

        val model: EditViewModel by viewModels()
        model.topic = topic
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.saveAction -> {
                GlobalScope.launch {
                    updateNotification()
                }
                true
            }
            R.id.deleteAction -> {
                GlobalScope.launch {
                    deleteNotification()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateTopic() {
        val wif = WebIdentityFederationSessionCredentialsProvider(
            idToken,
            null,
            "arn:aws:iam::404276529491:role/TestDonkeyAndroidRole"
        )

        //Update schedule

        //Update topic
        val dynamoDBClient = AmazonDynamoDBClient(wif)
        dynamoDBClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        val key = HashMap<String, AttributeValue>()
        key["id"] = AttributeValue().withS(topic.id)
        val updatedItem = HashMap<String, AttributeValueUpdate>()
        updatedItem["messages"] = AttributeValueUpdate().withValue(AttributeValue().withSS(topic.messages))
        val dynamoDBResult = dynamoDBClient.updateItem("Topics", key, updatedItem)
    }

    private fun updateNotification() {
        val notification = Notification(
                TopicId = topic.id,
                TopicName = topic.name,
                TopicArn = topic.arn,
                Cron = topic.cron,
                Messages = topic.messages)

        val call = notificationAPI.updateNotification(notification)

        val response = call?.execute()
        response?.let { res ->
            Log.d("TestDonkey", "Update: ${ res.isSuccessful }")
            Log.d("TestDonkey", "Body: ${ res.body() }")
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun deleteTopic() {
        val wif = WebIdentityFederationSessionCredentialsProvider(
            idToken,
            null,
            "arn:aws:iam::404276529491:role/TestDonkeyAndroidRole"
        )

        //Delete topic
        val snsClient = AmazonSNSClient(wif)
        snsClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        val snsResult = snsClient.deleteTopic(topic.arn)

        //Delete schedule

        //Save metadata
        val dynamoDBClient = AmazonDynamoDBClient(wif)
        dynamoDBClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        val key = HashMap<String, AttributeValue>()
        key["id"] = AttributeValue().withS(topic.id)
        val dynamoDBResult = dynamoDBClient.deleteItem("Topics", key)
    }

    private fun deleteNotification() {
        val call = notificationAPI.deleteNotification(topic.id)

        val response = call?.execute()
        response?.let { res ->
            Log.d("TestDonkey", "Delete: ${ res.isSuccessful }")
            Log.d("TestDonkey", "Body: ${ res.body() }")
        }

        setResult(Activity.RESULT_OK)
        finish()
    }
}