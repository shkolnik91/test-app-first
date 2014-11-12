package com.example.test1app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

public class SplashScreenActivity extends ActionBarActivity {
	private static final String GLOBAL_END = "global_end";
	private static final long DELAY_TIME = 20000L;

	private long globalEnd = Long.MIN_VALUE;
	private Handler handler;
	private Runnable changeActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);

		if (savedInstanceState == null) {
			globalEnd = System.currentTimeMillis() + DELAY_TIME;
		} else {
			globalEnd = savedInstanceState.getLong(GLOBAL_END);
		}

		View layout = findViewById(R.id.splash_layout);
		layout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				changeActivity();
			}

		});

		changeActivity = new Runnable() {

			@Override
			public void run() {
				changeActivity();
			}

		};

		long delay = globalEnd - System.currentTimeMillis();

		if (delay < 0L) {
			delay = 0L;
		}

		handler = new Handler();
		handler.postDelayed(changeActivity, delay);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
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
	public void onStop() {
		super.onStop();
		handler.removeCallbacks(changeActivity);
		handler = null;
		changeActivity = null;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		if (!savedInstanceState.containsKey(GLOBAL_END)) {
			savedInstanceState.putLong(GLOBAL_END, globalEnd);
		}
	}

	private void changeActivity() {
		if (handler != null) {
			handler.removeCallbacks(changeActivity);
		}

		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		finish();
	}
}