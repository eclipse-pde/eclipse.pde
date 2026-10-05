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

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.e4.ui.model.internal.ModelUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.CommandParameter;

/**
 * Where pasted model elements go: the nearest element at or above the selection
 * whose containment list accepts them, right after the selected branch.
 */
public record PasteTarget(EObject container, EStructuralFeature feature, int index) {

	/**
	 * Returns the paste target for {@code probe} starting at {@code selected}, or
	 * {@code null} if no element up to the root accepts it.
	 */
	public static PasteTarget find(EObject selected, EObject probe) {
		EObject child = null;
		for (EObject candidate = selected; candidate != null; child = candidate, candidate = candidate.eContainer()) {
			final EStructuralFeature feature = acceptingFeature(candidate, probe);
			if (feature != null) {
				return new PasteTarget(candidate, feature, indexAfter(candidate, feature, child));
			}
		}
		return null;
	}

	/**
	 * Returns whether {@code feature} of {@code container} can hold
	 * {@code element}.
	 */
	public static boolean accepts(EObject container, EStructuralFeature feature, EObject element) {
		final EClass eClass = container.eClass();
		return ModelUtils.getTypeArgument(eClass, feature.getEGenericType()).isInstance(element);
	}

	private static EStructuralFeature acceptingFeature(EObject container, EObject probe) {
		final EStructuralFeature children = UiPackageImpl.Literals.ELEMENT_CONTAINER__CHILDREN;
		if (container instanceof MElementContainer<?> && accepts(container, children, probe)) {
			return children;
		}
		for (final EReference reference : container.eClass().getEAllContainments()) {
			if (reference.isMany() && accepts(container, reference, probe)) {
				return reference;
			}
		}
		return null;
	}

	private static int indexAfter(EObject container, EStructuralFeature feature, EObject child) {
		if (child == null) {
			return CommandParameter.NO_INDEX;
		}
		final int index = ((List<?>) container.eGet(feature)).indexOf(child);
		return index < 0 ? CommandParameter.NO_INDEX : index + 1;
	}
}
