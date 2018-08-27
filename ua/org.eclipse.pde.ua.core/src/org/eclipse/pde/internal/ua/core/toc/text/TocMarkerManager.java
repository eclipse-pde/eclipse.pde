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

package org.eclipse.pde.internal.ua.core.toc.text;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.SAXParseException;

public class TocMarkerManager {

	public static void refreshMarkers(TocModel model) {
		deleteMarkers(model);
		createMarkers(model);
	}

	public static void deleteMarkers(TocModel model) {
		try {
			 if (model.getUnderlyingResource() == null) {
				 return;
			 }
			IMarker[] problems = model.getUnderlyingResource().findMarkers(
					IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			if (problems != null) {
				for (IMarker problem : problems) {
					problem.delete();
				}
			}
		} catch (CoreException e) {
			PDECore.log(e);
		}

	}

	public static void createMarkers(TocModel model) {
		Collection<Exception> errors = model.getErrors();
		if (errors == null || errors.isEmpty()) {
			return;
		}

		Iterator<Exception> iter = errors.iterator();
		while (iter.hasNext()) {
			Throwable exception = iter.next();
			if (exception instanceof SAXParseException) {
				int line = ((SAXParseException) exception).getLineNumber();
				try {

					IMarker marker = model.getUnderlyingResource()
							.createMarker(IMarker.PROBLEM);

					marker.setAttribute(IMarker.LINE_NUMBER, line);
					marker.setAttribute(IMarker.SEVERITY,
							IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.MESSAGE, exception
							.getLocalizedMessage());
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
		}
	}
}
