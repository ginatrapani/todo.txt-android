/**
 * This file is part of Todo.txt for Android, an app for managing your todo.txt file (http://todotxt.com).
 * <p>
 * Copyright (c) 2009-2013 Todo.txt for Android contributors (http://todotxt.com)
 * <p>
 * LICENSE:
 * <p>
 * Todo.txt for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p>
 * Todo.txt for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Todo.txt for Android. If not, see
 * <http://www.gnu.org/licenses/>.
 * <p>
 * Todo.txt for Android's source code is available at https://github.com/ginatrapani/todo.txt-android
 *
 * @author Todo.txt for Android contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2013 Todo.txt for Android contributors (http://todotxt.com)
 */

package com.todotxt.todotxttouch.remote;

import com.dropbox.core.v2.files.FolderMetadata;
import com.todotxt.todotxttouch.util.Strings;

class DropboxRemoteFolder extends RemoteFolderImpl {
    private static final String ROOT_PATH = "/";
    private static final String ROOT_NAME = "Dropbox";

    public DropboxRemoteFolder(String path) {
        super(Strings.isBlank(path) ? ROOT_PATH : path);
    }

    public DropboxRemoteFolder(FolderMetadata metadata){
        super(metadata);
    }

    @Override
    public String getName() {
        if (mPath.equals(ROOT_PATH)) {
            return ROOT_NAME;
        }

        return super.getName();
    }

    @Override
    public String getParentName() {
        if (mParentPath.equals(ROOT_PATH)) {
            return ROOT_NAME;
        }

        return super.getParentName();
    }
}
