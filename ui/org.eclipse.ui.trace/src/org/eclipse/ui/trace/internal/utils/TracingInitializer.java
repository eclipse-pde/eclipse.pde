/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
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
package org.eclipse.ui.trace.internal.utils;

import org.eclipse.osgi.service.debug.DebugOptions;

/**
 * Reads the preferences and initialises the {@link DebugOptions} options
 */
public class TracingInitializer {

	// Empty service component to trigger the bundle loading
	// TracingUIActivator.startup sets options from the preferences

}
