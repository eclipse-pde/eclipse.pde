/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.logview;

import java.io.*;
import java.util.*;

class LogReader {
	private static final int SESSION_STATE = 10;
	private static final int ENTRY_STATE = 20;
	private static final int SUBENTRY_STATE = 30;
	private static final int MESSAGE_STATE = 40;
	private static final int STACK_STATE = 50;
	private static final int TEXT_STATE = 60;
	private static final int UNKNOWN_STATE = 70;

	public static void parseLogFile(File file, ArrayList entries) {
		List lines = load(file);
		parse(lines, entries);
	}
	/**
	 * Reads the contents from the given file and returns them as
	 * a List of lines.
	 */
	private static List load(File file) {
		List lines = null;
		// read current contents
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			BufferedReader reader =
				new BufferedReader(new InputStreamReader(is, "UTF8"));
			lines = new LineReader(reader).readLines();
		} catch (FileNotFoundException ex) {
		} catch (UnsupportedEncodingException ex) {
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException ex) {
				}
		}
		if (lines == null)
			lines = new ArrayList();
		return lines;
	}
	private static LogEntry[] parse(List lines, ArrayList entries) {
		ArrayList parents = new ArrayList();
		LogEntry current = null;
		LogSession session = null;
		int writerState = UNKNOWN_STATE;
		StringWriter swriter = null;
		PrintWriter writer = null;
		int state = UNKNOWN_STATE;

		for (int i = 0; i < lines.size(); i++) {
			String line = (String) lines.get(i);
			line = line.trim();

			if (line.startsWith("!SESSION")) {
				state = SESSION_STATE;
			} else if (line.startsWith("!ENTRY")) {
				state = ENTRY_STATE;
			} else if (line.startsWith("!SUBENTRY")) {
				state = SUBENTRY_STATE;
			} else if (line.startsWith("!MESSAGE")) {
				state = MESSAGE_STATE;
			} else if (line.startsWith("!STACK")) {
				state = STACK_STATE;
			} else
				state = TEXT_STATE;

			if (state == TEXT_STATE) {
				if (writer != null)
					writer.println(line);
				if (i < lines.size() - 1)
					continue;
			}

			if (writer != null) {
				if (writerState == STACK_STATE && current != null) {
					current.setStack(swriter.toString());
				} else if (writerState == SESSION_STATE && session != null) {
					session.setSessionData(swriter.toString());
				}
				writerState = UNKNOWN_STATE;
				swriter = null;
				writer.close();
				writer = null;
			}

			if (state == STACK_STATE) {
				swriter = new StringWriter();
				writer = new PrintWriter(swriter, true);
				writerState = STACK_STATE;
			} else if (state == SESSION_STATE) {
				session = new LogSession();
				swriter = new StringWriter();
				writer = new PrintWriter(swriter, true);
				writerState = SESSION_STATE;
			} else if (state == ENTRY_STATE) {
				LogEntry entry = new LogEntry();
				entry.setSession(session);
				entry.processLogLine(line, true);
				setNewParent(parents, entry, 0);
				current = entry;
				entries.add(0, entry);
			} else if (state == SUBENTRY_STATE) {
				LogEntry entry = new LogEntry();
				entry.setSession(session);
				int depth = entry.processLogLine(line, false);
				setNewParent(parents, entry, depth);
				current = entry;
				LogEntry parent = (LogEntry) parents.get(depth - 1);
				parent.addChild(entry);
			} else if (state == MESSAGE_STATE) {
				String message = "";
				if (line.length() > 8)
					message = line.substring(9);
				if (current != null)
					current.setMessage(message);
			}
		}
		return (LogEntry[]) entries.toArray(new LogEntry[entries.size()]);
	}	
	
	private static void setNewParent(
		ArrayList parents,
		LogEntry entry,
		int depth) {
		if (depth + 1 > parents.size())
			parents.add(entry);
		else
			parents.set(depth, entry);
	}
}