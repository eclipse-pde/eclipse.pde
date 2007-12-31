/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bugs 209474, 207344
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import com.ibm.icu.text.SimpleDateFormat;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class LogEntry extends AbstractEntry {

	public static final String F_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; //$NON-NLS-1$
	private static final SimpleDateFormat F_SDF = new SimpleDateFormat(F_DATE_FORMAT);

	private String pluginId;
	private int severity;
	private int code;
	private String fDateString;
	private Date fDate;
	private String message;
	private String stack;
	private LogSession session;

	public LogEntry() { // do nothing
	}

	public LogSession getSession() {
		if ((session == null) && (parent != null) && (parent instanceof LogEntry))
			return ((LogEntry) parent).getSession();

		return session;
	}

	void setSession(LogSession session) {
		this.session = session;
	}

	public LogEntry(IStatus status) {
		processStatus(status);
	}

	public int getSeverity() {
		return severity;
	}

	public boolean isOK() {
		return severity == IStatus.OK;
	}

	public int getCode() {
		return code;
	}

	public String getPluginId() {
		return pluginId;
	}

	public String getMessage() {
		return message;
	}

	public String getStack() {
		return stack;
	}

	public String getFormattedDate() {
		if (fDateString == null)
			fDateString = F_SDF.format(getDate());
		return fDateString;
	}

	public Date getDate() {
		if (fDate == null)
			fDate = new Date(0); // unknown date - return epoch
		return fDate;
	}

	public String getSeverityText() {
		return getSeverityText(severity);
	}

	public String toString() {
		return getSeverityText();
	}

	/**
	 * @see IWorkbenchAdapter#getImageDescriptor(Object)
	 */
	public ImageDescriptor getImageDescriptor(Object arg0) {
		return null;
	}

	/**
	 * @see IWorkbenchAdapter#getLabel(Object)
	 */
	public String getLabel(Object obj) {
		return getSeverityText();
	}

	private String getSeverityText(int severity) {
		switch (severity) {
			case IStatus.ERROR :
				return Messages.LogView_severity_error;
			case IStatus.WARNING :
				return Messages.LogView_severity_warning;
			case IStatus.INFO :
				return Messages.LogView_severity_info;
			case IStatus.OK :
				return Messages.LogView_severity_ok;
		}
		return "?"; //$NON-NLS-1$
	}

	void processEntry(String line) {
		//!ENTRY <pluginID> <severity> <code> <date>
		//!ENTRY <pluginID> <date> if logged by the framework!!!
		StringTokenizer stok = new StringTokenizer(line, " "); //$NON-NLS-1$
		int tokenCount = stok.countTokens();
		boolean noSeverity = stok.countTokens() < 5;

		// no severity means it should be represented as OK
		if (noSeverity) {
			severity = 0;
			code = 0;
		}
		StringBuffer dateBuffer = new StringBuffer();
		for (int i = 0; i < tokenCount; i++) {
			String token = stok.nextToken();
			switch (i) {
				case 0 :
					break;
				case 1 :
					pluginId = token;
					break;
				case 2 :
					if (noSeverity) {
						if (dateBuffer.length() > 0)
							dateBuffer.append(" "); //$NON-NLS-1$
						dateBuffer.append(token);
					} else {
						severity = parseInteger(token);
					}
					break;
				case 3 :
					if (noSeverity) {
						if (dateBuffer.length() > 0)
							dateBuffer.append(" "); //$NON-NLS-1$
						dateBuffer.append(token);
					} else
						code = parseInteger(token);
					break;
				default :
					if (dateBuffer.length() > 0)
						dateBuffer.append(" "); //$NON-NLS-1$
					dateBuffer.append(token);
			}
		}
		try {
			Date date = F_SDF.parse(dateBuffer.toString());
			if (date != null) {
				fDate = date;
				fDateString = F_SDF.format(fDate);
			}
		} catch (ParseException e) { // do nothing
		}
	}

	int processSubEntry(String line) {
		//!SUBENTRY <depth> <pluginID> <severity> <code> <date>
		//!SUBENTRY  <depth> <pluginID> <date>if logged by the framework!!!
		StringTokenizer stok = new StringTokenizer(line, " "); //$NON-NLS-1$
		int tokenCount = stok.countTokens();
		boolean byFrameWork = stok.countTokens() < 5;

		StringBuffer dateBuffer = new StringBuffer();
		int depth = 0;
		for (int i = 0; i < tokenCount; i++) {
			String token = stok.nextToken();
			switch (i) {
				case 0 :
					break;
				case 1 :
					depth = parseInteger(token);
					break;
				case 2 :
					pluginId = token;
					break;
				case 3 :
					if (byFrameWork) {
						if (dateBuffer.length() > 0)
							dateBuffer.append(" "); //$NON-NLS-1$
						dateBuffer.append(token);
					} else {
						severity = parseInteger(token);
					}
					break;
				case 4 :
					if (byFrameWork) {
						if (dateBuffer.length() > 0)
							dateBuffer.append(" "); //$NON-NLS-1$
						dateBuffer.append(token);
					} else
						code = parseInteger(token);
					break;
				default :
					if (dateBuffer.length() > 0)
						dateBuffer.append(" "); //$NON-NLS-1$
					dateBuffer.append(token);
			}
		}
		try {
			Date date = F_SDF.parse(dateBuffer.toString());
			if (date != null) {
				fDate = date;
				fDateString = F_SDF.format(fDate);
			}
		} catch (ParseException e) { // do nothing
		}
		return depth;
	}

	private int parseInteger(String token) {
		try {
			return Integer.parseInt(token);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	void setStack(String stack) {
		this.stack = stack;
	}

	void setMessage(String message) {
		this.message = message;
	}

	private void processStatus(IStatus status) {
		pluginId = status.getPlugin();
		severity = status.getSeverity();
		code = status.getCode();
		fDate = new Date();
		fDateString = F_SDF.format(fDate);
		message = status.getMessage();
		Throwable throwable = status.getException();
		if (throwable != null) {
			StringWriter swriter = new StringWriter();
			PrintWriter pwriter = new PrintWriter(swriter);
			throwable.printStackTrace(pwriter);
			pwriter.flush();
			pwriter.close();
			stack = swriter.toString();
		}
		IStatus[] schildren = status.getChildren();
		if (schildren.length > 0) {
			for (int i = 0; i < schildren.length; i++) {
				LogEntry child = new LogEntry(schildren[i]);
				addChild(child);
			}
		}
	}

	public void write(PrintWriter writer) {
		if (session != null)
			writer.println(session.getSessionData());
		writer.println(getSeverityText());
		if (fDate != null)
			writer.println(getDate());

		if (message != null)
			writer.println(getMessage());

		if (stack != null) {
			writer.println();
			writer.println(stack);
		}
	}
}
