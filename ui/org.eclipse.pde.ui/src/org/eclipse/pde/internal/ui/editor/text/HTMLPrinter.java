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
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;


/**
 * Provides a set of convenience methods for creating HTML pages.
 */
public class HTMLPrinter {

	private static URL fJavaDocStyleSheet = null;
	private static RGB BG_COLOR_RGB;

	static {
		final Display display= Display.getDefault();
		if (display != null && !display.isDisposed()) {
			try {
				display.asyncExec(new Runnable() {
					/*
					 * @see java.lang.Runnable#run()
					 */
					public void run() {
						BG_COLOR_RGB= display.getSystemColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
					}
				});
			} catch (SWTError err) {
				// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=45294
				if (err.code != SWT.ERROR_DEVICE_DISPOSED)
					throw err;
			}
		}
	}
	
	public static void insertPageProlog(StringBuffer buffer, int position, URL styleSheetURL, boolean useColorBG) {

		StringBuffer pageProlog= new StringBuffer(300);

		pageProlog.append("<html><head>"); //$NON-NLS-1$
		pageProlog.append("<style type=\"text/css\">"); //$NON-NLS-1$
		pageProlog.append(getBodyStyle(useColorBG));
		pageProlog.append("</style>"); //$NON-NLS-1$
		appendStyleSheetURL(pageProlog, styleSheetURL);
		pageProlog.append("</head>"); //$NON-NLS-1$
		pageProlog.append("<body>"); //$NON-NLS-1$
		
		buffer.insert(position,  pageProlog.toString());
	}

	private static String getBodyStyle(boolean useInfoBG) {
		StringBuffer sb = new StringBuffer("body {margin:0px;color:#000000;"); //$NON-NLS-1$
		if (useInfoBG && BG_COLOR_RGB != null) {
			sb.append("background-color:"); //$NON-NLS-1$
			appendColor(sb, BG_COLOR_RGB);
			sb.append(';');
		}
		sb.append('}');
		return sb.toString();
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

		buffer.append("<link rel=\"stylesheet\" href=\""); //$NON-NLS-1$
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
	
	private static void appendColor(StringBuffer buffer, RGB rgb) {
		buffer.append('#');
		buffer.append(Integer.toHexString(rgb.red));
		buffer.append(Integer.toHexString(rgb.green));
		buffer.append(Integer.toHexString(rgb.blue));
	}
}
