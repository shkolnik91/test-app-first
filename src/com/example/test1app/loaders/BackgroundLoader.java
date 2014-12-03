package com.example.test1app.loaders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;


public class BackgroundLoader extends SampleLoader<Object> {
	private String filePath = null;
	private String urlString;
	private MediaPlayer mediaPlayer;
	private boolean ready;

	public BackgroundLoader(Context context, String url) {
		super(context);
		this.urlString = url;
	}

	@Override
	public Object loadInBackground() {
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		filePath = null;
		ready = false;

		try {
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
				if (isLoadInBackgroundCanceled()) {
					return null;
				}

				output.write(data, 0, count);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
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
		return filePath;
	}

	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	public boolean isPlayerReady() {
		return ready;
	}
}