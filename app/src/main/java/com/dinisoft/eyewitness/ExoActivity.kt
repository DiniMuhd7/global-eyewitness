// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.dinisoft.eyewitness

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.layout_exo.*
import java.io.File

/**
 * Created by Shamsudeen A. Muhammed (c) 2021
 */

class ExoActivity : AppCompatActivity(), Player.EventListener {

        private lateinit var simpleExoplayer: SimpleExoPlayer
        private var playbackPosition: Long = 0

        private val mp4Url = "https://html5demos.com/assets/dizzy.mp4"
        private val dashUrl = "https://storage.googleapis.com/wvmedia/clear/vp9/tears/tears_uhd.mpd"
        //private val urlList = listOf(mp4Url to "Choose Bundle", dashUrl to "dash")


        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.layout_exo)

                /*
                // Get string extra and initialize exo player
                val mItemSelected = intent.getStringExtra("Tag")
                if (mItemSelected.endsWith(".mp4")) {
                        val localFile = File(mItemSelected)
                        val localUriPath = Uri.fromFile(localFile)
                        initializePlayer(localUriPath)
                        //Toast.makeText(this, mItemSelected, Toast.LENGTH_LONG).show()
                } else if (mItemSelected.endsWith(".mp3")) {
                        val localFile = File(mItemSelected)
                        val localUriPath = Uri.fromFile(localFile)
                        initializeAudioPlayer(localUriPath)
                        //Toast.makeText(this, mItemSelected, Toast.LENGTH_LONG).show()
                } else {

                }
                */

        }


        private val dataSourceFactory: DataSource.Factory by lazy {
                DefaultDataSourceFactory(this, "exoplayer-sample")
        }

        fun initializeAudioPlayer(mURLString: Uri) {
                //val urlList = listOf(mURLString to "Choose Bundle", mURLString to "dash")
                simpleExoplayer = SimpleExoPlayer.Builder(this).build()
                //val randomUrl = urlList.random()
                preparePlayer(mURLString,"mp3")
                exoplayerView.player = simpleExoplayer
                simpleExoplayer.seekTo(playbackPosition)
                simpleExoplayer.playWhenReady = true
                simpleExoplayer.addListener(this)
        }

        fun initializeVideoPlayer(mURLString: Uri) {
                //val urlList = listOf(mURLString to "Choose Bundle", mURLString to "dash")
                simpleExoplayer = SimpleExoPlayer.Builder(this).build()
                //val randomUrl = urlList.random()
                preparePlayer(mURLString,"mp4")
                exoplayerView.player = simpleExoplayer
                simpleExoplayer.seekTo(playbackPosition)
                simpleExoplayer.playWhenReady = true
                simpleExoplayer.addListener(this)
        }

        private fun buildMediaSource(uri: Uri, type: String): MediaSource {
                return if (type == "dash") {
                        DashMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(uri)
                } else {
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(uri)
                }
        }

        fun preparePlayer(videoUrl: Uri, type: String) {
                val uri = Uri.parse(videoUrl.toString())
                val mediaSource = buildMediaSource(uri, type)
                simpleExoplayer.prepare(mediaSource)
        }

        private fun releasePlayer() {
                playbackPosition = simpleExoplayer.currentPosition
                simpleExoplayer.stop()
                simpleExoplayer.release()
        }

        override fun onPlayerError(error: ExoPlaybackException) {
                // handle error

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING)
                        progressBar.visibility = View.VISIBLE
                else if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED)
                        progressBar.visibility = View.INVISIBLE
        }


        override fun onStart() {
                super.onStart()
                // Get string extra and initialize exo player
                val mItemSelected = intent.getStringExtra("Tag")
                if (mItemSelected!!.endsWith(".mp4")) {
                        val localFile = File(mItemSelected)
                        val localUriPath = Uri.fromFile(localFile)
                        initializeVideoPlayer(localUriPath)
                        //Toast.makeText(this, mItemSelected, Toast.LENGTH_LONG).show()
                } else if (mItemSelected!!.endsWith(".mp3")) {
                        val localFile = File(mItemSelected)
                        val localUriPath = Uri.fromFile(localFile)
                        initializeAudioPlayer(localUriPath)
                        //Toast.makeText(this, mItemSelected, Toast.LENGTH_LONG).show()
                } else {

                }

        }

        override fun onResume() {
                super.onResume()
        }

        override fun onStop() {
                super.onStop()
                releasePlayer()
                //Get string extra and initialize exo player
                val mItemSelected = intent.getStringExtra("Tag")
                val localFile = File(mItemSelected)
                intent.removeExtra("Tag")
                MapActivity().deleteFileForce(localFile)
                if (mItemSelected!!.endsWith(".mp4")) {
                        val localFile = File(mItemSelected)
                        intent.removeExtra("Tag")
                        MapActivity().deleteFileForce(localFile)
                } else {
                        val localFile = File(mItemSelected)
                        intent.removeExtra("Tag")
                        MapActivity().deleteFileForce(localFile)
                }
        }


}