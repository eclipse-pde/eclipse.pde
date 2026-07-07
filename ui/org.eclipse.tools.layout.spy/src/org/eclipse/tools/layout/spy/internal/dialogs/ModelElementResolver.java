/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.tools.layout.spy.internal.dialogs;

import java.lang.reflect.Method;

import org.eclipse.e4.ui.model.application.MContribution;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Resolves an SWT {@link Control} to the owning e4 application-model element and
 * describes its implementing Java class.
 * <p>
 * The e4 SWT renderer tags each widget with its owning {@link MUIElement} under
 * the data key {@code "modelElement"} ({@code AbstractPartRenderer.OWNING_ME}).
 * That constant is in an {@code x-friends} package, so the literal is used here.
 */
public final class ModelElementResolver {

	/** Widget data key set by the e4 SWT renderer, == AbstractPartRenderer.OWNING_ME. */
	private static final String OWNING_MODEL_ELEMENT_KEY = "modelElement"; //$NON-NLS-1$

	/** Wrapper for 3.x parts; unwrapped to reveal the real view/editor class. */
	private static final String COMPATIBILITY_PART_CLASS = "org.eclipse.ui.internal.e4.compatibility.CompatibilityPart"; //$NON-NLS-1$

	private static final String MODEL_PACKAGE_PREFIX = "org.eclipse.e4.ui.model."; //$NON-NLS-1$

	private ModelElementResolver() {
	}

	/**
	 * Returns the closest model element owning the control, walking up its
	 * parents, or {@code null} if none (for example a plain JFace dialog).
	 */
	public static @Nullable MUIElement findModelElement(Control control) {
		for (Control current = control; current != null; current = current.getParent()) {
			if (current.getData(OWNING_MODEL_ELEMENT_KEY) instanceof MUIElement element) {
				return element;
			}
		}
		return null;
	}

	/** Builds a copyable description of the model element and implementing class. */
	public static String describe(Control control) {
		StringBuilder builder = new StringBuilder();
		builder.append("Control class: ").append(control.getClass().getName()).append('\n'); //$NON-NLS-1$

		MUIElement element = findModelElement(control);
		if (element == null) {
			describeWithoutModel(control, builder);
			return builder.toString();
		}

		builder.append('\n');
		builder.append("Model element: ").append(modelTypeName(element)); //$NON-NLS-1$
		String id = element.getElementId();
		if (id != null && !id.isEmpty()) {
			builder.append(" (id=").append(id).append(')'); //$NON-NLS-1$
		}
		builder.append('\n');

		if (element instanceof MContribution contribution) {
			describeContribution(contribution, builder);
		}
		return builder.toString();
	}

	private static void describeWithoutModel(Control control, StringBuilder builder) {
		builder.append('\n');
		builder.append("No application model element is associated with this control.").append('\n'); //$NON-NLS-1$
		Shell shell = control.getShell();
		builder.append("Shell class: ").append(shell.getClass().getName()).append('\n'); //$NON-NLS-1$
		Object shellData = shell.getData();
		if (shellData != null) {
			builder.append("Shell data class: ").append(shellData.getClass().getName()).append('\n'); //$NON-NLS-1$
		}
	}

	private static void describeContribution(MContribution contribution, StringBuilder builder) {
		String uri = contribution.getContributionURI();
		if (uri != null && !uri.isEmpty()) {
			builder.append("Contribution URI: ").append(uri).append('\n'); //$NON-NLS-1$
		}
		Object object = contribution.getObject();
		if (object == null) {
			return;
		}
		Object implementation = unwrapCompatibilityPart(object);
		Class<?> implementationClass = implementation.getClass();
		builder.append("Implementing class: ").append(implementationClass.getName()).append('\n'); //$NON-NLS-1$
		if (implementation != object) {
			builder.append("Wrapped by: ").append(object.getClass().getName()).append('\n'); //$NON-NLS-1$
		}
		Bundle bundle = FrameworkUtil.getBundle(implementationClass);
		if (bundle != null) {
			builder.append("Contributing bundle: ").append(bundle.getSymbolicName()).append('\n'); //$NON-NLS-1$
		}
	}

	/** Most specific model interface name (e.g. {@code MPart}) of the element. */
	private static String modelTypeName(MUIElement element) {
		for (Class<?> iface : element.getClass().getInterfaces()) {
			if (iface.getName().startsWith(MODEL_PACKAGE_PREFIX)) {
				return iface.getSimpleName();
			}
		}
		return element.getClass().getSimpleName();
	}

	/** Unwraps a 3.x compatibility part to its real {@code IWorkbenchPart}; else returns the object. */
	private static Object unwrapCompatibilityPart(Object object) {
		for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
			if (COMPATIBILITY_PART_CLASS.equals(type.getName())) {
				try {
					Method getPart = object.getClass().getMethod("getPart"); //$NON-NLS-1$
					Object part = getPart.invoke(object);
					if (part != null) {
						return part;
					}
				} catch (ReflectiveOperationException e) {
					// Best effort only; fall back to the wrapper class.
				}
				break;
			}
		}
		return object;
	}
}
