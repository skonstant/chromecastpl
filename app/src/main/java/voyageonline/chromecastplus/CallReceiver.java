package voyageonline.chromecastplus;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.Date;

public class CallReceiver extends PhonecallReceiver implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

	private GoogleApiClient mApiClient;
	private boolean mMute;
	private MediaRouter router;
	private MediaRouter.Callback callback;

	@Override
	protected void onIncomingCallStarted(Context ctx, String number, Date start) {
		muteCast(ctx);
	}

	@Override
	protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
		muteCast(ctx);
	}

	@Override
	protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
		unMuteCast(ctx);
	}

	@Override
	protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
		unMuteCast(ctx);
	}

	@Override
	protected void onMissedCall(Context ctx, String number, Date start) {
	}

	private void muteCast(final Context ctx) {
		muteCast(ctx, true);
	}

	private void muteCast(final Context ctx, boolean mute) {
		mMute = mute;

		MediaRouteSelector selectorBuilder = new MediaRouteSelector.Builder()
				.addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
				.addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
				.addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
				.build();

		router = MediaRouter.getInstance(ctx);

		callback = new MediaRouter.Callback() {
			@Override
			public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
				super.onRouteSelected(router, route);
				CastDevice device = CastDevice.getFromBundle(route.getExtras());
				if (device != null) {

					Cast.CastOptions.Builder apiOptionsBuilder = new Cast.CastOptions
							.Builder(device, new Cast.Listener() {

					});

					mApiClient = new GoogleApiClient.Builder(ctx)
							.addApi(Cast.API, apiOptionsBuilder.build())
							.addConnectionCallbacks(CallReceiver.this)
							.addOnConnectionFailedListener(CallReceiver.this)
							.build();
					mApiClient.connect();
				}
			}

			@Override
			public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
				super.onRouteUnselected(router, route);
			}

			@Override
			public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
				super.onRouteAdded(router, route);
				String name = route.getName();
				Log.d("TAG", "name = " + name);
				CastDevice device = CastDevice.getFromBundle(route.getExtras());
				if (device != null) {
					name = device.getFriendlyName();
					Log.d("TAG", "name = " + name);
					if ("Sss".equals(name)) {
						router.selectRoute(route);
					}
				}
			}

		};

		router.addCallback(selectorBuilder, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

		MediaRouter.RouteInfo route = router.getSelectedRoute();
		String name = route.getName();
		Log.d("TAG", "name = " + name);
		CastDevice device = CastDevice.getFromBundle(route.getExtras());
		if (device != null) {
			name = device.getFriendlyName();
			Log.d("TAG", "name = " + name);
			if ("Sss".equals(name)) {
				if (mApiClient != null && mApiClient.isConnected()) {
					try {
						Log.d("TAG", "muting directly = " + mMute);
						Cast.CastApi.setMute(mApiClient, mMute);
						router.removeCallback(callback);
						callback =  null;
						router = null;
						mApiClient.disconnect();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					Cast.CastOptions.Builder apiOptionsBuilder = new Cast.CastOptions
							.Builder(device, new Cast.Listener() {

					});

					mApiClient = new GoogleApiClient.Builder(ctx)
							.addApi(Cast.API, apiOptionsBuilder.build())
							.addConnectionCallbacks(CallReceiver.this)
							.addOnConnectionFailedListener(CallReceiver.this)
							.build();
					mApiClient.connect();
				}
			}
		}
	}

	private void unMuteCast(Context ctx) {
		muteCast(ctx, false);
	}


	@Override
	public void onConnected(Bundle connectionHint) {
		try {
			Log.d("TAG", "muting from googld api client = " + mMute);
			Cast.CastApi.setMute(mApiClient, mMute);
			router.removeCallback(callback);
			callback =  null;
			router = null;
			mApiClient.disconnect();
		} catch (Exception e) {
			Log.e("TAG", "Failed to mute cast", e);
		}
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.e("TAG", "connection suspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.e("TAG", "connection Failed");
	}
}
