/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details;

import java.util.Iterator;

import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.ISimpleCSCommandKeyListener;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.NewCommandKeyEvent;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSCommandManager;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SimpleCSCommandComboPart extends ComboPart implements ISimpleCSCommandKeyListener, DisposeListener {

	private int fNewCommandKeyIndex;

	private int fComboEntryLimit;

	public SimpleCSCommandComboPart() {
		super();
		fNewCommandKeyIndex = -1;
		fComboEntryLimit = -1;
	}

	public void addDisposeListener(DisposeListener listener) {
		if (combo == null) {
			return;
		} else if (combo instanceof Combo) {
			((Combo) combo).addDisposeListener(listener);
		} else {
			((CCombo) combo).addDisposeListener(listener);
		}
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit, int style) {
		super.createControl(parent, toolkit, style);
		// Connect to the global command manager
		SimpleCSCommandManager.Instance().addCommandKeyListener(this);
		// Register to be notified when the combo is diposed in order to
		// disconnect from the global command manager
		addDisposeListener(this);
	}

	@Override
	public void newCommandKey(NewCommandKeyEvent event) {
		// Source:  Another combo box
		String key = event.getKey();
		// Add the new key to the combo if it does not already exist
		putValueInCombo(key, fNewCommandKeyIndex);
	}

	private void putValueInCombo(String key, int index) {
		// Ensure the key does not already exist in the combo
		if (indexOf(key) != -1) {
			return;
		}
		// If we are at the combo entry limit, remove the least recent entry
		// that is not selected
		if (getItemCount() >= fComboEntryLimit) {
			removeLeastRecentEntry();
		}
		// Add the new key
		if (index < 0) {
			// Add at the end
			add(key);
		} else {
			// Add at the specified index
			add(key, index);
		}
	}

	private void removeLeastRecentEntry() {
		// The least recent entry is the last non-selected entry in the
		// reciever's list
		int entryCount = getItemCount();
		// Nothing to do if there is one entry or no entries
		if (entryCount <= 1) {
			return;
		}
		// There has to be at least two entries
		int lastEntry = entryCount - 1;
		// Remove the last entry if it is NOT selected
		// Important:  The entry may be selected for another model object;
		// since, the details page is static.  As a result, removing the last
		// entry for this model object may remove a selected entry for another
		// model object.  In that case, the entry is re-inserted into the
		// reciever when the other model object is selected again
		if (lastEntry != getSelectionIndex()) {
			remove(lastEntry);
			return;
		}
		// Last entry was selected, try the second last entry
		int secondlastEntry = lastEntry - 1;
		remove(secondlastEntry);
	}

	public void setComboEntryLimit(int limit) {
		fComboEntryLimit = limit;
	}

	public int getComboEntryLimit() {
		return fComboEntryLimit;
	}

	/**
	 * Specify the index to insert the new command key into the combo box
	 * reciever.  Applicable to new command keys obtained via new command key
	 * events (Source: other combo boxes).
	 * @param newCommandKeyIndex
	 */
	public void setNewCommandKeyIndex(int newCommandKeyIndex) {
		fNewCommandKeyIndex = newCommandKeyIndex;
	}

	public int getNewCommandKeyIndex() {
		return fNewCommandKeyIndex;
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		// Disconnect from the global command manager
		SimpleCSCommandManager.Instance().removeCommandKeyListener(this);
	}

	public void populate() {
		// Populate the combo with all the values found in the command manager
		Iterator<String> iterator = SimpleCSCommandManager.Instance().getKeys().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			add(key);
		}
	}

	public void putValue(String key, String value) {
		putValue(key, value, -1);
	}

	public void putValue(String key, String value, int index) {
		// Source:  This combo box
		// Add the new key to the combo if it does not already exist
		SimpleCSCommandManager manager = SimpleCSCommandManager.Instance();
		putValueInCombo(key, index);
		// Store the actual value in the command manager and notify the
		// other command combo boxes
		manager.put(key, value);
	}

	public String getValue(String key) {
		return SimpleCSCommandManager.Instance().get(key);
	}

}
