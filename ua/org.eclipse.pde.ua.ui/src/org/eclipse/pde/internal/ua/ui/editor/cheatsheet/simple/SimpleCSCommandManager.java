/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;

/**
 * SimpleCSCommandManager
 *
 */
public class SimpleCSCommandManager {

	private ListenerList<ISimpleCSCommandKeyListener> fListeners;

	private static SimpleCSCommandManager fPinstance;

	private Map<String, String> fCommandMap;

	private boolean fBlockEvents;

	private SimpleCSCommandManager() {
		fCommandMap = Collections.synchronizedMap(new HashMap<>());
		fBlockEvents = false;
		fListeners = null;
	}

	public void setBlockEvents(boolean block) {
		fBlockEvents = block;
	}

	public boolean getBlockEvents() {
		return fBlockEvents;
	}

	public static SimpleCSCommandManager Instance() {
		if (fPinstance == null) {
			fPinstance = new SimpleCSCommandManager();
		}
		return fPinstance;
	}

	public synchronized boolean put(String key, String value) {
		// Do not add the key-value pair if it is already in the map
		if (fCommandMap.containsKey(key)) {
			String presentValue = fCommandMap.get(key);
			if ((presentValue == null) && (value == null)) {
				// Key-value pair not added
				return false;
			} else if ((presentValue != null) && (presentValue.equals(value))) {
				// Key-value pair not added
				return false;
			}
		}
		// Insert the key-value pair into the map
		fCommandMap.put(key, value);
		// Notify all listeners of the new key-value pair
		fireNewCommandKeyEvent(key, value);
		// Key-value pair added
		return true;
	}

	public String get(String key) {
		return fCommandMap.get(key);
	}

	public boolean hasKey(String key) {
		return fCommandMap.containsKey(key);
	}

	public Set<String> getKeys() {
		return fCommandMap.keySet();
	}

	public int getSize() {
		return fCommandMap.size();
	}

	public void addCommandKeyListener(ISimpleCSCommandKeyListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList<>();
		}
		fListeners.add(listener);
	}

	public void removeCommandKeyListener(ISimpleCSCommandKeyListener listener) {
		if (fListeners == null) {
			return;
		}
		fListeners.remove(listener);
	}

	private void fireNewCommandKeyEvent(String key, String value) {
		// Do not fire the event if there are no listeners or we are blocking
		// events
		if ((fBlockEvents == true) || (fListeners == null) || (fListeners.isEmpty())) {
			return;
		}
		// Create the event
		NewCommandKeyEvent event = new NewCommandKeyEvent(this, key, value);
		// Notify all listeners
		for (ISimpleCSCommandKeyListener listener : fListeners) {
			listener.newCommandKey(event);
		}
	}

}
