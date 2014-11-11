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
	private final Handler handler = new Handler();
	private Runnable changeActivity = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);

		View layout = findViewById(R.id.splashlayout);
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

		handler.postDelayed(changeActivity, 10000L);
		//TODO change delay time to 2000 ms
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

	private void changeActivity() {
		handler.removeCallbacks(changeActivity);

		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		finish();
	}
}
