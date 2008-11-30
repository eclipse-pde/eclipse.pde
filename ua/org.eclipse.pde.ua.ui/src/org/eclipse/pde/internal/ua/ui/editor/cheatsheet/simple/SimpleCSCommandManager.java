/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private ListenerList fListeners;

	private static SimpleCSCommandManager fPinstance;

	private Map fCommandMap;

	private boolean fBlockEvents;

	/**
	 * 
	 */
	private SimpleCSCommandManager() {
		fCommandMap = Collections.synchronizedMap(new HashMap());
		fBlockEvents = false;
		fListeners = null;
	}

	/**
	 * @param block
	 */
	public void setBlockEvents(boolean block) {
		fBlockEvents = block;
	}

	/**
	 * @return
	 */
	public boolean getBlockEvents() {
		return fBlockEvents;
	}

	/**
	 * @return
	 */
	public static SimpleCSCommandManager Instance() {
		if (fPinstance == null) {
			fPinstance = new SimpleCSCommandManager();
		}
		return fPinstance;
	}

	/**
	 * @param key
	 * @param value
	 * @return 
	 */
	public synchronized boolean put(String key, String value) {
		// Do not add the key-value pair if it is already in the map
		if (fCommandMap.containsKey(key)) {
			String presentValue = (String) fCommandMap.get(key);
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

	/**
	 * @param key
	 * @return
	 */
	public String get(String key) {
		return (String) fCommandMap.get(key);
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean hasKey(String key) {
		return fCommandMap.containsKey(key);
	}

	/**
	 * @return
	 */
	public Set getKeys() {
		return fCommandMap.keySet();
	}

	/**
	 * @return
	 */
	public int getSize() {
		return fCommandMap.size();
	}

	/**
	 * @param listener
	 */
	public void addCommandKeyListener(ISimpleCSCommandKeyListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList();
		}
		fListeners.add(listener);
	}

	/**
	 * @param listener
	 */
	public void removeCommandKeyListener(ISimpleCSCommandKeyListener listener) {
		if (fListeners == null) {
			return;
		}
		fListeners.remove(listener);
	}

	/**
	 * @param key
	 * @param value
	 */
	private void fireNewCommandKeyEvent(String key, String value) {
		// Do not fire the event if there are no listeners or we are blocking
		// events
		if ((fBlockEvents == true) || (fListeners == null) || (fListeners.size() == 0)) {
			return;
		}
		// Create the event
		NewCommandKeyEvent event = new NewCommandKeyEvent(this, key, value);
		// Notify all listeners
		Object[] listenerList = fListeners.getListeners();
		for (int i = 0; i < fListeners.size(); i++) {
			ISimpleCSCommandKeyListener listener = (ISimpleCSCommandKeyListener) listenerList[i];
			listener.newCommandKey(event);
		}
	}

}
