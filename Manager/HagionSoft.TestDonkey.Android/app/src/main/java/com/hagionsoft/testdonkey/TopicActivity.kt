package com.hagionsoft.testdonkey

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.hagionsoft.testdonkey.apis.NotificationAPI
import com.hagionsoft.testdonkey.models.Topic
import kotlinx.android.synthetic.main.activity_topic.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class TopicActivity : AppCompatActivity(), TopicAdapter.ItemClickListener {

    private lateinit var accessToken: String
    private lateinit var idToken: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: TopicAdapter
    private lateinit var viewManager: LinearLayoutManager

    private var topicList = mutableListOf<Topic>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val notificationAPI: NotificationAPI = retrofit.create(
        NotificationAPI::class.java)


    companion object {
        const val EXTRA_TOPIC = "topic"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic)
        setSupportActionBar(toolbar)

        intent.getStringExtra(MainActivity.EXTRA_ACCESS_TOKEN)?.let {
            accessToken = it
        }

        intent.getStringExtra(MainActivity.EXTRA_ID_TOKEN)?.let {
            idToken = it
        }

        fab.setOnClickListener {
//            Snackbar.make(view, idToken, Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()

            val intent = Intent(this@TopicActivity, AddActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_ACCESS_TOKEN, accessToken)
            intent.putExtra(MainActivity.EXTRA_ID_TOKEN, idToken)

            startActivityForResult(intent, 0)
        }

        viewManager = LinearLayoutManager(this)
        viewAdapter = TopicAdapter()
        viewAdapter.setOnClickListener(this)

        recyclerView = findViewById<RecyclerView>(R.id.topicRecyclerView).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            //setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            viewManager.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)

        GlobalScope.launch(Dispatchers.Main) {
            topicList = getTopics()
            viewAdapter.setTopics(topicList)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_topic, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.logoutAction -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_CLEAR_CREDENTIALS, true)

                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (arrayOf(0, 1).any{c -> requestCode == c} && resultCode == RESULT_OK) {
            GlobalScope.launch(Dispatchers.Main) {
                topicList = getTopics()
                viewAdapter.setTopics(topicList)
            }
        }

    }

    private suspend fun getTopics() : MutableList<Topic> {
        return withContext(Dispatchers.IO) {
            val wif = WebIdentityFederationSessionCredentialsProvider(
                idToken,
                null,
                "arn:aws:iam::404276529491:role/TestDonkeyAndroidRole"
            )
            val dynamoDBClient = AmazonDynamoDBClient(wif)
            dynamoDBClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
            val scanRequest = ScanRequest("Topics")
            val result = dynamoDBClient.scan(scanRequest)
            val topics = mutableListOf<Topic>()
            for (item in result.items) {
                val id = item.getValue("id").s
                val name = item.getValue("name").s
                val arn = item.getValue("arn").s
                val cron = if (item.containsKey("cron")) { item.getValue("cron").s } else { "cron(0 0 * * ? *)" }
                val messages = if (item.containsKey("messages")) { item.getValue("messages").ss } else { mutableListOf<String>() }
                val topic = Topic(id = id,
                                  name = name,
                                  arn = arn,
                                  cron = cron,
                                  messages = messages)
                topics.add(topic)
            }
            topics
        }
    }

    override fun onItemClick(view: View?, position: Int) {
        val topic = topicList[position]
        val intent = Intent(this@TopicActivity, EditActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_ACCESS_TOKEN, accessToken)
        intent.putExtra(MainActivity.EXTRA_ID_TOKEN, idToken)
        intent.putExtra(TopicActivity.EXTRA_TOPIC, topic)

        startActivityForResult(intent, 1)
    }

    override fun onLongItemClick(view: View?, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Remove notification?")
            .setPositiveButton(R.string.ok_button) { dialog, id ->

                GlobalScope.launch(Dispatchers.Main) {
                    val topic = topicList[position]
                    val job = launch(Dispatchers.IO) {
                        removeNotification(topic.id)
                    }
                    job.join()
                    topicList = getTopics()
                    viewAdapter.setTopics(topicList)
                }

                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel_button) { dialog, id ->
                dialog.cancel()
            }
        builder.create().show()
    }

    private fun removeNotification(topicId: String) {
        val call = notificationAPI.removeNotification(topicId)

        val response = call?.execute()
        response?.let { res ->
            Log.d("TestDonkey", "Remove: ${ res.isSuccessful }")
            Log.d("TestDonkey", "Body: ${ res.body() }")
        }
    }
}

class TopicAdapter : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    private var topicDataset = mutableListOf<Topic>()
    private var clickListener: ItemClickListener? = null

    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onLongItemClick(view: View?, position: Int)
    }

    inner class TopicViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.name)
        init {
            view.setOnClickListener {
                clickListener?.onItemClick(it, adapterPosition)
            }
            view.setOnLongClickListener {
                clickListener?.onLongItemClick(it, adapterPosition)
                true
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicAdapter.TopicViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_topic, parent, false)

        return TopicViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.text = topicDataset[position].name
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = topicDataset.size

    fun setTopics(topics: MutableList<Topic>) {
        topicDataset = topics
        notifyDataSetChanged()
    }

    fun setOnClickListener(listener: ItemClickListener) {
        this.clickListener = listener
    }
}

