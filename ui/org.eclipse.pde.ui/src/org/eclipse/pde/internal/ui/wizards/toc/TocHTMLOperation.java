/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.toc;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class TocHTMLOperation extends WorkspaceModifyOperation {
	
	private IFile fFile;
	private static byte[] htmlContent = null;
	private static final String 
		delimiter = System.getProperty("line.separator"); //$NON-NLS-1$
	private static final String 
		indent = "   "; //$NON-NLS-1$

	static
	{	StringBuffer buf = new StringBuffer();
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
		buf.append("Title");
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
		buf.append("Body");
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
			htmlContent = buf.toString().getBytes("UTF8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
		}
	}

	public TocHTMLOperation(IFile file)
	{	fFile = file;
	}

	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		
		ByteArrayInputStream stream = new ByteArrayInputStream(htmlContent);
		fFile.setContents(stream, 0, monitor);

        monitor.done();
	}

}
