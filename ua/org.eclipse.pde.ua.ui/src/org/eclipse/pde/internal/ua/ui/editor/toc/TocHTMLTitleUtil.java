/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TocHTMLTitleUtil {

	private static final String whitespace = "[ \\t\\n\\r\\f\\v]*"; //$NON-NLS-1$
	private static final String titleTag = "[Tt][Ii][Tt][Ll][Ee]"; //$NON-NLS-1$

	private static Pattern titlePattern = null;

	private static void initPattern() {
		StringBuffer buf = new StringBuffer();
		buf.append('<');
		buf.append(whitespace);
		buf.append(titleTag);
		buf.append('>');
		buf.append("(.*?)"); //$NON-NLS-1$
		buf.append('<');
		buf.append(whitespace);
		buf.append('/');
		buf.append(whitespace);
		buf.append(titleTag);
		buf.append('>');

		titlePattern = Pattern.compile(buf.toString());
	}

	public static String findTitle(File f) {
		if (titlePattern == null) {
			initPattern();
		}

		try {
			FileChannel fc = new FileInputStream(f).getChannel();

			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

			CharBuffer cb = Charset.forName("8859_1").newDecoder().decode(bb); //$NON-NLS-1$

			Matcher m = titlePattern.matcher(cb);
			String title = null;
			if (m.find()) {
				title = m.group(1);
			}

			return title;
		} catch (IOException e) {
			return null;
		}
	}
}
