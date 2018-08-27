/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class TocHTMLOperation extends WorkspaceModifyOperation {

	private IFile fFile;

	private static byte[] getHTMLContent() throws CoreException {
		String indent = "   "; //$NON-NLS-1$
		String delimiter = System.getProperty("line.separator"); //$NON-NLS-1$

		StringBuilder buf = new StringBuilder();
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

		return buf.toString().getBytes(StandardCharsets.UTF_8);
	}

	public TocHTMLOperation(IFile file) {
		fFile = file;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		ByteArrayInputStream stream = new ByteArrayInputStream(getHTMLContent());
		fFile.setContents(stream, 0, monitor);

		monitor.done();
	}

}
