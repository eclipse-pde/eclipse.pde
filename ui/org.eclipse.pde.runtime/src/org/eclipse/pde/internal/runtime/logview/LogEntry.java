package org.eclipse.pde.internal.runtime.logview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class LogEntry extends PlatformObject implements IWorkbenchAdapter {
	private static final String KEY_ERROR = "LogView.severity.error";
	private static final String KEY_WARNING = "LogView.severity.warning";
	private static final String KEY_INFO = "LogView.severity.info";
	private ArrayList children;
	private LogEntry parent;
	private String pluginId;
	private int severity;
	private int code;
	private String date;
	private String message;
	private String stack;

	public LogEntry() {
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
	public String getDate() {
		return date;
	}
	public String getSeverityText() {
		return getSeverityText(severity);
	}
	public boolean hasChildren() {
		return children != null && children.size() > 0;
	}
	public String toString() {
		return getSeverityText();
	}
	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		if (children == null)
			return new Object[0];
		return children.toArray();
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

	/**
	 * @see IWorkbenchAdapter#getParent(Object)
	 */
	public Object getParent(Object obj) {
		return parent;
	}

	void setParent(LogEntry parent) {
		this.parent = parent;
	}

	private String getSeverityText(int severity) {
		switch (severity) {
			case IStatus.ERROR :
				return PDERuntimePlugin.getResourceString(KEY_ERROR);
			case IStatus.WARNING :
				return PDERuntimePlugin.getResourceString(KEY_WARNING);
			case IStatus.INFO :
				return PDERuntimePlugin.getResourceString(KEY_INFO);
		}
		return "?";
	}

	int processLogLine(String line, boolean root) {
		//!ENTRY <pluginID> <severity> <code> <date>
		//!SUBENTRY <depth> <pluginID> <severity> <code> <date>
		StringTokenizer stok = new StringTokenizer(line, " ", true);
		StringBuffer dateBuffer = new StringBuffer();

		int dateCount = 5;
		int depth = 0;
		for (int i = 0; stok.hasMoreTokens();) {
			String token = stok.nextToken();
			if (i >= dateCount) {
				dateBuffer.append(token);
				continue;
			} else if (token.equals(" "))
				continue;
			switch (i) {
				case 0 : // entry or subentry
					if (root) i+=2;
					else i++;
					break;
				case 1 : // depth
					depth = parseInteger(token);
					i++;
					break;
				case 2 :
					pluginId = token;
					i++;
					break;
				case 3 : // severity
					severity = parseInteger(token);
					i++;
					break;
				case 4 : // code
					code = parseInteger(token);
					i++;
					break;
			}
		}
		date = dateBuffer.toString();
		return depth;
	}
	
	private int parseInteger(String token) {
		try {
			return Integer.parseInt(token);
		}
		catch (NumberFormatException e) {
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
		date = 	new Date().toString();
		message = status.getMessage();
		Throwable throwable = status.getException();
		if (throwable != null) {
			StringBuffer buffer = new StringBuffer();
			StringWriter swriter = new StringWriter();
			PrintWriter pwriter = new PrintWriter(swriter);
			throwable.printStackTrace(pwriter);
			pwriter.flush();
			pwriter.close();
			stack = swriter.toString();
		}
		IStatus[] schildren = status.getChildren();
		if (schildren.length > 0) {
			children = new ArrayList();
			for (int i = 0; i < schildren.length; i++) {
				LogEntry child = new LogEntry(schildren[i]);
				addChild(child);
			}
		}
	}
	void addChild(LogEntry child) {
		if (children == null)
			children = new ArrayList();
		children.add(child);
		child.setParent(this);
	}
}