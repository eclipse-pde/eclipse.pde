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

import java.util.Map;

/**
 * Provides log files.
 */
public interface ILogFileProvider {

	/**
	 * Returns a Map of java.io.File log files indexed by String names.
	 * 
	 * @return Map of java.io.File log files index by String names.
	 * @since 3.4
	 */
	Map getLogSources();
}