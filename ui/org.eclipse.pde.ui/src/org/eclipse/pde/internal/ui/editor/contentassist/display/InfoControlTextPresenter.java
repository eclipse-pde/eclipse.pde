/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist.display;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenterExtension;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.LineBreakingReader;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

/**
 * InfoControlTextPresenter
 * Derived from org.eclipse.jdt.internal.ui.text.HTMLTextPresenter
 */
public class InfoControlTextPresenter implements IInformationPresenter, IInformationPresenterExtension {

	private static final String LINE_DELIM = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Used to ensure that text does not rest right against the right border and
	 * bottom border
	 */
	private static final int LINE_REDUCTION = 2;

	private static final String INDENT = " "; //$NON-NLS-1$

	private int fCounter;

	protected void adaptTextPresentation(TextPresentation presentation, int offset, int insertLength) {

		int yoursStart = offset;
		int yoursEnd = offset + insertLength - 1;
		yoursEnd = Math.max(yoursStart, yoursEnd);
		Iterator e = presentation.getAllStyleRangeIterator();

		while (e.hasNext()) {
			StyleRange range = (StyleRange) e.next();
			int myStart = range.start;
			int myEnd = range.start + range.length - 1;
			myEnd = Math.max(myStart, myEnd);

			if (myEnd < yoursStart)
				continue;

			if (myStart < yoursStart) {
				range.length += insertLength;
			} else {
				range.start += insertLength;
			}
		}
	}

	private void append(StringBuffer buffer, String string, TextPresentation presentation) {

		int length = string.length();
		buffer.append(string);

		if (presentation != null)
			adaptTextPresentation(presentation, fCounter, length);

		fCounter += length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter#updatePresentation(org.eclipse.swt.widgets.Display,
	 *      java.lang.String, org.eclipse.jface.text.TextPresentation, int, int)
	 */
	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
		return updatePresentation((Drawable) display, hoverInfo, presentation, maxWidth, maxHeight);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.DefaultInformationControl.IInformationPresenterExtension#updatePresentation(org.eclipse.swt.graphics.Drawable,
	 *      java.lang.String, org.eclipse.jface.text.TextPresentation, int, int)
	 */
	public String updatePresentation(Drawable drawable, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {

		if (hoverInfo == null)
			return null;

		GC gc = new GC(drawable);
		Font font = null;

		try {
			StringBuffer buffer = new StringBuffer();
			// Calculate the maximum number of lines that will fit
			// vertically
			int maxNumberOfLines = Math.round((float) maxHeight / gc.getFontMetrics().getHeight()) - LINE_REDUCTION;
			fCounter = 0;
			// Break continuous string into a set of lines that conform
			// to the maximum width allowed
			LineBreakingReader reader = new LineBreakingReader(new StringReader(hoverInfo), gc, maxWidth - LINE_REDUCTION);
			boolean lastLineFormatted = false;
			String line = reader.readLine();
			boolean lineFormatted = reader.isFormattedLine();
			boolean firstLineProcessed = false;

			while (line != null) {
				// Stop processing if the maximum number of lines is
				// exceeded
				if (maxNumberOfLines <= 0)
					break;

				if (firstLineProcessed) {
					if (!lastLineFormatted)
						// Add line delimeter
						append(buffer, LINE_DELIM, null);
					else {
						// Add line delimeter + indent
						append(buffer, LINE_DELIM, presentation);
						append(buffer, INDENT, presentation);
					}
				}
				// Add line itself
				append(buffer, line, null);
				firstLineProcessed = true;
				lastLineFormatted = lineFormatted;
				// Get the next line
				line = reader.readLine();
				lineFormatted = reader.isFormattedLine();
				// Track the number of lines left to process before
				// maxing out
				maxNumberOfLines--;
			}
			// Maximum number of lines available exceeded by available
			// content.  Trail off with ...
			if (line != null && buffer.length() > 0) {
				append(buffer, LINE_DELIM, lineFormatted ? presentation : null);
				append(buffer, PDEUIMessages.InfoControlTextPresenter_ContinuationChars, presentation);
			}
			// Pad with a space because first line is not indented
			return INDENT + buffer.toString(); //$NON-NLS-1$

		} catch (IOException e) {
			PDEPlugin.log(e);
			return null;
		} finally {
			if (font != null)
				font.dispose();
			gc.dispose();
		}
	}
}
