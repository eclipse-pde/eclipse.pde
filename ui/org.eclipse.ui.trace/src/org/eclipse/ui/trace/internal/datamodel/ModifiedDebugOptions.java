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
package org.eclipse.ui.trace.internal.datamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility handler for storing the options that were changed
 */
public class ModifiedDebugOptions {

	/**
	 * Construct a new ModifiedDebugOptions object
	 */
	public ModifiedDebugOptions() {
		debugOptionsToAdd = new ArrayList<TracingComponentDebugOption>();
		debugOptionsToRemove = new ArrayList<TracingComponentDebugOption>();
	}

	/**
	 * Accessor for an array of the {@link TracingComponentDebugOption} items that were selected to be added on the
	 * tracing preference page.
	 * 
	 * @return An array of the {@link TracingComponentDebugOption} items that were selected to be added on the tracing
	 *         preference page
	 */
	public final TracingComponentDebugOption[] getDebugOptionsToAdd() {
		return debugOptionsToAdd.toArray(new TracingComponentDebugOption[debugOptionsToAdd.size()]);
	}

	/**
	 * Accessor for an array of the {@link TracingComponentDebugOption} items that were selected to be removed on the
	 * tracing preference page.
	 * 
	 * @return An array of the {@link TracingComponentDebugOption} items that were selected to be removed on the tracing
	 *         preference page
	 */
	public final TracingComponentDebugOption[] getDebugOptionsToRemove() {
		return debugOptionsToRemove.toArray(new TracingComponentDebugOption[debugOptionsToRemove.size()]);
	}

	/**
	 * Adds a new {@link TracingComponentDebugOption} to the list of debug options to add
	 * 
	 * @param option
	 *            The {@link TracingComponentDebugOption} option to add
	 */
	public final void addDebugOption(final TracingComponentDebugOption option) {
		if (option != null) {
			boolean isBeingRemoved = debugOptionsToRemove.contains(option);
			if (isBeingRemoved) {
				// remove it from the list of debug options to remove
				debugOptionsToRemove.remove(option);
			}
			// add it to the list of debug options to add
			debugOptionsToAdd.add(option);
		}
	}

	/**
	 * Adds a new {@link TracingComponentDebugOption} to the list of debug options to remove
	 * 
	 * @param option
	 *            The {@link TracingComponentDebugOption} option to add
	 */
	public final void removeDebugOption(final TracingComponentDebugOption option) {
		if (option != null) {
			boolean isBeingAdded = debugOptionsToAdd.contains(option);
			if (isBeingAdded) {
				// remove it from the list of debug options to add
				debugOptionsToAdd.remove(option);
			}
			// add it to the list of debug options to remove
			debugOptionsToRemove.add(option);
		}
	}

	/**
	 * Purge the list of bundles to add and remove
	 */
	public final void clear() {
		debugOptionsToAdd.clear();
		debugOptionsToRemove.clear();
	}

	/**
	 * A list of the {@link TracingComponentDebugOption} instances to be added.
	 */
	private List<TracingComponentDebugOption> debugOptionsToAdd = null;

	/**
	 * A list of the {@link TracingComponentDebugOption} instances to be removed.
	 */
	private List<TracingComponentDebugOption> debugOptionsToRemove = null;
}