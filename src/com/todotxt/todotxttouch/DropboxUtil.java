package com.todotxt.todotxttouch;

import java.io.InputStream;
import java.util.ArrayList;

import android.util.Log;

import com.dropbox.client.DropboxClient;
import com.dropbox.client.DropboxClientHelper;

public class DropboxUtil {

	private final static String TAG = DropboxUtil.class.getSimpleName();

	public static boolean addTask(DropboxClient client, String input) {
		if (client != null) {
			ArrayList<Task> tasks = null;
			try {
				tasks = fetchTasks(client);
				Task task = TaskHelper.createTask(tasks.size(), input);
				tasks.add(task);
				TodoUtil.writeToFile(tasks, Constants.TODOFILETMP);
				DropboxClientHelper.putFile(client, "/", Constants.TODOFILETMP);
				TodoUtil.writeToFile(tasks, Constants.TODOFILE);
				return true;
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		return false;
	}

	public static boolean updateTask(DropboxClient client, char prio,
			String input, Task backup) {
		if (client != null) {
			Task t = TaskHelper.createTask(backup.id, backup.text);
			t.prio = prio;
			t.text = input;
			try {
				ArrayList<Task> tasks = fetchTasks(client);
				Task found = TaskHelper.find(tasks, backup);
				if (found != null) {
					t.id = found.id;
					TaskHelper.updateById(tasks, t);
					TodoUtil.writeToFile(tasks, Constants.TODOFILETMP);
					DropboxClientHelper.putFile(client, "/",
							Constants.TODOFILETMP);
					TodoUtil.writeToFile(tasks, Constants.TODOFILE);
					return true;
				} else {
					Log.v(TAG, "Task not found, not updated");
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
		} else {
			Log.v(TAG, "Task not updated, client is null");
		}
		return false;
	}

	public static ArrayList<Task> fetchTasks(DropboxClient client)
			throws Exception {
		InputStream is = DropboxClientHelper.getFileStream(client,
				Constants.REMOTE_FILE);
		return TodoUtil.loadTasksFromStream(is);
	}

}
