/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2013 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2013 Todo.txt contributors (http://todotxt.com)
 */
package com.todotxt.todotxttouch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

public class Preferences extends PreferenceActivity {
	final static String TAG = Preferences.class.getSimpleName();

	private Preference aboutDialog;
	private Preference logoutDialog;
	private TodoLocationPreference mLocationPreference;
	private ListPreference periodicSync;
	private static final int ABOUT_DIALOG = 1;
	private static final int LOGOUT_DIALOG = 2;
	TodoApplication m_app;

	private String version;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		m_app = (TodoApplication) getApplication();
		((CheckBoxPreference) findPreference(m_app.m_prefs
				.prepend_date_pref_key())).setChecked(m_app.m_prefs
				.isPrependDateEnabled());

		mLocationPreference = (TodoLocationPreference)findPreference(m_app.m_prefs.todo_path_key());
		mLocationPreference.setApplication(m_app);
		mLocationPreference.setDisplayWarning(m_app.m_prefs.needToPush());
		
		PackageInfo packageInfo;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(),
					0);
			Preference versionPref = (Preference) findPreference("app_version");
			versionPref.setSummary("v" + packageInfo.versionName);
			version = packageInfo.versionName;

		} catch (NameNotFoundException e) {
			// e.printStackTrace();
		}
		aboutDialog = findPreference("app_version");
		logoutDialog = findPreference("logout_dropbox");
		periodicSync = (ListPreference) findPreference(m_app.m_prefs.periodic_sync_pref_key());
		setPeriodicSummary(periodicSync.getValue());
		periodicSync
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						setPeriodicSummary(newValue);
						return true;
					}
				});
	}

	private void setPeriodicSummary(Object newValue) {
		// Sync preference summary with selected entry. Ugly but this is the
		// only way that works.
		periodicSync.setSummary(periodicSync.getEntries()[periodicSync
				.findIndexOfValue((String) newValue)]);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen screen,
			Preference preference) {
		if (preference == aboutDialog) {
			showDialog(ABOUT_DIALOG);
		} else if (preference == logoutDialog) {
			showDialog(LOGOUT_DIALOG);
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		if (id == ABOUT_DIALOG) {
			AlertDialog.Builder aboutAlert = new AlertDialog.Builder(this);
			aboutAlert.setTitle("Todo.txt v" + version);
			aboutAlert
					.setMessage("by Gina Trapani &\nthe Todo.txt community\n\nhttp://todotxt.com");
			aboutAlert.setIcon(R.drawable.todotxt_touch_icon);
			aboutAlert.setPositiveButton("Follow us",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri
									.parse("https://mobile.twitter.com/todotxt"));
							startActivity(i);
						}
					});
			aboutAlert.setNegativeButton("Close",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
						}
					});
			aboutAlert.setOnCancelListener(new OnCancelListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onCancel(DialogInterface dialog) {
					removeDialog(id);
				}
			});
			return aboutAlert.create();
		} else if (id == LOGOUT_DIALOG) {
			AlertDialog.Builder logoutAlert = new AlertDialog.Builder(this);
			logoutAlert.setTitle(R.string.areyousure);
			SpannableStringBuilder ss = new SpannableStringBuilder();
			if (m_app.m_prefs.needToPush()) {
				ss.append(getString(R.string.dropbox_logout_warning));
				ss.setSpan(new ForegroundColorSpan(Color.RED), 0, ss.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ss.append("\n\n");
			}
			ss.append(getString(R.string.dropbox_logout_explainer));
			logoutAlert.setMessage(ss);
			logoutAlert.setPositiveButton(R.string.dropbox_logout_pref_title,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							((TodoApplication) getApplication())
									.getRemoteClientManager().getRemoteClient()
									.deauthenticate();

							// produce a logout intent and broadcast it
							Intent broadcastLogoutIntent = new Intent();
							broadcastLogoutIntent
									.setAction(Constants.INTENT_ACTION_LOGOUT);
							sendBroadcast(broadcastLogoutIntent);
							finish();
						}
					});
			logoutAlert.setNegativeButton(R.string.cancel, null);
			logoutAlert.setOnCancelListener(new OnCancelListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onCancel(DialogInterface dialog) {
					removeDialog(id);
				}
			});
			return logoutAlert.create();
		}
		return null;
	}

}
