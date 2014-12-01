package com.example.test1app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class HomeActivity extends ActionBarActivity implements LoaderCallbacks<Object> {
	private static final int LOADER_ID = 1;
	private static final String PATH = "path";
	private static final String PLAYER_STARTED = "player_started";
	private static final String TASK_CANCELLED = "task_cancelled";
	private static final String DIALOG = "dialog";
	private static final String LOADER_CREATED = "loader_created";

	private String path;
	private PlaybackTask playbackTask;
	private MediaPlayer mediaPlayer;
	private boolean mediaPlayerStarted = false;
	private boolean wasCancelled = false;
	private boolean loaderCreated = false;
	private Button button;
	private TextView label;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		if (savedInstanceState != null) {
			mediaPlayerStarted = savedInstanceState.getBoolean(PLAYER_STARTED);
			wasCancelled = savedInstanceState.getBoolean(TASK_CANCELLED);
			loaderCreated = savedInstanceState.getBoolean(LOADER_CREATED);
			path = savedInstanceState.getString(PATH);
		}

		if (loaderCreated) {
			getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
			if (getLoaderManager().getLoader(LOADER_ID).isStarted()) {
				Log.v("     AAAAAAAAAAAAAAAAAAAAAAAAAAAA   ", " init, started ");
				getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
			} else {
				Log.v("     AAAAAAAAAAAAAAAAAAAAAAAAAAAA   ", " init, NOT started ");
				getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this).forceLoad();
			}
		} else {
			getLoaderManager().restartLoader(LOADER_ID, savedInstanceState, this).forceLoad();
			Log.v("     AAAAAAAAAAAAAAAAAAAAAAAAAAAA   ", " restart ");
		}

		Object instance = getLastCustomNonConfigurationInstance();

		if (instance instanceof InstanceObjects) {
			playbackTask = ((InstanceObjects) instance).getPlaybackTask();
			mediaPlayer = ((InstanceObjects) instance).getMediaPlayer();
		}
	}

	void showDialog() {
		DialogFragment dialogFragment = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOG);

		if (dialogFragment == null) {
			dialogFragment = new ProgressDialogFragment();
			dialogFragment.show(getFragmentManager(), DIALOG);
		}
	}

	void dismissDialog() {
		DialogFragment dialogFragment = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOG);

		if (dialogFragment != null) {
			dialogFragment.dismiss();
		}
	}

	void startViewFragment() {
		TextViewFragment viewFragment = new TextViewFragment();

		FragmentTransaction transaction = getFragmentManager().beginTransaction();

		transaction.add(R.id.relative_layout, viewFragment);
		transaction.commit();
	}

	@Override
	protected void onStart() {
		super.onStart();

		button = (Button) findViewById(R.id.play_button);
		label = (TextView) findViewById(R.id.text_view);

		BackgroundLoader castedLoader = (BackgroundLoader) getLoaderManager().getLoader(LOADER_ID);
		if ((castedLoader != null) && (castedLoader.isFinished())) {
			path = castedLoader.getPath();
			label.setText(R.string.home_idle);
			button.setClickable(true);
			button.setEnabled(true);
			dismissDialog();
		}

		if ((!wasCancelled) && (path != null)) {
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

		mediaPlayer.setOnCompletionListener(null);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return new InstanceObjects(playbackTask, mediaPlayer);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putString(PATH, path);
		savedInstanceState.putBoolean(PLAYER_STARTED, mediaPlayerStarted);
		savedInstanceState.putBoolean(TASK_CANCELLED, wasCancelled);
		savedInstanceState.putBoolean(LOADER_CREATED, loaderCreated);
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

	@Override
	public Loader<Object> onCreateLoader(int id, Bundle args) {
		showDialog();

		Log.v("     AAAAAAAAAAAAAAAAAAAAAAAAAAAA   ", " loader created ");
		loaderCreated = true;
		return new BackgroundLoader(this);
	}

	@Override
	public void onLoadFinished(Loader<Object> loader, Object data) {
		setPath((String) data);
		handler.sendEmptyMessage(0);
		Log.v("     AAAAAAAAAAAAAAAAAAAAAAAAAAAA   ", " loading finished ");
	}

	@Override
	public void onLoaderReset(Loader<Object> loader) {
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			label.setText(R.string.home_idle);
			button.setClickable(true);
			button.setEnabled(true);
			dismissDialog();
		}
	};

	private static class BackgroundLoader extends AsyncTaskLoader<Object> {
		private boolean finished = false;
		private String filePath = null;

		public BackgroundLoader(Context context) {
			super(context);
		}

		@Override
		public Object loadInBackground() {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			filePath = null;
			finished = false;

			try {
				URL url = new URL("https://upload.wikimedia.org/wikipedia/commons/f/f5/Russian_Anthem_Instrumental.ogg");
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

			finished = true;
			return filePath;
		}

		public boolean isFinished() {
			return finished;
		}

		public String getPath() {
			return filePath;
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

		private PlaybackTask playbackTask;
		private MediaPlayer mediaPlayer;

		public InstanceObjects(PlaybackTask playbackTask, MediaPlayer mediaPlayer) {

			this.playbackTask = playbackTask;
			this.mediaPlayer = mediaPlayer;
		}

		public PlaybackTask getPlaybackTask() {
			return playbackTask;
		}

		public MediaPlayer getMediaPlayer() {
			return mediaPlayer;
		}
	}

	public static class ProgressDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
			dialog.setTitle(R.string.home_dialog);
			dialog.setIndeterminate(true);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setCanceledOnTouchOutside(false);
			return dialog;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			Log.v("     AAAAAAAAAAAAAAAAAAAAAAAAAAAA   ", " is loader started? " + getActivity().getLoaderManager().getLoader(LOADER_ID).isStarted());

			getActivity().getLoaderManager().getLoader(LOADER_ID).cancelLoad();
		}
	}

	public static class TextViewFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
		        Bundle savedInstanceState) {
			TextView view = new TextView(getActivity());
			view.setText(R.string.home_download_success);
			return view;
		}

	}

}