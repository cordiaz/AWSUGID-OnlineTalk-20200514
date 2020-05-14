package com.hagionsoft.testdonkey.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hagionsoft.testdonkey.R
import com.hagionsoft.testdonkey.viewmodels.EditViewModel
import kotlinx.android.synthetic.main.fragment_edit.*

class EditFragment : Fragment(), AdapterView.OnItemSelectedListener  {

    private val model: EditViewModel by activityViewModels()

    private lateinit var spinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_edit, container, false)

        spinner = root.findViewById(R.id.frequencySpinner)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        topicName.setText(model.topic?.name)

        context?.let {
            ArrayAdapter.createFromResource(
                it,
                R.array.frequency_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                spinner.adapter = adapter
            }
            model.topic?.let {topic ->
                val pos = getPosition(topic.cron)
                spinner.setSelection(pos)
                if (pos > 5) {
                    cronInput.setText(topic.cron)
                }
            }
            spinner.onItemSelectedListener = this
        }

        cronInput.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                model.topic?.cron = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position < 6) {
            model.topic?.cron = createCron(position)
            cronInputLayout.visibility = View.GONE
        } else {
            cronInputLayout.visibility = View.VISIBLE
        }
    }

    private fun createCron(spinnerPosition: Int): String {
        return when(spinnerPosition) {
            0 -> "cron(0 0 * * ? *)" //Daily rate(1 day)
            1 -> "cron(0 0 ? * MON *)" //Weekly (every Monday)
            2 -> "cron(0 0 1 * ? *)" //Monthly (every 1st)
            3 -> "cron(0/5 * * * ? *)" //Every 5 minutes rate(5 minutes)
            4 ->  "cron(0/10 * * * ? *)" //Every 10 minutes rate(10 minutes)
            5 -> "cron(0/15 * * * ? *)" //Every 15 minutes rate(15 minutes)
            else -> ""
        }
    }

    private fun getPosition(cron: String): Int {
        return when(cron) {
            "cron(0 0 * * ? *)" -> 0
            "cron(0 0 ? * MON *)" -> 1
            "cron(0 0 1 * ? *)" -> 2
            "cron(0/5 * * * ? *)" -> 3
            "cron(0/10 * * * ? *)" -> 4
            "cron(0/15 * * * ? *)" -> 5
            else -> 6
        }
    }
}