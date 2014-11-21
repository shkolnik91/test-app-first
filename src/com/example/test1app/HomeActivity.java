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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
	private static final int DIALOG_ID = 123;
	private static final String PATH = "path";
	private static final String PLAYER_STARTED = "player_started";
	private static final String TASK_CANCELLED = "task_cancelled";

	private String path;
	private DownloadTask downloadTask;
	private PlaybackTask playbackTask;
	private MediaPlayer mediaPlayer;
	private boolean mediaPlayerStarted;
	private boolean wasCancelled;
	private Button button;
	private TextView label;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		if (savedInstanceState != null) {
			mediaPlayerStarted = savedInstanceState.getBoolean(PLAYER_STARTED);
			wasCancelled = savedInstanceState.getBoolean(TASK_CANCELLED);
			path = savedInstanceState.getString(PATH);
		}

		button = (Button) findViewById(R.id.play_button);
		label = (TextView) findViewById(R.id.text_view);
		label.setText(R.string.home_idle);

		Object instance = getLastNonConfigurationInstance();

		if (instance instanceof InstanceObjects) {
			downloadTask = ((InstanceObjects) instance).getDownloadTask();
			playbackTask = ((InstanceObjects) instance).getPlaybackTask();
			mediaPlayer = ((InstanceObjects) instance).getMediaPlayer();
		}

		if (downloadTask == null) {
			if ((path == null) || (wasCancelled)) {
				downloadTask = new DownloadTask(this);

				downloadTask.execute(getResources().getText(R.string.home_url).toString());
			}
		} else {
			downloadTask.setActivity(this);
		}

		if (((downloadTask == null) || (AsyncTask.Status.FINISHED.equals(downloadTask.getStatus()))) && (!wasCancelled)) {
			label.setText(R.string.home_idle);
			button.setClickable(true);
			button.setEnabled(true);
		} else {
			button.setClickable(false);
			button.setEnabled(false);
			label.setText(R.string.home_downloading);
		}

		if ((mediaPlayer == null) || (!mediaPlayerStarted)) {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		} else {
			if (mediaPlayer.isPlaying()) {
				label.setText(R.string.home_playing);
				button.setText(R.string.button_pause);
			}
		}

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.isEnabled()) {
					HomeActivity.this.onClick();
				}
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (downloadTask != null) {
			downloadTask.setActivity(this);
		}

		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				label.setText(R.string.home_idle);
				button.setText(R.string.button_play);
				mediaPlayerStarted = false;
			}
		});
	}

	@Override
	@Deprecated
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_ID) {
			return new ProgressDialog(this);
		}

		return super.onCreateDialog(id);
	}

	@Override
	@Deprecated
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_ID) {
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					downloadTask.cancel(true);
				}
			});

			dialog.setCanceledOnTouchOutside(false);
			dialog.setTitle(R.string.home_dialog);
		}

		super.onPrepareDialog(id, dialog);
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
	protected void onPause() {
		super.onPause();

		if (downloadTask != null) {
			downloadTask.setActivity(null);
		}
		mediaPlayer.setOnCompletionListener(null);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return new InstanceObjects(downloadTask, playbackTask, mediaPlayer);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putString(PATH, path);
		savedInstanceState.putBoolean(PLAYER_STARTED, mediaPlayerStarted);
		savedInstanceState.putBoolean(TASK_CANCELLED, wasCancelled);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (!this.isChangingConfigurations()) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setWasCancelled(boolean wasCancelled) {
		this.wasCancelled = wasCancelled;
	}

	private class DownloadTask extends AsyncTask<String, Void, String> {
		private HomeActivity activity;

		public DownloadTask(HomeActivity activity) {
			setActivity(activity);
		}

		@Override
		protected void onPreExecute() {
			activity.showDialog(DIALOG_ID);

			activity.setWasCancelled(false);
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
				while ((count = input.read(data)) != -1) {
					if (isCancelled()) {
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

			return filePath;
		}

		@Override
		protected void onPostExecute(String result) {
			if (activity != null) {
				Button button = (Button) activity.findViewById(R.id.play_button);
				TextView label = (TextView) activity.findViewById(R.id.text_view);
				button.setClickable(true);
				button.setEnabled(true);
				label.setText(R.string.home_idle);

				activity.setPath(result);

				activity.dismissDialog(DIALOG_ID);
			}
		}

		@Override
		protected void onCancelled(String result) {
			if (activity != null) {
				activity.setPath(null);
				activity.setWasCancelled(true);
			}
		}

		public void setActivity(HomeActivity activity) {
			this.activity = activity;
		}
	}

	private class PlaybackTask extends AsyncTask<String, Void, Object> {
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

	private class InstanceObjects {
		private DownloadTask downloadTask;
		private PlaybackTask playbackTask;
		private MediaPlayer mediaPlayer;

		public InstanceObjects(DownloadTask downloadTask, PlaybackTask playbackTask, MediaPlayer mediaPlayer) {
			this.downloadTask = downloadTask;
			this.playbackTask = playbackTask;
			this.mediaPlayer = mediaPlayer;
		}

		public DownloadTask getDownloadTask() {
			return downloadTask;
		}

		public PlaybackTask getPlaybackTask() {
			return playbackTask;
		}

		public MediaPlayer getMediaPlayer() {
			return mediaPlayer;
		}
	}
}