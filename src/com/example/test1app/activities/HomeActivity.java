package com.example.test1app.activities;


import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.test1app.R;
import com.example.test1app.services.DownloadPlaybackService;

public class HomeActivity extends ActionBarActivity {
	private static final String DIALOG = "dialog";
	public static final String ACTION_KEY = "action";
	public static final String RECEIVER_KEY = "receiver";
	public static final String URL_KEY = "url";
	public static final String CANCEL_VAL = "cancel";
	public static final String START_VAL = "start";
	public static final String SWITCH_VAL = "switch";
	public static final String ALREADY_STARTED = "started";
	public static final String CURRENT_DOWNLOAD = "download";
	public static final String CURRENT_PLAYBACK = "playback";


	private Button button;
	private TextView label;
	private ResultReceiver resultReceiver;

	private boolean alreadyStarted;
	private boolean currentDownload;
	private boolean currentPlayback;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		if (savedInstanceState != null) {
			currentDownload = savedInstanceState.getBoolean(CURRENT_DOWNLOAD);
			currentPlayback = savedInstanceState.getBoolean(CURRENT_PLAYBACK);
			alreadyStarted = savedInstanceState.getBoolean(ALREADY_STARTED);
		} else {
			currentDownload = false;
			currentPlayback = false;
			alreadyStarted = false;
		}

		if (!alreadyStarted) {
			showDialog();
			Intent intent = new Intent(this, DownloadPlaybackService.class);
			intent.putExtra(ACTION_KEY, START_VAL);
			intent.putExtra(URL_KEY, getString(R.string.home_url));
			intent.putExtra(RECEIVER_KEY, new DownloadPlaybackReceiver(new Handler()));
			startService(intent);
			currentDownload = true;
			alreadyStarted = true;
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

		boolean playing = false;

		if (playing) {
			label.setText(R.string.home_playing);
			button.setText(R.string.button_pause);
		}

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.isEnabled()) {
					boolean play = false;
					switchPlayer();

					setPlayerPlaying(!play);
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
		super.onDestroy();
		if (!this.isChangingConfigurations()) {

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
		Intent intent = new Intent(this, DownloadPlaybackService.class);
		intent.putExtra(ACTION_KEY, SWITCH_VAL);
		startService(intent);
	}

	public void cancelDownload() {
		Intent intent = new Intent(this, DownloadPlaybackService.class);
		intent.putExtra(ACTION_KEY, CANCEL_VAL);
		startService(intent);
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

	private class DownloadPlaybackReceiver extends ResultReceiver {
		
		
		public DownloadPlaybackReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			super.onReceiveResult(resultCode, resultData);
			dismissDialog();
			currentDownload = false;
		}
	}
}