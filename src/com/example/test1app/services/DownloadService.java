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
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.example.test1app.activities.HomeActivity;

public class DownloadService extends IntentService {
	private static final String NAME = "DownloadService";

	private boolean cancelled;
	private boolean alreadyStarted = false;
	private boolean finished = false;
	private String filePath = null;

	private final IBinder binder = new DownloadServiceBinder();

	public DownloadService() {
		super(NAME);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;

		String urlString = intent.getStringExtra(HomeActivity.URL_KEY);

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

			if (!cancelled) {
				Intent outerIntent = new Intent(HomeActivity.DOWNLOAD_SERVICE_INTENT);
				outerIntent.putExtra(HomeActivity.SERVICE_MESSAGE, HomeActivity.DOWNLOAD_FINISHED);
				outerIntent.putExtra(HomeActivity.PATH_KEY, filePath);
				LocalBroadcastManager.getInstance(this).sendBroadcast(outerIntent);
				finished = true;
			}
		}
	}

	public void cancelLoad() {
		cancelled = true;
	}

	public boolean isStarted() {
		return alreadyStarted;
	}

	public boolean isFinished() {
		return finished;
	}

	public class DownloadServiceBinder extends Binder {
		public DownloadService getService() {
			return DownloadService.this;
		}
	}
}