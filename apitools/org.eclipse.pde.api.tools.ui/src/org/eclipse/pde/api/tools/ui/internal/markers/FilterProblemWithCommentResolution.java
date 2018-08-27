/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
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
				return MessageFormat.format(MarkerMessages.FilterProblemWithCommentResolution_create_commented_filter,
						JavaElementLabels.getTextLabel(element, JavaElementLabels.M_PARAMETER_TYPES),
						resolveCategoryName() );
			} else {
				IResource res = fBackingMarker.getResource();
				return MessageFormat.format(MarkerMessages.FilterProblemWithCommentResolution_create_commented_filter,
						res.getFullPath().removeFileExtension().lastSegment(),
						resolveCategoryName() );
			}
		}
	}

	@Override
	public String getDescription() {
		try {
			String value = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = new String[0];
			if (value != null) {
				args = value.split("#"); //$NON-NLS-1$
			}
			int id = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0);
			return MessageFormat.format(MarkerMessages.FilterProblemWithCommentResolution_create_commented_filter_desc,
					ApiProblemFactory.getLocalizedMessage(ApiProblemFactory.getProblemMessageId(id), args),
					resolveCategoryName());
		} catch (CoreException e) {
		}
		return null;
	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor) {
		CreateApiFilterOperation op = new CreateApiFilterOperation(markers, true);
		op.setSystem(true);
		op.schedule();
	}
}
