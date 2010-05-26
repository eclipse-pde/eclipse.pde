/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

/**
 * Contains strings and methods for writing HTML markup
 * 
 * @since 1.0.1
 */
public abstract class HTMLConvertor {

	/**
	 * Default file extension for HTML files: <code>.html</code> 
	 */
	public static final String HTML_EXTENSION = ".html"; //$NON-NLS-1$
	/**
	 * Default file extension for XML files: <code>.xml</code>
	 */
	public static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	/**
	 * Standard HTML file prefix
	 */
	public static final String HTML_HEADER = "<!doctype HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"; //$NON-NLS-1$
	/**
	 * Meta tag for default HTML content type
	 */
	public static final String CONTENT_TYPE_META = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"; //$NON-NLS-1$
	/**
	 * Standard W3C footer image + link
	 */
	public static final String W3C_FOOTER = "<p>\n\t<a href=\"http://validator.w3.org/check?uri=referer\">\n<img src=\"http://www.w3.org/Icons/valid-html401-blue\" alt=\"Valid HTML 4.01 Transitional\" height=\"31\" width=\"88\"></a>\n</p>\n"; //$NON-NLS-1$
	/**
	 * Opening title tag: <code>&lt;title&gt;</code>
	 */
	public static final String OPEN_TITLE = "<title>"; //$NON-NLS-1$
	/**
	 * Closing title tag: <code>&lt;/title&gt;</code>
	 */
	public static final String CLOSE_TITLE = "</title>\n"; //$NON-NLS-1$
	/**
	 * Opening head tag: <code>&lt;head&gt;</code>
	 */
	public static final String OPEN_HEAD = "<head>\n"; //$NON-NLS-1$
	/**
	 * Closing head tag: <code>&lt;/head&gt;</code>
	 */
	public static final String CLOSE_HEAD = "</head>\n"; //$NON-NLS-1$
	/**
	 * Opening body tag: <code>&lt;body&gt;</code>
	 */
	public static final String OPEN_BODY = "<body>\n"; //$NON-NLS-1$
	/**
	 * Closing body tag: <code>&lt;/body&gt;</code>
	 */
	public static final String CLOSE_BODY = "</body>\n"; //$NON-NLS-1$
	/**
	 * Opening h3 tag: <code>&lt;h3&gt;</code>
	 */
	public static final String OPEN_H3 = "<h3>"; //$NON-NLS-1$
	/**
	 * Closing h3 tag: <code>&lt;/h3&gt;</code>
	 */
	public static final String CLOSE_H3 = "</h3>\n"; //$NON-NLS-1$
	/**
	 * Opening html tag: <code>&lt;html&gt;</code>
	 */
	public static final String OPEN_HTML = "<html>\n"; //$NON-NLS-1$
	/**
	 * Closing html tag: <code>&lt;html&gt;</code>
	 */
	public static final String CLOSE_HTML = "</html>\n"; //$NON-NLS-1$
	/**
	 * Closing table tag: <code>&lt;/table&gt;</code>
	 */
	public static final String CLOSE_TABLE = "</table>\n"; //$NON-NLS-1$
	/**
	 * Opening td tag: <code>&lt;td&gt;</code>
	 */
	public static final String OPEN_TD = "<td>"; //$NON-NLS-1$
	/**
	 * Closing td tag: <code>&lt;/td&gt;</code>
	 */
	public static final String CLOSE_TD = "</td>\n"; //$NON-NLS-1$
	/**
	 * Opening li tag: <code>&lt;li&gt;</code>
	 */
	public static final String OPEN_LI = "\t<li>"; //$NON-NLS-1$
	/**
	 * Closing li tag: <code>&lt;/li&gt;</code>
	 */
	public static final String CLOSE_LI = "</li>\n"; //$NON-NLS-1$
	/**
	 * Opening p tag: <code>&lt;p&gt;</code>
	 */
	public static final String OPEN_P = "<p>"; //$NON-NLS-1$
	/**
	 * Closing p tag: <code>&lt;/p&gt;</code>
	 */
	public static final String CLOSE_P = "</p>\n"; //$NON-NLS-1$	
	/**
	 * Opening ol tag: <code>&lt;ol&gt;</code>
	 */
	public static final String OPEN_OL = "<ol>\n"; //$NON-NLS-1$
	/**
	 * Closing ol tag: <code>&lt;/ol&gt;</code>
	 */
	public static final String CLOSE_OL = "</ol>\n"; //$NON-NLS-1$
	/**
	 * Opening ul tag: <code>&lt;ul&gt;</code>
	 */
	public static final String OPEN_UL = "<ul>\n"; //$NON-NLS-1$
	/**
	 * Closing ul tag: <code>&lt;/ul&gt;</code>
	 */
	public static final String CLOSE_UL = "</ul>\n"; //$NON-NLS-1$
	/**
	 * Opening tr tag: <code>&lt;tr&gt;</code>
	 */
	public static final String OPEN_TR = "<tr>\n"; //$NON-NLS-1$
	/**
	 * Closing tr tag: <code>&lt;/tr&gt;</code>
	 */
	public static final String CLOSE_TR = "</tr>\n"; //$NON-NLS-1$
	/**
	 * Closing div tag: <code>&lt;/div&gt;</code>
	 */
	public static final String CLOSE_DIV = "</div>\n"; //$NON-NLS-1$
	/**
	 * Break tag: <code>&lt;br&gt;</code>
	 */
	public static final String BR = "<br>"; //$NON-NLS-1$
	/**
	 * Closing a tag: <code>&lt;/a&gt;</code>
	 */
	public static final String CLOSE_A = "</a>\n"; //$NON-NLS-1$
	/**
	 * Opening b tag: <code>&lt;b&gt;</code>
	 */
	public static final String OPEN_B = "<b>"; //$NON-NLS-1$
	/**
	 * Closing b tag: <code>&lt;/b&gt;</code>
	 */
	public static final String CLOSE_B = "</b>"; //$NON-NLS-1$
	/**
	 * Closing h4 tag: <code>&lt;/h4&gt;</code>
	 */
	public static final String CLOSE_H4 = "</h4>\n"; //$NON-NLS-1$
	/**
	 * Opening h4 tag: <code>&lt;h4&gt;</code>
	 */
	public static final String OPEN_H4 = "<h4>"; //$NON-NLS-1$
	
	/**
	 * Opens a new <code>&lt;td&gt;</code> with the given width attribute set
	 * @param width
	 * @return a new open <code>&lt;td&gt;</code> tag
	 */
	public static String openTD(int width) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<td width=\"").append(width).append("%\">");  //$NON-NLS-1$//$NON-NLS-2$
		return buffer.toString();
	}
}
