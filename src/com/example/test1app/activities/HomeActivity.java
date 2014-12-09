package com.example.test1app.activities;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.test1app.R;
import com.example.test1app.services.DownloadService;
import com.example.test1app.services.DownloadService.DownloadServiceBinder;
import com.example.test1app.services.PlaybackService;
import com.example.test1app.services.PlaybackService.PlaybackServiceBinder;

public class HomeActivity extends ActionBarActivity {
	private static final String DIALOG = "dialog";
	public static final String PATH_KEY = "path";
	public static final String RECEIVER_KEY = "receiver";
	public static final String URL_KEY = "url";
	public static final String ALREADY_STARTED = "started";
	public static final String CURRENT_DOWNLOAD = "download";
	public static final String CURRENT_PLAYBACK = "playback";
	public static final String DOWNLOAD_FINISHED = "download finished";
	public static final String PLAYBACK_FINISHED = "playback finished";
	public static final String PLAYER_STARTED = "player started"; //????
	public static final String SERVICE_MESSAGE = "service message";
	public static final String DOWNLOAD_SERVICE_INTENT = "download service";
	public static final String PLAYBACK_SERVICE_INTENT = "playback service";

	private Button button;
	private TextView label;
	private String filePath;

	private boolean alreadyStarted;
	private boolean currentDownload;
	private boolean currentPlayback;
	private boolean playerStarted;

	private DownloadService downloadService = null;
	private PlaybackService playbackService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		if (savedInstanceState != null) {
			currentDownload = savedInstanceState.getBoolean(CURRENT_DOWNLOAD);
			currentPlayback = savedInstanceState.getBoolean(CURRENT_PLAYBACK);
			alreadyStarted = savedInstanceState.getBoolean(ALREADY_STARTED);
			filePath = savedInstanceState.getString(PATH_KEY);
			playerStarted = savedInstanceState.getBoolean(PLAYER_STARTED);
		} else {
			currentDownload = false;
			currentPlayback = false;
			alreadyStarted = false;
			playerStarted = false;
		}

		Intent downloadIntent = new Intent(this, DownloadService.class);
		if (!alreadyStarted) {
			showDialog();
			downloadIntent.putExtra(URL_KEY, getString(R.string.home_url));
			startService(downloadIntent);
			currentDownload = true;
			alreadyStarted = true;
		}

		bindService(downloadIntent, downloadConnection, Context.BIND_AUTO_CREATE);

		Intent playbackIntent = new Intent(HomeActivity.this, PlaybackService.class);

		bindService(playbackIntent, playbackConnection, Context.BIND_AUTO_CREATE);

		LocalBroadcastManager.getInstance(this).registerReceiver(intentReceiver, new IntentFilter(DOWNLOAD_SERVICE_INTENT));
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

		if (!currentDownload && !currentPlayback) {
			label.setText(R.string.home_idle);
			button.setClickable(true);
			button.setEnabled(true);
		} else {
			button.setClickable(false);
			button.setEnabled(false);
			label.setText(R.string.home_downloading);
		}

		if (currentPlayback) {
			label.setText(R.string.home_playing);
			button.setText(R.string.button_pause);
			button.setClickable(true);
			button.setEnabled(true);
		}

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.isEnabled()) {
					Intent playbackIntent = new Intent(HomeActivity.this, PlaybackService.class);
					if (filePath != null) {
						if (playerStarted) {
							switchPlayer();
						} else {
							playbackIntent.putExtra(PATH_KEY, filePath);

							bindService(playbackIntent, playbackConnection, Context.BIND_AUTO_CREATE);
							startService(playbackIntent);

							playerStarted = true;
							currentPlayback = true;
						}
					}
					setPlayerPlaying(currentPlayback);
				}
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();
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

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean(CURRENT_DOWNLOAD, currentDownload);
		savedInstanceState.putBoolean(CURRENT_PLAYBACK, currentPlayback);
		savedInstanceState.putBoolean(ALREADY_STARTED, alreadyStarted);
		savedInstanceState.putString(PATH_KEY, filePath);
		savedInstanceState.putBoolean(PLAYER_STARTED, playerStarted);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		button.setOnClickListener(null);
	}

	@Override
	protected void onDestroy() {
		unbindService(downloadConnection);
		unbindService(playbackConnection);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(intentReceiver);
		super.onDestroy();
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
		if (playbackService != null) {
			currentPlayback = playbackService.switchPlayer();
		}
	}

	public void cancelDownload() {
		if (downloadService != null) {
			downloadService.cancelLoad();
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
			((HomeActivity) getActivity()).cancelDownload();
		}
	}

	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String message = intent.getStringExtra(SERVICE_MESSAGE);

			if (DOWNLOAD_FINISHED.equals(message)) {
				filePath = intent.getStringExtra(PATH_KEY);
				dismissDialog();
				currentDownload = false;

				label.setText(R.string.home_idle);
				button.setClickable(true);
				button.setEnabled(true);
			} else if (PLAYBACK_FINISHED.equals(message)) {
				//TODO
			}
		}
	};

	private ServiceConnection downloadConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			DownloadServiceBinder binder = (DownloadServiceBinder) service;
			downloadService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			downloadService = null;
		}
	};

	private ServiceConnection playbackConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			PlaybackServiceBinder binder = (PlaybackServiceBinder) service;
			playbackService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			playbackService = null;
		}
	};
}