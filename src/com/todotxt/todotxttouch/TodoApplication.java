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

import java.util.ArrayList;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.todotxt.todotxttouch.remote.RemoteClientManager;
import com.todotxt.todotxttouch.remote.RemoteConflictException;
import com.todotxt.todotxttouch.task.Priority;
import com.todotxt.todotxttouch.task.Sort;
import com.todotxt.todotxttouch.task.TaskBag;
import com.todotxt.todotxttouch.task.TaskBagFactory;
import com.todotxt.todotxttouch.util.Util;

public class TodoApplication extends Application {
	private final static String TAG = TodoApplication.class.getSimpleName();
	public SharedPreferences m_prefs;
	private RemoteClientManager remoteClientManager;
	private boolean m_pulling = false;
	private boolean m_pushing = false;
	private int pushQueue = 0;
	private TaskBag taskBag;
	private BroadcastReceiver m_broadcastReceiver;
	private static Context appContext;

	// filter variables
	public ArrayList<Priority> m_prios = new ArrayList<Priority>();
	public ArrayList<String> m_contexts = new ArrayList<String>();
	public ArrayList<String> m_projects = new ArrayList<String>();
	public String m_search;
	public ArrayList<String> m_filters = new ArrayList<String>();
	public Sort sort = Sort.PRIORITY_DESC;

	@Override
	public void onCreate() {
		super.onCreate();
		TodoApplication.appContext = getApplicationContext();
		m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		remoteClientManager = new RemoteClientManager(this, m_prefs);
		this.taskBag = TaskBagFactory.getTaskBag(this, m_prefs);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Constants.INTENT_SET_MANUAL);
		intentFilter.addAction(Constants.INTENT_START_SYNC_WITH_REMOTE);
		intentFilter.addAction(Constants.INTENT_START_SYNC_TO_REMOTE);
		intentFilter.addAction(Constants.INTENT_START_SYNC_FROM_REMOTE);
		intentFilter.addAction(Constants.INTENT_ASYNC_FAILED);

		if (null == m_broadcastReceiver) {
			m_broadcastReceiver = new BroadcastReceiverExtension();
			registerReceiver(m_broadcastReceiver, intentFilter);
		}

		// initialize tasks so widget gets tasks after application redeployment
		taskBag.reload();
		Log.d("TODO APPLICATION1", taskBag.toString());
		Log.d("TODO APPLICATION1", this.toString());

	}

	@Override
	public void onTerminate() {
		if (null != m_broadcastReceiver) {
			unregisterReceiver(m_broadcastReceiver);
		}
		super.onTerminate();
	}
	
	/**
	 * Are we currently pushing or pulling remote data?
	 * @return true iff a remote operation currently in progress.
	 */
	public boolean syncInProgress() {
		return m_pulling || m_pushing;
	}

	/**
	 * If we previously tried to push and failed, then attempt to push again
	 * now. Otherwise, pull.
	 */
	private void syncWithRemote(boolean force, boolean suppressToast) {
		if (needToPush()) {
			Log.d(TAG, "needToPush = true; pushing.");
			pushToRemote(force, false, suppressToast);
		} else {
			Log.d(TAG, "needToPush = false; pulling.");
			pullFromRemote(force, suppressToast);
		}
	}

	/**
	 * Check network status, then push.
	 */
	private void pushToRemote(boolean force, boolean overwrite, boolean suppressToast) {
		pushQueue += 1;
		setNeedToPush(true);
		if (!force && isManualMode()) {
			Log.i(TAG, "Working offline, don't push now");
		} else if (getRemoteClientManager().getRemoteClient().isAvailable()
				&& !m_pulling) {
			Log.i(TAG, "Working online; should push if file revisions match");
			backgroundPushToRemote(overwrite, suppressToast);
		} else if (m_pulling) {
			Log.d(TAG, "app is pulling right now. don't start push."); 
		} else {
			Log.i(TAG, "Not connected, don't push now");
			if (!suppressToast) {
			    showToast(R.string.toast_notconnected);
			}
		}
	}

	/**
	 * Check network status, then pull.
	 */
	private void pullFromRemote(boolean force, boolean suppressToast) {
		if (!force && isManualMode()) {
			Log.i(TAG, "Working offline, don't pull now");
			return;
		}

		setNeedToPush(false);

		if (getRemoteClientManager().getRemoteClient().isAvailable()
				&& !m_pushing) {
			Log.i(TAG, "Working online; should pull file");
			backgroundPullFromRemote();
		} else if (m_pushing) {
			Log.d(TAG, "app is pushing right now. don't start pull."); 
		} else {
			Log.i(TAG, "Not connected, don't pull now");
			
			if (!suppressToast) {
			    showToast(R.string.toast_notconnected);
			}
		}
	}

	public TaskBag getTaskBag() {
		return taskBag;
	}

	public RemoteClientManager getRemoteClientManager() {
		return remoteClientManager;
	}

	public boolean isManualMode() {
		try {
			long period = Long.parseLong(m_prefs.getString(
					Constants.PREF_PERIODIC_SYNC, "0"));
			return period < 0;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public boolean needToPush() {
		return m_prefs.getBoolean(Constants.PREF_NEED_TO_PUSH, false);
	}

	public void setNeedToPush(boolean needToPush) {
		Editor editor = m_prefs.edit();
		editor.putBoolean(Constants.PREF_NEED_TO_PUSH, needToPush);
		editor.commit();
	}

	public static Context getAppContetxt() {
		return appContext;
	}

	public void showToast(int resid) {
		Util.showToastLong(this, resid);
	}

	public void showToast(String string) {
		Util.showToastLong(this, string);
	}

	/**
	 * Do asynchronous push with gui changes. Do availability check first.
	 */
	void backgroundPushToRemote(final boolean overwrite, final boolean suppressToast) {
		if (getRemoteClientManager().getRemoteClient().isAuthenticated()) {
			if(!(m_pushing || m_pulling)) {
				new AsyncPushTask(overwrite, suppressToast).execute();
			}
		} else {
			Log.e(TAG, "NOT AUTHENTICATED!");
			showToast("NOT AUTHENTICATED!");
		}
	}

	private class AsyncPushTask extends AsyncTask<Void, Void, Integer> {
		static final int SUCCESS = 0;
		static final int CONFLICT = 1;
		static final int ERROR = 2;

		private boolean overwrite;
        private boolean suppressToast;
		
		public AsyncPushTask(boolean overwrite, boolean suppressToast) {
			this.overwrite = overwrite;
			this.suppressToast = suppressToast;
		}
		
		@Override
		protected void onPreExecute() {
			m_pushing = true;
			pushQueue = 0;
			updateSyncUI(false);
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			try {
				Log.d(TAG, "start taskBag.pushToRemote");
				taskBag.pushToRemote(true, overwrite);
			} catch (RemoteConflictException c) {
				Log.e(TAG, c.getMessage());
				return CONFLICT;
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				return ERROR;
			}
			return SUCCESS;
		}

		@Override
		protected void onPostExecute(Integer result) {
			Log.d(TAG, "post taskBag.pushToremote");
			m_pushing = false;
			if (result == SUCCESS) {
				if (pushQueue > 0) {
					m_pushing = true;
					Log.d(TAG, "pushQueue == " + pushQueue + ". Need to push again.");
					new AsyncPushTask(false, suppressToast).execute();
				} else {
					Log.d(TAG, "taskBag.pushToRemote done");
					setNeedToPush(false);
					updateSyncUI(false);
					// Push is complete. Now do a pull in case the remote
					// done.txt has changed.
					pullFromRemote(true, suppressToast);
				}
			} else if (result == CONFLICT) {
				// FIXME: need to know which file had conflict
				sendBroadcast(new Intent(Constants.INTENT_SYNC_CONFLICT));
			} else {
				sendBroadcast(new Intent(Constants.INTENT_ASYNC_FAILED));
			}
			super.onPostExecute(result);
		}
	}

	/**
	 * Do an asynchronous pull from remote. Check network availability before
	 * calling this.
	 */
	private void backgroundPullFromRemote() {
		if (getRemoteClientManager().getRemoteClient().isAuthenticated()) {
			m_pulling = true;
			updateSyncUI(false);

			new AsyncTask<Void, Void, Boolean>() {

				@Override
				protected Boolean doInBackground(Void... params) {
					try {
						Log.d(TAG, "start taskBag.pullFromRemote");
						taskBag.pullFromRemote(true);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
						return false;
					}
					return true;
				}

				@Override
				protected void onPostExecute(Boolean result) {
					Log.d(TAG, "post taskBag.pullFromRemote");
					m_pulling = false;
					if (result) {
						Log.d(TAG, "taskBag.pullFromRemote done");
						updateSyncUI(true);
					} else {
						sendBroadcast(new Intent(Constants.INTENT_ASYNC_FAILED));
					}
					super.onPostExecute(result);
				}

			}.execute();
		} else {
			Log.e(TAG, "NOT AUTHENTICATED!");
			showToast("NOT AUTHENTICATED!");
		}
	}

	private void updateSyncUI(boolean redrawList) {
		sendBroadcast(new Intent(Constants.INTENT_UPDATE_UI).putExtra(
				"redrawList", redrawList));
		if(redrawList) {
			broadcastWidgetUpdate();
		}
	}

	private final class BroadcastReceiverExtension extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean force_sync = intent.getBooleanExtra(
					Constants.EXTRA_FORCE_SYNC, false);
			boolean overwrite = intent.getBooleanExtra(
					Constants.EXTRA_OVERWRITE, false);
			boolean suppressToast = intent.getBooleanExtra(
                    Constants.EXTRA_SUPPRESS_TOAST, false);
			if (intent.getAction().equalsIgnoreCase(
					Constants.INTENT_START_SYNC_WITH_REMOTE)) {
				syncWithRemote(force_sync, suppressToast);
			} else if (intent.getAction().equalsIgnoreCase(
					Constants.INTENT_START_SYNC_TO_REMOTE)) {
				pushToRemote(force_sync, overwrite, suppressToast);
			} else if (intent.getAction().equalsIgnoreCase(
					Constants.INTENT_START_SYNC_FROM_REMOTE)) {
				pullFromRemote(force_sync, suppressToast);
			} else if (intent.getAction().equalsIgnoreCase(
					Constants.INTENT_ASYNC_FAILED)) {
			    if (!suppressToast) {
			        showToast("Synchronizing Failed");
			    }
				m_pulling = false;
				m_pushing = false;
				updateSyncUI(true);
			}
		}
	}

	public void broadcastWidgetUpdate() {
		Log.d(TAG, "Broadcasting widget update intent");
		Intent intent = new Intent(Constants.INTENT_WIDGET_UPDATE);
		sendBroadcast(intent);
	}

	public long getSyncPeriod() {
		try {
			long period = Long.parseLong(m_prefs.getString(
					Constants.PREF_PERIODIC_SYNC, "0"));
			return period > 0 ? period : 0;
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	public void storeSort() {
		Editor editor = m_prefs.edit();
		editor.putInt(Constants.PREF_SORT, sort.getId());
		editor.commit();
		broadcastWidgetUpdate();
	}
	
	public void getStoredSort() {
		sort = Sort.getById(m_prefs.getInt(Constants.PREF_SORT, Sort.PRIORITY_DESC.getId()));
	}
	
	public void storeFilters() {
		Editor editor = m_prefs.edit();
		editor.putString(Constants.PREF_FILTER_PRIOS, Util.join(Priority.inCode(m_prios), " "));
		editor.putString(Constants.PREF_FILTER_CONTEXTS, Util.join(m_contexts, " "));
		editor.putString(Constants.PREF_FILTER_PROJECTS, Util.join(m_projects, " "));
		editor.putString(Constants.PREF_FILTER_SEARCH, m_search);
		//split on tab just in case there is a space in the text
		editor.putString(Constants.PREF_FILTER_SUMMARY, Util.join(m_filters, "\t"));
		editor.commit();
		broadcastWidgetUpdate();
	}

	public void getStoredFilters() {
		m_prios = Priority.toPriority(Util.split(m_prefs.getString(Constants.PREF_FILTER_PRIOS, ""), " "));
		m_contexts = Util.split(m_prefs.getString(Constants.PREF_FILTER_CONTEXTS, ""), " ");
		m_projects = Util.split(m_prefs.getString(Constants.PREF_FILTER_PROJECTS, ""), " ");
		m_search = m_prefs.getString(Constants.PREF_FILTER_SEARCH, "");
		//split on tab just in case there is a space in the text
		m_filters = Util.split(m_prefs.getString(Constants.PREF_FILTER_SUMMARY, ""), "\t");
	}

}