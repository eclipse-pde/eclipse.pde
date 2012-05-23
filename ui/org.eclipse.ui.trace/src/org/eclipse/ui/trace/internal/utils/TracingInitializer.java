/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
