/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 202583
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.ibm.icu.text.SimpleDateFormat;

public class LogSession extends PlatformObject implements IWorkbenchAdapter {
	private String sessionData;
	private Date date;
	private List entries = new ArrayList();

	/**
	 * Constructor for LogSession.
	 */
	public LogSession() {
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(String dateString) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
		try {
			date = formatter.parse(dateString);
		} catch (ParseException e) {
		}
	}
	
	public String getSessionData() {
		return sessionData;
	}

	void setSessionData(String data) {
		this.sessionData = data;
	}
	
	public void processLogLine(String line) {
		// process "!SESSION <dateUnknownFormat> ----------------------------"
		line = line.substring(9); // strip "!SESSION "
		int delim = line.indexOf("----"); //$NON-NLS-1$ // single "-" may be in date, so take few for sure
		if (delim == -1)
			return;
		
		String dateBuffer = line.substring(0, delim).trim();
		setDate(dateBuffer);
	}
	
	public List getEntries() {
		return entries;
	}

	public Object[] getChildren(Object o) {
		return getEntries().toArray(new LogEntry[getEntries().size()]);
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	public String getLabel(Object o) {
		return null;
	}

	public Object getParent(Object o) {
		return null;
	}
}
