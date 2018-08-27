/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.pde.ui.tests.util;

import java.io.IOException;
import java.io.Writer;

public class PDEPerfTesterUtil {

	private String fTag;

	private long fDuration;

	private long fStart;

	private long fEnd;

	private long fIteration;

	private long fTotalDuration;

	private long fAverageDuration;

	private final static long F_SECOND_IN_MS = 1000;

	private final static long F_MINUTE_IN_MS = 60000;

	private final static long F_HOUR_IN_MS = 3600000;

	/**
	 * @param tag
	 */
	public PDEPerfTesterUtil(String tag) {
		fTag = tag;
		reset();
	}

	/**
	 *
	 */
	public void reset() {
		fDuration = 0;
		fStart = 0;
		fEnd = 0;
		fIteration = 0;
		fTotalDuration = 0;
		fAverageDuration = 0;
	}

	/**
	 *
	 */
	public void start() {
		fIteration++;
		fStart = System.currentTimeMillis();
	}

	/**
	 *
	 */
	public void stop() {
		fEnd = System.currentTimeMillis();
		calculateDuration();
	}

	/**
	 *
	 */
	private void calculateDuration() {
		fDuration = (fEnd - fStart);
		fTotalDuration = fTotalDuration + fDuration;
		if (fIteration > 0) {
			fAverageDuration = fTotalDuration / fIteration;
		}
	}

	/**
	 * @param duration
	 * @return
	 */
	private String formatDuration(long duration) {

		String output = null;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		long milliseconds = 0;
		long timeDifference = duration;

		hours = (int) Math.rint(timeDifference / F_HOUR_IN_MS);
		if (hours > 0) {
			timeDifference = timeDifference - (hours * F_HOUR_IN_MS);
		}

		minutes = (int) Math.rint(timeDifference / F_MINUTE_IN_MS);
		if (minutes > 0) {
			timeDifference = timeDifference - (minutes * F_MINUTE_IN_MS);
		}

		seconds = (int) Math.rint(timeDifference / F_SECOND_IN_MS);
		if (seconds > 0) {
			timeDifference = timeDifference - (seconds * F_SECOND_IN_MS);
		}

		milliseconds = timeDifference;

		output = hours + " h " + //$NON-NLS-1$
				minutes + " m " + //$NON-NLS-1$
				seconds + " s " + //$NON-NLS-1$
				milliseconds + " ms"; //$NON-NLS-1$

		return output;
	}

	/**
	 * @param writer
	 */
	public void printDuration(Writer writer) {
		String output = formatTag() + "(" + //$NON-NLS-1$
				fIteration + "): " + //$NON-NLS-1$
				formatDuration(fDuration) + "\n"; //$NON-NLS-1$
		try {
			writer.write(output);
			writer.flush();
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * @param writer
	 */
	public void printTotalDuration(Writer writer) {
		String output = formatTag() + "(TOTAL " + //$NON-NLS-1$
				fIteration + "): " + //$NON-NLS-1$
				formatDuration(fTotalDuration) + "\n"; //$NON-NLS-1$
		try {
			writer.write(output);
			writer.flush();
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * @param writer
	 */
	public void printAverageDuration(Writer writer) {
		String output = formatTag() + "(AVERAGE " + //$NON-NLS-1$
				fIteration + "): " + //$NON-NLS-1$
				formatDuration(fAverageDuration) + "\n"; //$NON-NLS-1$
		try {
			writer.write(output);
			writer.flush();
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * @return
	 */
	private String formatTag() {
		return "[" + fTag + "]: "; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
