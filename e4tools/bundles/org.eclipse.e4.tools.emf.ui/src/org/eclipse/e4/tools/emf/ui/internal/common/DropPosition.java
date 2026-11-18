/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common;

import java.util.List;

import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.fragment.impl.FragmentPackageImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.jface.viewers.ViewerDropAdapter;

/**
 * Where a drag and drop in the model editor tree goes: the drop zone under the
 * pointer, the list index that zone stands for and the element that lands there.
 */
public final class DropPosition {

	private DropPosition() {
	}

	/**
	 * Returns the {@link ViewerDropAdapter} location for a pointer {@code offset}
	 * pixels below the top of a row that accepts the element as a sibling.
	 *
	 * @param acceptsChild         whether the row's element also accepts the
	 *                             dragged element as a child
	 * @param expandedWithChildren whether the row shows its children below it, so
	 *                             that a line under it would mean "first child"
	 *                             rather than "after the whole subtree"
	 */
	public static int location(int offset, int height, boolean acceptsChild, boolean expandedWithChildren) {
		if (!acceptsChild) {
			return expandedWithChildren || offset < height / 2 ? ViewerDropAdapter.LOCATION_BEFORE
					: ViewerDropAdapter.LOCATION_AFTER;
		}
		if (offset < height / 3) {
			return ViewerDropAdapter.LOCATION_BEFORE;
		}
		if (expandedWithChildren || offset < height * 2 / 3) {
			return ViewerDropAdapter.LOCATION_ON;
		}
		return ViewerDropAdapter.LOCATION_AFTER;
	}

	/**
	 * Returns the index the dragged element ends up at when dropped before or after
	 * the element at {@code target}, or {@code target} itself when it is negative.
	 *
	 * @param current the dragged element's index in the same list, or -1 when it
	 *                comes from another list
	 */
	public static int index(int target, boolean after, int current) {
		if (target < 0) {
			return target;
		}
		final int index = after ? target + 1 : target;
		return current >= 0 && current < index ? index - 1 : index;
	}

	/**
	 * Returns the element to add to {@code feature} of {@code parent} when
	 * {@code dragged} is dropped there: an element id placeholder for the imports
	 * of a fragment, a copy for an element of another model (such as one dragged
	 * from a second editor) without references into that model, a copy if
	 * {@code copy} is set, otherwise {@code dragged} itself, which is then moved.
	 */
	public static EObject element(EObject dragged, EObject parent, EStructuralFeature feature, boolean copy) {
		if (feature == FragmentPackageImpl.Literals.MODEL_FRAGMENTS__IMPORTS
				&& !((List<?>) parent.eGet(feature)).contains(dragged)
				&& dragged instanceof MApplicationElement source) {
			final MApplicationElement placeholder = (MApplicationElement) EcoreUtil.create(dragged.eClass());
			placeholder.setElementId(source.getElementId());
			return (EObject) placeholder;
		}
		if (EcoreUtil.getRootContainer(dragged) != EcoreUtil.getRootContainer(parent)) {
			final Copier copier = new Copier(true, false);
			final EObject result = copier.copy(dragged);
			copier.copyReferences();
			return result;
		}
		return copy ? EcoreUtil.copy(dragged) : dragged;
	}
}
