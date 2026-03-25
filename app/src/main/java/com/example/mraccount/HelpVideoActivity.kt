package com.example.mraccount

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class HelpVideoActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_video)

        videoView = findViewById(R.id.videoViewTutorial)
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnPause = findViewById<Button>(R.id.btnPause)

        // Set video path from res/raw/tutorial.mp4
        val videoPath = "android.resource://" + packageName + "/" + R.raw.tutorial
        val uri = Uri.parse(videoPath)
        videoView.setVideoURI(uri)

        btnPlay.setOnClickListener {
            videoView.start()
        }

        btnPause.setOnClickListener {
            if (videoView.isPlaying) {
                videoView.pause()
            }
        }
    }
}
