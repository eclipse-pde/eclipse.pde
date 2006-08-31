/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;


import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.Bundle;


/**
 * Provides a set of convenience methods for creating HTML pages.
 */
public class HTMLPrinter {

	private static URL fJavaDocStyleSheet = null;

	public static void insertPageProlog(StringBuffer buffer, int position, URL styleSheetURL) {

		StringBuffer pageProlog= new StringBuffer(300);

		pageProlog.append("<html><head>"); //$NON-NLS-1$
		pageProlog.append("<style type=\"text/css\">body {margin:0px;color:#000000;}</style>"); //$NON-NLS-1$
		appendStyleSheetURL(pageProlog, styleSheetURL);
		pageProlog.append("</head>"); //$NON-NLS-1$
		pageProlog.append("<body>"); //$NON-NLS-1$
		
		buffer.insert(position,  pageProlog.toString());
	}

	public static void insertStyles(StringBuffer buffer, String[] styles) {
		if (styles == null || styles.length == 0)
			return;

		StringBuffer styleBuf= new StringBuffer(10 * styles.length);
		for (int i= 0; styles != null && i < styles.length; i++) {
			styleBuf.append(" style=\""); //$NON-NLS-1$
			styleBuf.append(styles[i]);
			styleBuf.append('"');
		}

		// Find insertion index
		int index= buffer.indexOf("<body "); //$NON-NLS-1$
		if (index == -1)
			return;

		buffer.insert(index+5, styleBuf);
	}

	private static void appendStyleSheetURL(StringBuffer buffer, URL styleSheetURL) {
		if (styleSheetURL == null)
			return;

		buffer.append("<link rel=\"stylesheet\" href= \""); //$NON-NLS-1$
		buffer.append(styleSheetURL);
		buffer.append("\" charset=\"ISO-8859-1\" type=\"text/css\" />"); //$NON-NLS-1$
	}

	public static void addPageEpilog(StringBuffer buffer) {
		buffer.append("</body></html>"); //$NON-NLS-1$
	}
	
	public static URL getJavaDocStyleSheerURL() {
		if (fJavaDocStyleSheet == null) {
			Bundle bundle= Platform.getBundle(IPDEUIConstants.PLUGIN_ID);
			fJavaDocStyleSheet= bundle.getEntry("/JavadocHoverStyleSheet.css"); //$NON-NLS-1$
			if (fJavaDocStyleSheet != null) {
				try {
					fJavaDocStyleSheet= FileLocator.toFileURL(fJavaDocStyleSheet);
				} catch (IOException ex) {
					PDEPlugin.log(ex);
				}
			}
		}
		return fJavaDocStyleSheet;
	}
}
