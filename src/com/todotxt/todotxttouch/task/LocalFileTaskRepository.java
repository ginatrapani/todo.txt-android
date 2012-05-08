/**
 *
 * Todo.txt Touch/src/com/todotxt/todotxttouch/task/LocalFileTaskRepository.java
 *
 * Copyright (c) 2011 Tim Barlotta
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
 * @author Tim Barlotta <tim[at]barlotta[dot]net>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2011 Tim Barlotta
 */

package com.todotxt.todotxttouch.task;

import android.os.Environment;
import android.util.Log;
import com.todotxt.todotxttouch.TodoException;
import com.todotxt.todotxttouch.util.TaskIo;
import com.todotxt.todotxttouch.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A task repository for interacting with the local file system
 *
 * @author Tim Barlotta
 */
class LocalFileTaskRepository implements LocalTaskRepository {
	private static final String TAG = LocalFileTaskRepository.class
			.getSimpleName();
	final static File TODO_TXT_FILE = new File(
			Environment.getExternalStorageDirectory(),
			"data/com.todotxt.todotxttouch/todo.txt");
	private final TaskBagImpl.Preferences preferences;

	public LocalFileTaskRepository(TaskBagImpl.Preferences preferences) {
		this.preferences = preferences;
	}

	@Override
	public void init() {
		try {
			if (!TODO_TXT_FILE.exists()) {
				Util.createParentDirectory(TODO_TXT_FILE);
				TODO_TXT_FILE.createNewFile();
			}
		} catch (IOException e) {
			throw new TodoException("Error initializing LocalFile", e);
		}
	}

	@Override
	public void purge() {
		TODO_TXT_FILE.delete();
	}

	@Override
	public ArrayList<Task> load() {
		init();
		if (!TODO_TXT_FILE.exists()) {
			Log.w(TAG, TODO_TXT_FILE.getAbsolutePath() + " does not exist!");
			throw new TodoException(TODO_TXT_FILE.getAbsolutePath()
					+ " does not exist!");
		} else {
			try {
				return TaskIo.loadTasksFromFile(TODO_TXT_FILE);
			} catch (IOException e) {
				throw new TodoException("Error loading from local file", e);
			}
		}
	}

	@Override
	public void store(ArrayList<Task> tasks) {
		TaskIo.writeToFile(tasks, TODO_TXT_FILE,
				preferences.isUseWindowsLineBreaksEnabled());
	}
}
