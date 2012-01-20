/**
 *
 * Todo.txt Touch/src/com/todotxt/todotxttouch/task/Task.java
 *
 * Copyright (c) 2009-2011 mathias, Gina Trapani, Tim Barlotta, Florian Behr
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
 * @author mathias <mathias[at]x2[dot](none)>
 * @author Gina Trapani <ginatrapani[at]gmail[dot]com>
 * @author Tim Barlotta <tim[at]barlotta[dot]net>
 * @author Florian Behr <mail[at]florianbehr[dot]de>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2011 mathias, Gina Trapani, Tim Barlotta, Florian Behr
 */
package com.todotxt.todotxttouch.task;

import java.io.Serializable;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.todotxt.todotxttouch.util.RelativeDate;
import com.todotxt.todotxttouch.util.Strings;

@SuppressWarnings("serial")
public class Task implements Serializable {
	private static final String COMPLETED = "x ";
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private final String originalText;
	private final Priority originalPriority;

	private long id;
	private Priority priority;
	private boolean deleted = false;
	private boolean completed = false;
	private String text;
	private String completionDate;
	private String prependedDate;
	private String relativeAge = "";
	private List<String> contexts;
	private List<String> projects;
	private List<String> mailAddresses;
	private List<URL> links;

	public Task(long id, String rawText, Date defaultPrependedDate) {
		this.id = id;
		this.init(rawText, defaultPrependedDate);
		this.originalPriority = priority;
		this.originalText = text;
	}

	public Task(long id, String rawText) {
		this(id, rawText, null);
	}

	public void update(String rawText) {
		this.init(rawText, null);
	}

	private void init(String rawText, Date defaultPrependedDate) {
		TextSplitter splitter = TextSplitter.getInstance();
		TextSplitter.SplitResult splitResult = splitter.split(rawText);
		this.priority = splitResult.priority;
		this.text = splitResult.text;
		this.prependedDate = splitResult.prependedDate;
		this.completed = splitResult.completed;
		this.completionDate = splitResult.completedDate;

		this.contexts = ContextParser.getInstance().parse(text);
		this.projects = ProjectParser.getInstance().parse(text);
		this.mailAddresses = MailAddressParser.getInstance().parse(text);
		this.links = LinkParser.getInstance().parse(text);
		this.deleted = Strings.isEmptyOrNull(text);

		if (defaultPrependedDate != null
				&& Strings.isEmptyOrNull(this.prependedDate)) {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
			this.prependedDate = formatter.format(defaultPrependedDate);
		}

		if (!Strings.isEmptyOrNull(this.prependedDate)) {
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			try {
				Date d = sdf.parse(this.prependedDate);
				this.relativeAge = RelativeDate.getRelativeDate(d);
			} catch (ParseException e) {
				// e.printStackTrace();
			}
		}
	}

	public Priority getOriginalPriority() {
		return originalPriority;
	}

	public String getOriginalText() {
		return originalText;
	}

	public String getText() {
		return text;
	}

	public long getId() {
		return id;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Priority getPriority() {
		return priority;
	}

	public List<String> getContexts() {
		return contexts;
	}

	public List<URL> getLinks() {
		return links;
	}

	public List<String> getProjects() {
		return projects;
	}

	public List<String> getMailAddresses() {
		return mailAddresses;
	}

	public String getPrependedDate() {
		return prependedDate;
	}

	public String getRelativeAge() {
		return relativeAge;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public boolean isCompleted() {
		return completed;
	}

	public String getCompletionDate() {
		return completionDate;
	}

	public void markComplete(Date date) {
		if (!this.completed) {
			this.priority = Priority.NONE;
			this.completionDate = new SimpleDateFormat(Task.DATE_FORMAT)
					.format(date);
			this.deleted = false;
			this.completed = true;
		}
	}

	public void markIncomplete() {
		if (this.completed) {
			this.completionDate = "";
			this.completed = false;
		}
	}

	public void delete() {
		this.update("");
	}

	// TODO need a better solution (TaskFormatter?) here
	public String inScreenFormat() {
		StringBuilder sb = new StringBuilder();
		if (this.completed) {
			sb.append(COMPLETED).append(this.completionDate).append(" ");
			if (!Strings.isEmptyOrNull(this.prependedDate)) {
				sb.append(this.prependedDate).append(" ");
			}
		}
		sb.append(this.text);
		return sb.toString();
	}

	public String inFileFormat() {
		StringBuilder sb = new StringBuilder();
		if (this.completed) {
			sb.append(COMPLETED).append(this.completionDate).append(" ");
			if (!Strings.isEmptyOrNull(this.prependedDate)) {
				sb.append(this.prependedDate).append(" ");
			}
		} else {
			if (priority != Priority.NONE) {
				sb.append(priority.inFileFormat()).append(" ");
			}
			if (!Strings.isEmptyOrNull(this.prependedDate)) {
				sb.append(this.prependedDate).append(" ");
			}
		}
		sb.append(this.text);
		return sb.toString();
	}

	public void copyInto(Task destination) {
		destination.id = this.id;
		destination.init(this.inFileFormat(), null);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		if (completed != other.completed)
			return false;
		if (completionDate == null) {
			if (other.completionDate != null)
				return false;
		} else if (!completionDate.equals(other.completionDate))
			return false;
		if (contexts == null) {
			if (other.contexts != null)
				return false;
		} else if (!contexts.equals(other.contexts))
			return false;
		if (deleted != other.deleted)
			return false;
		if (id != other.id)
			return false;
		if (links == null) {
			if (other.links != null)
				return false;
		} else if (!links.equals(other.links))
			return false;
		if (mailAddresses == null) {
			if (other.mailAddresses != null)
				return false;
		} else if (!mailAddresses.equals(other.mailAddresses))
			return false;
		if (prependedDate == null) {
			if (other.prependedDate != null)
				return false;
		} else if (!prependedDate.equals(other.prependedDate))
			return false;
		if (priority != other.priority)
			return false;
		if (projects == null) {
			if (other.projects != null)
				return false;
		} else if (!projects.equals(other.projects))
			return false;
		if (relativeAge == null) {
			if (other.relativeAge != null)
				return false;
		} else if (!relativeAge.equals(other.relativeAge))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (completed ? 1231 : 1237);
		result = prime * result
				+ ((completionDate == null) ? 0 : completionDate.hashCode());
		result = prime * result
				+ ((contexts == null) ? 0 : contexts.hashCode());
		result = prime * result + (deleted ? 1231 : 1237);
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((links == null) ? 0 : links.hashCode());
		result = prime * result
				+ ((mailAddresses == null) ? 0 : mailAddresses.hashCode());
		result = prime * result
				+ ((prependedDate == null) ? 0 : prependedDate.hashCode());
		result = prime * result
				+ ((priority == null) ? 0 : priority.hashCode());
		result = prime * result
				+ ((projects == null) ? 0 : projects.hashCode());
		result = prime * result
				+ ((relativeAge == null) ? 0 : relativeAge.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}
}