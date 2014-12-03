package com.example.test1app.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;

import com.example.test1app.activities.HomeActivity;

public class DownloadPlaybackService extends IntentService {
	private static final String NAME = "DownloadPlaybackService";

	private boolean cancelled;
	private boolean alreadyStarted = false;
	private MediaPlayer mediaPlayer;

	public DownloadPlaybackService() {
		super(NAME);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent.hasExtra(HomeActivity.ACTION_KEY)) {
			// Set the canceling flag
			cancelled = HomeActivity.CANCEL_VAL.equals(intent.getStringExtra(HomeActivity.ACTION_KEY));

		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		String filePath = null;
		boolean ready = false;
		String urlString = intent.getStringExtra(HomeActivity.URL_KEY);
		ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra(HomeActivity.RECEIVER_KEY);

		if (!alreadyStarted) {

			try {
				alreadyStarted = true;
				URL url = new URL(urlString);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				input = connection.getInputStream();

				File directory = new File(Environment.getExternalStorageDirectory() + "/");
				directory.mkdirs();

				File outputFile = new File(directory, "1.ogg");
				filePath = outputFile.getPath();

				output = new FileOutputStream(outputFile);

				byte data[] = new byte[4096];
				int count;
				while ((count = input.read(data)) != -1) {
					if (cancelled) {
						break;
					}

					output.write(data, 0, count);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (output != null) {
						output.close();
					}
					if (input != null) {
						input.close();
					}
				} catch (IOException ignored) {
					if (input != null) {
						try {
							input.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				if (connection != null) {
					connection.disconnect();
				}
			}

			try {
				mediaPlayer = new MediaPlayer();
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

				mediaPlayer.setDataSource(filePath);
				mediaPlayer.prepare();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ready = true;
			receiver.send(0, new Bundle());
		}
	}
}