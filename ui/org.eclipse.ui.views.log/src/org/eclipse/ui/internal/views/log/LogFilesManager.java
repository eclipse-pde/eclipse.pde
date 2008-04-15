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
package org.eclipse.ui.internal.views.log;

import java.util.*;

/**
 * Manages the log file providers.
 * One adds log file provider to let Log View know where to find log files.
 */
public class LogFilesManager {

	private static List logFileProviders = new ArrayList();

	/**
	 * Adds log file provider.
	 * Has no effect if an identical provider is already registered. 
	 */
	public static void addLogFileProvider(ILogFileProvider provider) {
		if (!logFileProviders.contains(provider)) {
			logFileProviders.add(provider);
		}
	}

	/**
	 * Removes log file provider.
	 * Has no effect if an identical provider is already removed.
	 */
	public static void removeLogFileProvider(ILogFileProvider provider) {
		logFileProviders.remove(provider);
	}

	/**
	 * Returns the list of logs.
	 */
	static Map getLogSources() {
		ILogFileProvider[] providers = (ILogFileProvider[]) logFileProviders.toArray(new ILogFileProvider[logFileProviders.size()]);
		Map result = new HashMap(providers.length);

		for (int i = 0; i < providers.length; i++) {
			ILogFileProvider provider = providers[i];

			Map sources = provider.getLogSources();
			result.putAll(sources);
		}

		return result;
	}
}
