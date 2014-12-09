package com.example.test1app.services;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.example.test1app.R;
import com.example.test1app.activities.HomeActivity;

public class PlaybackService extends Service {
	private static final int ID = 181;

	private MediaPlayer mediaPlayer;
	private String filePath;

	private final IBinder binder = new PlaybackServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Intent notificationIntent = new Intent(this, HomeActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

		Notification notification = new Notification.Builder(this)
		        .setContentTitle(getString(R.string.media_player_started))
		        .setContentText(getString(R.string.media_player_started))
		        .setSmallIcon(R.drawable.ic_notif)
		        .setContentIntent(pendingIntent)
		        .build();

		startForeground(ID, notification);
		filePath = intent.getStringExtra(HomeActivity.PATH_KEY);
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

			mediaPlayer.setDataSource(filePath);
			mediaPlayer.prepare();

			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					mp.pause();
					mp.seekTo(0);

					Intent outerIntent = new Intent(HomeActivity.PLAYBACK_SERVICE_INTENT);
					outerIntent.putExtra(HomeActivity.SERVICE_MESSAGE, HomeActivity.PLAYBACK_FINISHED);
					LocalBroadcastManager.getInstance(PlaybackService.this).sendBroadcast(outerIntent);
				}
			});

			mediaPlayer.start();
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

		return super.onStartCommand(notificationIntent, flags, startId);
	}

	public boolean switchPlayer() {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		} else {
			mediaPlayer.start();
		}

		return mediaPlayer.isPlaying();
	}

	public class PlaybackServiceBinder extends Binder {
		public PlaybackService getService() {
			return PlaybackService.this;
		}
	}
}
