/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.logview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IMemento;

class LogReader {
	private static final int SESSION_STATE = 10;
	public static final long MAX_FILE_LENGTH = 1024*1024;
	private static final int ENTRY_STATE = 20;
	private static final int SUBENTRY_STATE = 30;
	private static final int MESSAGE_STATE = 40;
	private static final int STACK_STATE = 50;
	private static final int TEXT_STATE = 60;
	private static final int UNKNOWN_STATE = 70;
	
	private static LogSession currentSession;
		
	public static void parseLogFile(File file, ArrayList entries, IMemento memento) {
		ArrayList parents = new ArrayList();
		LogEntry current = null;
		LogSession session = null;
		int writerState = UNKNOWN_STATE;
		StringWriter swriter = null;
		PrintWriter writer = null;
		int state = UNKNOWN_STATE;
		currentSession = null;
		BufferedReader reader = null;
		try {
					
			reader = new BufferedReader(new InputStreamReader(
					new TailInputStream(file, MAX_FILE_LENGTH), "UTF-8")); //$NON-NLS-1$
			for (;;) {
				String line = reader.readLine();
				if (line == null)
					break;
				line = line.trim();
				if (line.length() == 0)
					continue;

				if (line.startsWith("!SESSION")) { //$NON-NLS-1$
					state = SESSION_STATE;
				} else if (line.startsWith("!ENTRY")) { //$NON-NLS-1$
					state = ENTRY_STATE;
				} else if (line.startsWith("!SUBENTRY")) { //$NON-NLS-1$
					state = SUBENTRY_STATE;
				} else if (line.startsWith("!MESSAGE")) { //$NON-NLS-1$
					state = MESSAGE_STATE;
				} else if (line.startsWith("!STACK")) { //$NON-NLS-1$
					state = STACK_STATE;
				} else
					state = TEXT_STATE;
			
				if (state == TEXT_STATE) {
					if (writer != null)
						writer.println(line);
					continue;
				}
			
				if (writer != null) {
					if (writerState == STACK_STATE && current != null) {
						current.setStack(swriter.toString());
					} else if (writerState == SESSION_STATE && session != null) {
						session.setSessionData(swriter.toString());
					} else if (writerState == MESSAGE_STATE && current != null){
						String message = current.getMessage() + swriter.toString();
						message = message.trim();
						current.setMessage(message);
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
					session.processLogLine(line);
					swriter = new StringWriter();
					writer = new PrintWriter(swriter, true);
					writerState = SESSION_STATE;
					updateCurrentSession(session);
					if (!currentSession.equals(session) && !memento.getString(LogView.P_SHOW_ALL_SESSIONS).equals("true")) //$NON-NLS-1$
						entries.clear();
				} else if (state == ENTRY_STATE) {
					LogEntry entry = new LogEntry();
					entry.setSession(session);
					entry.processEntry(line);
					setNewParent(parents, entry, 0);
					current = entry;
					addEntry(current, entries, memento, false);
				} else if (state == SUBENTRY_STATE) {
					if (parents.size() > 0) {
						LogEntry entry = new LogEntry();
						entry.setSession(session);
						int depth = entry.processSubEntry(line);
						setNewParent(parents, entry, depth);
						current = entry;
						LogEntry parent = (LogEntry) parents.get(depth - 1);
						parent.addChild(entry);
					}
				} else if (state == MESSAGE_STATE) {
					swriter = new StringWriter();
					writer = new PrintWriter(swriter, true);
					String message = ""; //$NON-NLS-1$
					if (line.length() > 8)
						message = line.substring(9).trim();
					message = message.trim();
					if (current != null)
						current.setMessage(message);
					writerState = MESSAGE_STATE;
				}
			} 
			
			if (swriter != null && current != null && writerState == STACK_STATE)
				current.setStack(swriter.toString());
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e1) {
			}
			if (writer != null)
				writer.close();
		}
	}
		
	private static void updateCurrentSession(LogSession session) {
		if (currentSession == null) {
			currentSession = session;
			return;
		}		
		Date currentDate = currentSession.getDate();
		Date sessionDate = session.getDate();		
		if (currentDate == null && sessionDate != null)
			currentSession = session;
		else if (currentDate != null && sessionDate == null)
			currentSession = session;
		else if (currentDate != null && sessionDate != null && sessionDate.after(currentDate))
			currentSession = session;	
	}
	
	public static void addEntry(LogEntry current, ArrayList entries, IMemento memento, boolean useCurrentSession) {
		int severity = current.getSeverity();
		boolean doAdd = true;
		switch(severity) {
			case IStatus.INFO:
				doAdd = memento.getString(LogView.P_LOG_INFO).equals("true"); //$NON-NLS-1$
				break;
			case IStatus.WARNING:
				doAdd = memento.getString(LogView.P_LOG_WARNING).equals("true"); //$NON-NLS-1$
				break;
			case IStatus.ERROR:
				doAdd = memento.getString(LogView.P_LOG_ERROR).equals("true"); //$NON-NLS-1$
				break;
		}
		if (doAdd) {
			if (useCurrentSession)
				current.setSession(currentSession);
			entries.add(0, current);
			
			if (memento.getString(LogView.P_USE_LIMIT).equals("true") //$NON-NLS-1$
				&& entries.size() > memento.getInteger(LogView.P_LOG_LIMIT).intValue())
				entries.remove(entries.size() - 1);
		}
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
	
	public static void reset() {
		currentSession = null;
	}
}
