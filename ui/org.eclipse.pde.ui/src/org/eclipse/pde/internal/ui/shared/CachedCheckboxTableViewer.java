/*******************************************************************************
 * Copyright (c) 2025 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/**
 * Counterpart of the {@link CachedCheckboxTreeViewer} which is specialized for
 * use with {@link FilteredCheckboxTable}. This viewer caches the check state of
 * the table items. When a filter is applied the cache stores which nodes are
 * checked. When a filter is removed the viewer can be told to restore check
 * state from the cache.
 * <p>
 * Note: If duplicate items are added to the table the cache will treat them as
 * a single entry.
 * </p>
 */
public class CachedCheckboxTableViewer extends CheckboxTableViewer {

	private final Set<Object> checkState = new HashSet<>();
	private static final Object[] NO_ELEMENTS = new Object[0];

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Tree)
	 */
	protected CachedCheckboxTableViewer(Table table) {
		super(table);
		addCheckStateListener(event -> updateCheckState(event.getElement(), event.getChecked()));
		setUseHashlookup(true);
	}

	protected void updateCheckState(Object element, boolean state) {
		if (state) {
			checkState.add(element);
		} else {
			checkState.remove(element);
		}
	}

	/**
	 * Restores the checked state of items based on the cached check state.No
	 * events will be fired.
	 */
	public void restoreCachedState() {
		getTable().setRedraw(false);
		// Call the super class so we don't mess up the cache
		super.setCheckedElements(NO_ELEMENTS);
		setGrayedElements(NO_ELEMENTS);
		// Now we are only going to set the check state of the leaf nodes
		// and rely on our container checked code to update the parents properly.
		super.setCheckedElements(checkState.toArray());
		getTable().setRedraw(true);
	}

	@Override
	protected void preservingSelection(Runnable updateCode) {
		super.preservingSelection(updateCode);
		// The super class implementation will preserve a root element's check
		// mark but that can cause newly unfiltered children to become check
		// marked.
		// See https://github.com/eclipse-pde/eclipse.pde/issues/62
		restoreCachedState();
	}

	/**
	 * Returns the contents of the cached check state.  The contents will be all
	 * checked leaf nodes ignoring any filters.
	 *
	 * @return checked leaf elements
	 */
	public Object[] getUnfilteredCheckedElements() {
		return checkState.toArray(Object[]::new);
	}

	/**
	 * Returns the number of checked nodes. This method uses its internal check
	 * state cache to determine what has been checked, not what is visible in
	 * the viewer. The cache does not count duplicate items in the table.
	 *
	 * @return number of elements checked according to the cached check state
	 */
	public int getUnfilteredCheckedCount() {
		return checkState.size();
	}

	/**
	 * Returns whether {@code element} is checked. This method uses its internal
	 * check state cache to determine what has been checked, not what is visible
	 * in the viewer.
	 */
	public boolean isCheckedElement(Object element) {
		return checkState.contains(element);
	}

	@Override
	public boolean setChecked(Object element, boolean state) {
		updateCheckState(element, state);
		return super.setChecked(element, state);
	}

	@Override
	public void setCheckedElements(Object[] elements) {
		checkState.clear();
		Collections.addAll(checkState, elements);
		super.setCheckedElements(elements);
	}

	@Override
	public void setAllChecked(boolean state) {
		super.setAllChecked(state);
		if (state) {
			// Find all visible children and check them
			Object[] visible = getFilteredChildren(getRoot());
			Collections.addAll(checkState, visible);
		} else {
			// Remove any item in the check state that is visible (passes the filters)
			Object[] visible = filter(checkState.toArray());
			for (Object visibleObject : visible) {
				checkState.remove(visibleObject);
			}
		}
	}

	@Override
	public void remove(Object[] elements) {
		for (Object element : elements) {
			updateCheckState(element, false);
		}
		super.remove(elements);
	}

	@Override
	public void remove(Object element) {
		updateCheckState(element, false);
		super.remove(element);
	}
}