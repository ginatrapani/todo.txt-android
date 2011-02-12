/**
 *
 * Todo.txt Touch/src/com/todotxt/todotxttouch/AddTask.java
 *
 * Copyright (c) 2009-2011 mathias, Gina Trapani, Tormod Haugen
 *
 * LICENSE:
 *
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
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
 * @author Gina Trapani <ginatrapani[at]gmail[dot]com>
 * @author mathias <mathias[at]x2[dot](none)>
 * @author Tormod Haugen <tormodh[at]gmail[dot]com>
 * @author mathias <mathias[at]ws7862[dot](none)>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2011 mathias, Gina Trapani, Tormod Haugen
 */
package com.todotxt.todotxttouch;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.dropbox.client.DropboxAPI;
import com.todotxt.todotxttouch.task.Task;

public class AddTask extends Activity {

	private final static String TAG = AddTask.class.getSimpleName();

	private ProgressDialog m_ProgressDialog = null;

	private Task m_backup;

	private TodoApplication m_app;

	private TextView titleBarLabel;

	private String share_text;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.add_task);

		ArrayList<Task> tasks;
		try {
			tasks = TodoUtil.loadTasksFromFile();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			tasks = new ArrayList<Task>();
		}

		final Intent intent = getIntent();
		final String action = intent.getAction();
		// create shortcut and exit
		// create shortcut and exit
		if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			Log.d(TAG, "Setting up shortcut icon");
			setupShortcut();
			finish();
			return;
		} else if (Intent.ACTION_SEND.equals(action)) {
			Log.d(TAG, "Share");
			share_text = (String) intent
					.getCharSequenceExtra(Intent.EXTRA_TEXT);
			Log.d(TAG, share_text);
		}

		m_app = (TodoApplication) getApplication();
		// title bar label
		titleBarLabel = (TextView) findViewById(R.id.title_bar_label);

		// text
		final EditText text = (EditText) findViewById(R.id.taskText);
		text.setGravity(Gravity.TOP);

		if (share_text != null) {
			text.setText(share_text);
		}

		Task task = (Task) getIntent().getSerializableExtra(
				Constants.EXTRA_TASK);
		if (task != null) {
			m_backup = task;
			text.setText(task.inFileFormat());
			setTitle(R.string.update);
			titleBarLabel.setText(R.string.update);
		} else {
			setTitle(R.string.addtask);
			titleBarLabel.setText(R.string.addtask);
		}

		// priorities
		Spinner priorities = (Spinner) findViewById(R.id.priorities);
		final ArrayList<String> prioArr = new ArrayList<String>();
		prioArr.add("Priority");
		for (char c = 'A'; c <= 'E'; c++) {
			prioArr.add("" + c);
		}
		priorities.setAdapter(Util.newSpinnerAdapter(this, prioArr));
		if (m_backup != null && m_backup.getPriority() >= 'A'
				&& m_backup.getPriority() <= 'E') {
			priorities.setSelection(1 + m_backup.getPriority() - 'A');
		}
		priorities.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (position > 0) {
					String item = prioArr.get(position);
					String t = text.getText().toString();
					Task task = new Task(-1L, t);

					char priority = item.charAt(0);
					if (Util.isEmpty(t) && priority != Task.NO_PRIORITY) {
						task.update(" ");
						task.setPriority(priority);
					}
					text.setText(task.inFileFormat());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		// projects
		Spinner projects = (Spinner) findViewById(R.id.projects);
		final ArrayList<String> projectsArr = TaskHelper.getProjects(tasks);
		projectsArr.add(0, "Project");
		projects.setAdapter(Util.newSpinnerAdapter(this, projectsArr));
		projects.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (position > 0) {
					String item = projectsArr.get(position);
					text.append("+" + item + " ");
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		// contexts
		Spinner contexts = (Spinner) findViewById(R.id.contexts);
		final ArrayList<String> contextsArr = TaskHelper.getContexts(tasks);
		contextsArr.add(0, "Context");
		contexts.setAdapter(Util.newSpinnerAdapter(this, contextsArr));
		contexts.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (position > 0) {
					String item = contextsArr.get(position);
					text.append(" @" + item + " ");
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		// cancel
		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		// add
		Button addBtn = (Button) findViewById(R.id.addTask);
		if (m_backup != null) {
			addBtn.setText(R.string.update);
		}
		addBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// strip line breaks
				final String input = text.getText().toString()
						.replaceAll("\\r\\n|\\r|\\n", " ");

				TodoApplication app = (TodoApplication) getApplication();
				DropboxAPI api = app.getAPI();
				new AsyncTask<Object, Void, Boolean>() {
					protected void onPreExecute() {
						m_ProgressDialog = ProgressDialog.show(AddTask.this,
								getTitle(), "Please wait...", true);
					}

					@Override
					protected Boolean doInBackground(Object... params) {
						try {
							DropboxAPI api = (DropboxAPI) params[0];
							Task task = (Task) params[1];
							String input = (String) params[2];
							TodoApplication m_app = (TodoApplication) params[3];
							if (api != null) {
								if (task != null) {
									task.update(input);
									return m_app.m_util.updateTask(task);
								} else {
									return m_app.m_util.addTask(input);
								}
							}
						} catch (Exception e) {
							Log.e(TAG,
									"input: " + input + " - " + e.getMessage());
						}
						return false;
					}

					protected void onPostExecute(Boolean result) {
						if (result) {
							String res = m_backup != null ? getString(R.string.updated_task)
									: getString(R.string.added_task);
							Util.showToastLong(AddTask.this, res);
							finish();
						} else {
							String res = m_backup != null ? getString(R.string.update_task_failed)
									: getString(R.string.add_task_failed);
							Util.showToastLong(AddTask.this, res);
						}
					}
				}.execute(api, m_backup, input, m_app);
			}
		});
	}

	private void setupShortcut() {
		Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
		shortcutIntent.setClassName(this, this.getClass().getName());

		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.shortcut_addtask_name));
		Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,
				R.drawable.todotxt_touch_icon);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

		setResult(RESULT_OK, intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (m_ProgressDialog != null) {
			m_ProgressDialog.dismiss();
		}
	}
}