package com.csctracker.androidtracker.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import android.provider.Settings;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.csctracker.androidtracker.misc.Const;
import com.csctracker.androidtracker.misc.DatabaseHelper;
import com.csctracker.androidtracker.misc.Util;
import com.csctracker.androidtracker.service.NotificationHandler;
import org.com.csctracker.androidtracker.BuildConfig;
import org.com.csctracker.androidtracker.R;

public class SettingsFragment extends PreferenceFragmentCompat {

	public static final String TAG = SettingsFragment.class.getName();

	private DatabaseHelper dbHelper;
	private BroadcastReceiver updateReceiver;

	private Preference prefStatus;
	private Preference prefBrowse;
	private Preference prefText;
	private Preference prefOngoing;
	private Preference prefToken;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.preferences);

		PreferenceManager pm = getPreferenceManager();

		prefStatus = pm.findPreference(Const.PREF_STATUS);
		if(prefStatus != null) {
			prefStatus.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
				startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
				return true;
			});
		}

		prefBrowse = pm.findPreference(Const.PREF_BROWSE);
		if(prefBrowse != null) {
			prefBrowse.setOnPreferenceClickListener(preference -> {
				startActivity(new Intent(getActivity(), BrowseActivity.class));
				return true;
			});
		}

		prefText    = pm.findPreference(Const.PREF_TEXT);
		prefOngoing = pm.findPreference(Const.PREF_ONGOING);
		prefToken = pm.findPreference(Const.PREF_TOKEN);

		Preference prefAbout = pm.findPreference(Const.PREF_ABOUT);
		if(prefAbout != null) {
			prefAbout.setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://github.com/interactionlab/android-notification-log"));
				startActivity(intent);
				return true;
			});
		}

		Preference prefVersion = pm.findPreference(Const.PREF_VERSION);
		if(prefVersion != null) {
			prefVersion.setSummary(BuildConfig.VERSION_NAME + (Const.DEBUG ? " dev" : ""));
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			dbHelper = new DatabaseHelper(getActivity());
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}

		updateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				update();
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		if(Util.isNotificationAccessEnabled(getActivity())) {
			prefStatus.setSummary(R.string.settings_notification_access_enabled);
			prefText.setEnabled(true);
			prefOngoing.setEnabled(true);
		} else {
			prefStatus.setSummary(R.string.settings_notification_access_disabled);
			prefText.setEnabled(false);
			prefOngoing.setEnabled(false);
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(NotificationHandler.BROADCAST);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(updateReceiver, filter);

		update();
	}

	@Override
	public void onPause() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(updateReceiver);
		super.onPause();
	}

	private void update() {
		try {
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			long numRowsPosted = DatabaseUtils.queryNumEntries(db, DatabaseHelper.PostedEntry.TABLE_NAME);
			int stringResource = numRowsPosted == 1 ? R.string.settings_browse_summary_singular : R.string.settings_browse_summary_plural;
			prefBrowse.setSummary(getString(stringResource, numRowsPosted));
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

}