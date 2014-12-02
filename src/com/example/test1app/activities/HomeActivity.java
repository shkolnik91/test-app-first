package com.example.test1app.activities;

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
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.test1app.R;
import com.example.test1app.loaders.SampleLoader;

public class HomeActivity extends ActionBarActivity implements LoaderCallbacks<Object> {
	private static final int LOADER_ID = 1;
	private static final String DIALOG = "dialog";

	private Button button;
	private TextView label;
	private boolean playerInitiaziled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);

		playerInitiaziled = ((BackgroundLoader) getLoaderManager().getLoader(LOADER_ID)).isPlayerReady();
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

	@Override
	protected void onStart() {
		super.onStart();

		button = (Button) findViewById(R.id.play_button);
		label = (TextView) findViewById(R.id.text_view);

		if (playerInitiaziled) {
			label.setText(R.string.home_idle);
			button.setClickable(true);
			button.setEnabled(true);
		} else {
			button.setClickable(false);
			button.setEnabled(false);
			label.setText(R.string.home_downloading);
		}

		boolean playing = isPlayerPlaying();

		if (playing) {
			label.setText(R.string.home_playing);
			button.setText(R.string.button_pause);
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

		setPlayerCompletionListener(true);
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
		boolean play = isPlayerPlaying();
		switchPlayer();

		setPlayerPlaying(!play);
	}

	@Override
	protected void onPause() {
		super.onPause();

		setPlayerCompletionListener(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (!this.isChangingConfigurations()) {
			BackgroundLoader castedLoader = (BackgroundLoader) getLoaderManager().getLoader(LOADER_ID);
			castedLoader.getMediaPlayer().stop();
			castedLoader.getMediaPlayer().release();
		}
	}

	public boolean isPlayerPlaying() {
		BackgroundLoader castedLoader = (BackgroundLoader) getLoaderManager().getLoader(LOADER_ID);

		if (castedLoader == null) {
			return false;
		} else {
			MediaPlayer player = castedLoader.getMediaPlayer();

			return ((castedLoader.isPlayerReady()) && (player.isPlaying()));
		}
	}

	public void setPlayerPlaying(boolean playerPlaying) {
		if (!playerPlaying) {
			button.setText(R.string.button_play);
			label.setText(R.string.home_idle);
		} else {
			button.setText(R.string.button_pause);
			label.setText(R.string.home_playing);
		}
	}

	public void switchPlayer() {
		BackgroundLoader castedLoader = (BackgroundLoader) getLoaderManager().getLoader(LOADER_ID);
		MediaPlayer player = castedLoader.getMediaPlayer();

		if (castedLoader.isPlayerReady()) {
			if (player.isPlaying()) {
				player.pause();
			} else {
				player.start();
			}
		}

	}

	public void setPlayerCompletionListener(boolean realListener) {
		BackgroundLoader castedLoader = (BackgroundLoader) getLoaderManager().getLoader(LOADER_ID);
		final boolean ready = castedLoader.isPlayerReady();

		if (ready) {
			OnCompletionListener listener = null;

			if (realListener) {
				listener = new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer mp) {
						setPlayerPlaying(false);
						mp.pause();
						mp.seekTo(0);
					}
				};
			}

			MediaPlayer player = castedLoader.getMediaPlayer();
			player.setOnCompletionListener(listener);
		}
	}

	@Override
	public Loader<Object> onCreateLoader(int id, Bundle args) {
		showDialog();

		return new BackgroundLoader(this, this.getString(R.string.home_url));
	}

	@Override
	public void onLoadFinished(Loader<Object> loader, Object data) {
		handler.sendEmptyMessage(0);
	}

	@Override
	public void onLoaderReset(Loader<Object> loader) {
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			playerInitiaziled = true;

			if (!isPlayerPlaying()) {
				label.setText(R.string.home_idle);
				button.setClickable(true);
				button.setEnabled(true);
				dismissDialog();
			}
		}
	};

	private static class BackgroundLoader extends SampleLoader<Object> {
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
			getActivity().getLoaderManager().getLoader(LOADER_ID).cancelLoad();
		}
	}
}