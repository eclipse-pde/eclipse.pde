/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.shared;

import java.util.*;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

/**
 * Copy of ContainerCheckedTreeViewer which is specialized for use
 * with {@link FilteredCheckboxTree}.  This container caches
 * the check state of leaf nodes in the tree.  When a filter
 * is applied the cache stores which nodes are checked.  When
 * a filter is removed the viewer can be told to restore check
 * state from the cache.  This viewer updates the check state of
 * parent items the same way as {@link CachedCheckboxTreeViewer}
 * <p>
 * Note: If duplicate items are added to the tree the cache will treat them
 * as a single entry.
 * </p>
 */
public class CachedCheckboxTreeViewer extends ContainerCheckedTreeViewer {

	private Set<Object> checkState = new HashSet<>();
	private static final Object[] NO_ELEMENTS = new Object[0];

	/**
	 * Constructor for ContainerCheckedTreeViewer.
	 * @see CheckboxTreeViewer#CheckboxTreeViewer(Tree)
	 */
	protected CachedCheckboxTreeViewer(Tree tree) {
		super(tree);
		addCheckStateListener(event -> updateCheckState(event.getElement(), event.getChecked()));
		setUseHashlookup(true);
	}

	protected void updateCheckState(Object element, boolean state) {
		if (state) {
			// Add the item (or its children) to the cache
			if (checkState == null) {
				checkState = new HashSet<>();
			}

			Object[] children = getFilteredChildren(element);
			if (children != null && children.length > 0) {
				for (Object child : children) {
					updateCheckState(child, state);
				}
			} else if (!checkState.contains(element)) {
				// Check if already added to avoid concurrent modification exceptions
				checkState.add(element);
			}
		} else if (checkState != null) {
			// Remove the item (or its children) from the cache

			Object[] children = getFilteredChildren(element);
			if (children.length > 0) {
				for (Object child : children) {
					updateCheckState(child, state);
				}
			}
			checkState.remove(element);
		}
	}

	/**
	 * Restores the checked state of items based on the cached check state.  This
	 * will only check leaf nodes, but parent items will be updated by the container
	 * viewer.  No events will be fired.
	 */
	public void restoreLeafCheckState() {
		if (checkState == null)
			return;

		getTree().setRedraw(false);
		// Call the super class so we don't mess up the cache
		super.setCheckedElements(NO_ELEMENTS);
		setGrayedElements(NO_ELEMENTS);
		// The elements must be expanded to modify their check state
		if (!checkState.isEmpty()) {
			expandAll();
		}
		// Now we are only going to set the check state of the leaf nodes
		// and rely on our container checked code to update the parents properly.
		super.setCheckedElements(checkState.toArray());
		getTree().setRedraw(true);
	}

	/**
	 * Returns the contents of the cached check state.  The contents will be all
	 * checked leaf nodes ignoring any filters.
	 *
	 * @return checked leaf elements
	 */
	public Object[] getCheckedLeafElements() {
		if (checkState == null) {
			return NO_ELEMENTS;
		}
		return checkState.toArray(new Object[checkState.size()]);
	}

	/**
	 * Returns the number of leaf nodes checked.  This method uses its internal check
	 * state cache to determine what has been checked, not what is visible in the viewer.
	 * The cache does not count duplicate items in the tree.
	 *
	 * @return number of leaf nodes checked according to the cached check state
	 */
	public int getCheckedLeafCount() {
		if (checkState == null) {
			return 0;
		}
		return checkState.size();
	}

	/**
	 * Returns whether {@code element} is a checked leaf node. This method uses
	 * its internal check state cache to determine what has been checked, not
	 * what is visible in the viewer.
	 */
	public boolean isCheckedLeafElement(Object element) {
		if (checkState == null) {
			return false;
		}

		return checkState.contains(element);
	}

	@Override
	public boolean setChecked(Object element, boolean state) {
		updateCheckState(element, state);
		return super.setChecked(element, state);
	}

	@Override
	public void setCheckedElements(Object[] elements) {
		super.setCheckedElements(elements);
		if (checkState == null) {
			checkState = new HashSet<>();
		} else {
			checkState.clear();
		}
		ITreeContentProvider contentProvider = null;
		if (getContentProvider() instanceof ITreeContentProvider) {
			contentProvider = (ITreeContentProvider) getContentProvider();
		}

		for (int i = 0; i < elements.length; i++) {
			Object[] children = contentProvider != null ? contentProvider.getChildren(elements[i]) : null;
			if (!getGrayed(elements[i]) && (children == null || children.length == 0)) {
				if (!checkState.contains(elements[i])) {
					checkState.add(elements[i]);
				}
			}
		}
	}

	@Override
	public void setAllChecked(boolean state) {
		super.setAllChecked(state);
		if (state) {

			// Find all visible children, add only the visible leaf nodes to the check state cache
			Object[] visible = getFilteredChildren(getRoot());
			if (checkState == null) {
				checkState = new HashSet<>();
			}

			ITreeContentProvider contentProvider = null;
			if (getContentProvider() instanceof ITreeContentProvider) {
				contentProvider = (ITreeContentProvider) getContentProvider();
			}

			if (contentProvider == null) {
				for (Object visibleObject : visible) {
					checkState.add(visibleObject);
				}
			} else {
				Set<Object> toCheck = new HashSet<>();
				for (Object visibleObject : visible) {
					addFilteredChildren(visibleObject, contentProvider, toCheck);
				}
				checkState.addAll(toCheck);
			}
		} else {
			// Remove any item in the check state that is visible (passes the filters)
			if (checkState != null) {
				Object[] visible = filter(checkState.toArray());
				for (Object visibleObject : visible) {
					checkState.remove(visibleObject);
				}
			}
		}
	}

	/**
	 * If the element is a leaf node, it is added to the result collection.  If the element has
	 * children, this method will recursively look at the children and add any visible leaf nodes
	 * to the collection.
	 *
	 * @param element element to check
	 * @param contentProvider tree content provider to check for children
	 * @param result collection to collect leaf nodes in
	 */
	private void addFilteredChildren(Object element, ITreeContentProvider contentProvider, Collection<Object> result) {
		if (!contentProvider.hasChildren(element)) {
			result.add(element);
		} else {
			Object[] visibleChildren = getFilteredChildren(element);
			for (Object visibleChild : visibleChildren) {
				addFilteredChildren(visibleChild, contentProvider, result);
			}
		}
	}

	@Override
	public void remove(Object[] elementsOrTreePaths) {
		for (Object elementOrTreePath : elementsOrTreePaths) {
			updateCheckState(elementOrTreePath, false);
		}
		super.remove(elementsOrTreePaths);
	}

	@Override
	public void remove(Object elementsOrTreePaths) {
		updateCheckState(elementsOrTreePaths, false);
		super.remove(elementsOrTreePaths);
	}

}
