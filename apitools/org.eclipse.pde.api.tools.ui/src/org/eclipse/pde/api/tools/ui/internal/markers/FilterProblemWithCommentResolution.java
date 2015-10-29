/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

import com.ibm.icu.text.MessageFormat;

/**
 * Problem resolution that creates an {@link IApiProblemFilter} with a comment
 *
 * @since 1.1
 */
public class FilterProblemWithCommentResolution extends FilterProblemResolution {

	/**
	 * Constructor
	 *
	 * @param marker
	 */
	public FilterProblemWithCommentResolution(IMarker marker) {
		super(marker);
	}

	@Override
	public String getLabel() {
		if (plural) {
			return MarkerMessages.FilterProblemWithCommentResolution_create_commented_filters;
		} else {
			IJavaElement element = resolveElementFromMarker();
			if (element != null) {
				return MessageFormat.format(MarkerMessages.FilterProblemWithCommentResolution_create_commented_filter, new Object[] {
						JavaElementLabels.getTextLabel(element, JavaElementLabels.M_PARAMETER_TYPES),
						resolveCategoryName() });
			} else {
				IResource res = fBackingMarker.getResource();
				return MessageFormat.format(MarkerMessages.FilterProblemWithCommentResolution_create_commented_filter, new Object[] {
						res.getFullPath().removeFileExtension().lastSegment(),
						resolveCategoryName() });
			}
		}
	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor) {
		CreateApiFilterOperation op = new CreateApiFilterOperation(markers, true);
		op.setSystem(true);
		op.schedule();
	}
}
