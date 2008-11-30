/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class TocHTMLOperation extends WorkspaceModifyOperation {

	private IFile fFile;

	private static byte[] getHTMLContent() throws CoreException {
		String indent = "   "; //$NON-NLS-1$
		String delimiter = System.getProperty("line.separator"); //$NON-NLS-1$

		StringBuffer buf = new StringBuffer();
		buf.append("<!DOCTYPE HTML PUBLIC"); //$NON-NLS-1$
		buf.append(" \"-//W3C//DTD HTML 4.01 Transitional//EN\""); //$NON-NLS-1$
		buf.append(" \"http://www.w3.org/TR/html4/loose.dtd\">"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append("<html>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append("<head>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append("<title>Title</title>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append("</head>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(delimiter);

		buf.append(indent);
		buf.append("<body>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append("<h2>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append(indent);
		buf.append("Title"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append("</h2>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append("<p>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append(indent);
		buf.append("Body"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append(indent);
		buf.append("</p>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append(indent);
		buf.append("</body>"); //$NON-NLS-1$
		buf.append(delimiter);

		buf.append("</html>"); //$NON-NLS-1$
		buf.append(delimiter);

		try {
			return buf.toString().getBytes("UTF8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			PDEUserAssistanceUIPlugin.logException(e);
			IStatus status = new Status(IStatus.ERROR, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		}
	}

	public TocHTMLOperation(IFile file) {
		fFile = file;
	}

	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		ByteArrayInputStream stream = new ByteArrayInputStream(getHTMLContent());
		fFile.setContents(stream, 0, monitor);

		monitor.done();
	}

}
