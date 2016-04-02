package game.opencv;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private boolean started = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((Button) this.findViewById(R.id.button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				perform();
			}
		});
	}

	private void perform() {
		if (started) {
			Intent intent = new Intent(this, MainService.class);
			stopService(intent);
			started = false;

			((TextView) this.findViewById(R.id.text)).setText(R.string.not_started);
		} else {
			Intent intent = new Intent(this, MainService.class);
			startService(intent);
			started = true;

			((TextView) this.findViewById(R.id.text)).setText(R.string.started);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class MainService extends Service {
		private NotificationManager mNM;

		// Unique Identification Number for the Notification.
		// We use it on Notification start, and to cancel it.
		private int NOTIFICATION = R.string.started;

		private boolean serverActive = true;

		public void startServer() {
			AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					DatagramSocket socket = null;

					try {
						byte[] message = new byte[100];
						DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("127.0.0.1"), 9876);

						socket = new DatagramSocket();
						socket.setBroadcast(true);

						int n = 10000;
						int i = 0;

						long start = System.currentTimeMillis();

						while (serverActive) {
							if (i == n) {
								break;
							}

							socket.send(packet);

							// socket.receive(packet);
							
							i++;
						}

						long end = System.currentTimeMillis();

						System.out.println("Pockets = " + n);
						System.out.println("Total time = " + (end - start) + "ms");
						System.out.println("Avg time = " + (double)(end - start) / n + "ms");

					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (socket != null) {
							socket.close();
						}
					}

					return null;
				}
			};

			async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		public void stopServer() {
			serverActive = false;
		}

		/**
		 * Class for clients to access. Because we know this service always runs
		 * in the same process as its clients, we don't need to deal with IPC.
		 */
		public class LocalBinder extends Binder {
			MainService getService() {
				return MainService.this;
			}
		}

		@Override
		public void onCreate() {
			mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			// Display a notification about us starting. We put an icon in the
			// status bar.
			showNotification(getText(R.string.perform));

			startServer();
		}

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			Log.i("LocalService", "Received start id " + startId + ": " + intent);
			return START_NOT_STICKY;
		}

		@Override
		public void onDestroy() {
			stopServer();

			// Cancel the persistent notification.
			mNM.cancel(NOTIFICATION);

			// Tell the user we stopped.
			Toast.makeText(this, R.string.not_started, Toast.LENGTH_SHORT).show();
		}

		@Override
		public IBinder onBind(Intent intent) {
			return mBinder;
		}

		// This is the object that receives interactions from clients. See
		// RemoteService for a more complete example.
		private final IBinder mBinder = new LocalBinder();

		private void showNotification(CharSequence value) {
			// In this sample, we'll use the same text for the ticker and the
			// expanded notification
			CharSequence text = getText(R.string.started);

			// Set the info for the views that show in the notification panel.
			Notification notification = new Notification.Builder(this).setSmallIcon(R.drawable.ic_launcher) // the
																											// status
																											// icon
					.setTicker(text) // the status text
					.setWhen(System.currentTimeMillis()) // the time stamp
					.setContentTitle(value) // the label of
											// the entry
					.setContentText(text) // the contents of the entry
					.build();

			// Send the notification.
			mNM.notify(NOTIFICATION, notification);
		}
	}
}
