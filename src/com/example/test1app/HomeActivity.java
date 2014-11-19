package com.example.test1app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class HomeActivity extends Activity {
	private static String path;
	private ProgressDialog dialog;
	private DownloadTask downloadTask;
	private PlaybackTask playbackTask;
	private static MediaPlayer mediaPlayer;
	private static boolean mediaPlayerStarted = false;
	private Button button;
	private TextView label;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		button = (Button) findViewById(R.id.play_button);
		label = (TextView) findViewById(R.id.text_view);
		label.setText(R.string.home_idle);

		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} else {
			if (mediaPlayer.isPlaying()) {
				label.setText(R.string.home_playing);
				button.setText(R.string.button_pause);
			}
		}

		createOrRestoreDownloadTask();

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.isEnabled()) {
					HomeActivity.this.onClick();
				}

			}

		});
	}

	private void createOrRestoreDownloadTask() {
		Object instance = getLastNonConfigurationInstance();

		dialog = new ProgressDialog(this);
		if ((instance != null) && (instance instanceof DownloadTask)) {
			downloadTask = (DownloadTask) instance;
			downloadTask.setDialog(dialog);
			downloadTask.setButton(button);
			downloadTask.setLabel(label);

		} else {
			downloadTask = new DownloadTask(dialog);
			downloadTask.setButton(button);
			downloadTask.setLabel(label);
			downloadTask.execute(getResources().getText(R.string.home_url).toString());
		}

		if (AsyncTask.Status.FINISHED.equals(downloadTask.getStatus())) {
			label.setText(R.string.home_idle);
			button.setClickable(true);
			button.setEnabled(true);
			return;
		}

		button.setClickable(false);
		button.setEnabled(false);
		label.setText(R.string.home_downloading);
		dialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void onClick() {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			button.setText(R.string.button_play);
			label.setText(R.string.home_idle);
		} else {
			if (!mediaPlayerStarted) {
				playbackTask = new PlaybackTask();
				playbackTask.execute(path);
			} else {
				mediaPlayer.start();
			}
			button.setText(R.string.button_pause);
			label.setText(R.string.home_playing);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}

		downloadTask.setDialog(null);
		downloadTask.setButton(null);
		downloadTask.setLabel(null);

		if (downloadTask != null) {
			return downloadTask;
		}

		return super.onRetainNonConfigurationInstance();
	}

	private class DownloadTask extends AsyncTask<String, Void, String> {
		private ProgressDialog dialog;
		private Button button;
		private TextView label;

		public DownloadTask(ProgressDialog dialog) {
			setDialog(dialog);
		}

		@Override
		protected String doInBackground(String... params) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			String filePath = null;

			try {
				URL url = new URL(params[0]);
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
				int batchNum = 0;
				while ((count = input.read(data)) != -1) {
					if (isCancelled()) {
						input.close();
						output.close();
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
				}

				if (connection != null) {
					connection.disconnect();
				}
			}

			return filePath;
		}

		@Override
		protected void onPostExecute(String result) {
			button.setClickable(true);
			button.setEnabled(true);
			label.setText(R.string.home_idle);
			path = result;

			dialog.dismiss();
		}

		public void setDialog(ProgressDialog dialog) {
			this.dialog = dialog;
		}

		public void setButton(Button button) {
			this.button = button;
		}

		public void setLabel(TextView label) {
			this.label = label;
		}
	}

	private class PlaybackTask extends AsyncTask<String, Void, Object> {
		@Override
		protected void onPreExecute() {

		}

		@Override
		protected Object doInBackground(String... params) {
			try {
				if (!mediaPlayer.isPlaying()) {
					mediaPlayer.setDataSource(path);
					mediaPlayer.prepare();
					mediaPlayer.start();
					mediaPlayerStarted = true;
				}
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

			return null;
		}
	}
}