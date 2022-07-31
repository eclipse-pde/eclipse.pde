/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.core.ctxhelp.text;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.SAXParseException;

/**
 * Manages markers for xml problems in the context help editor
 */
public class CtxHelpMarkerManager {

	public static void refreshMarkers(CtxHelpModel model) {
		deleteMarkers(model);
		createMarkers(model);
	}

	public static void deleteMarkers(CtxHelpModel model) {
		try {
			IMarker[] problems = model.getUnderlyingResource().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			if (problems != null) {
				for (IMarker problem : problems) {
					problem.delete();
				}
			}
		} catch (CoreException e) {
		}

	}

	public static void createMarkers(CtxHelpModel model) {
		Collection<Exception> errors = model.getErrors();
		if (errors == null || errors.isEmpty()) {
			return;
		}

		for (Throwable exception : errors) {
			if (exception instanceof SAXParseException) {
				int line = ((SAXParseException) exception).getLineNumber();
				try {
					Map<String, Object> attributes = Map.of(IMarker.LINE_NUMBER, line,//
							IMarker.SEVERITY, IMarker.SEVERITY_ERROR,//
							IMarker.MESSAGE, exception.getLocalizedMessage());
					model.getUnderlyingResource().createMarker(IMarker.PROBLEM, attributes);
				} catch (CoreException e) {
				}
			}
		}
	}
}
