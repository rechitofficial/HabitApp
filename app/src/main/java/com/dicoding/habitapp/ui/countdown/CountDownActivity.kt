package com.dicoding.habitapp.ui.countdown

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.dicoding.habitapp.R
import com.dicoding.habitapp.data.Habit
import com.dicoding.habitapp.notification.NotificationWorker
import com.dicoding.habitapp.utils.HABIT
import com.dicoding.habitapp.utils.HABIT_ID
import com.dicoding.habitapp.utils.HABIT_TITLE
import java.lang.Exception

class CountDownActivity : AppCompatActivity() {

    private lateinit var oneTimeWorkRequest: OneTimeWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_count_down)
        supportActionBar?.title = "Count Down"

        val habit = intent.getParcelableExtra<Habit>(HABIT) as Habit

        findViewById<TextView>(R.id.tv_count_down_title).text = habit.title

        val viewModel = ViewModelProvider(this).get(CountDownViewModel::class.java)

        //TODO 10 : Set initial time and observe current time. Update button state when countdown is finished
        viewModel.setInitialTime(habit.minutesFocus)
        viewModel.currentTimeString.observe(this) {
            findViewById<TextView>(R.id.tv_count_down).text = it
        }

        //TODO 13 : Start and cancel One Time Request WorkManager to notify when time is up.

        val workManager: WorkManager = WorkManager.getInstance(this)
        var isTimerStart = false
        viewModel.eventCountDownFinish.observe(this) {
            updateButtonState(!it)
            if (it && isTimerStart) {
                val data = Data.Builder()
                    .putInt(HABIT_ID, habit.id)
                    .putString(HABIT_TITLE, habit.title)
                    .build()
                oneTimeWorkRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
                    .setInputData(data)
                    .build()
                workManager.enqueue(oneTimeWorkRequest)
                workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
                    .observe(this@CountDownActivity){ workInfo ->
                        val status = workInfo.state.name
                        if(workInfo.state == WorkInfo.State.ENQUEUED) {
                            Log.d("TAG-NOTIF", "Notification has been queued. Status: $status")
                        } else if(workInfo.state == WorkInfo.State.CANCELLED) {
                            Log.d("TAG-NOTIF-ELSE", "Notification has been cancelled")
                        }
                    }
            }

        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            viewModel.startTimer()
            isTimerStart = true
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            viewModel.resetTimer()
            isTimerStart = false
            try {
                workManager.cancelWorkById(oneTimeWorkRequest.id)
                Log.d("WORK-MANAGER-CANCEL", "WorkManager cancelled successfully! ")
            } catch (e: Exception){
                Log.d("WORK-MANAGER-CANCEL-ERR", "WorkManager cancelled unsuccessfully! as a result of ${e.message} ")
            }

        }
    }

    private fun updateButtonState(isRunning: Boolean) {
        findViewById<Button>(R.id.btn_start).isEnabled = !isRunning
        findViewById<Button>(R.id.btn_stop).isEnabled = isRunning
    }
}