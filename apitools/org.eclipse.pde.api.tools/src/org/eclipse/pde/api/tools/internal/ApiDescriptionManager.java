/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;

/**
 * Manages a cache of API descriptions for Java projects. Descriptions
 * are re-used between API components for the same project.
 * 
 * @since 1.0
 * 
 * TODO: persist/restore descriptions to improve performance
 */
public class ApiDescriptionManager implements IElementChangedListener {
	
	/**
	 * Singleton
	 */
	private static ApiDescriptionManager fgDefault;
	
	/**
	 * Maps Java projects to API descriptions
	 */
	private Map fDescriptions = new HashMap();

	/**
	 * Constructs an API description manager.
	 */
	private ApiDescriptionManager() {
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
	}

	/**
	 * Cleans up Java element listener
	 */
	public static void shutdown() {
		if (fgDefault != null) {
			JavaCore.removeElementChangedListener(fgDefault);
		}
	}
	
	/**
	 * Returns the singleton API description manager.
	 * 
	 * @return API description manager
	 */
	public synchronized static ApiDescriptionManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new ApiDescriptionManager();
		}
		return fgDefault;
	}
	
	/**
	 * Returns an API description for the given Java project.
	 * 
	 * @param project Java project
	 * @return API description
	 */
	public synchronized IApiDescription getApiDescription(IJavaProject project) {
		IApiDescription description = (IApiDescription) fDescriptions.get(project);
		if (description == null) {
			description = new ProjectApiDescription(project);
			fDescriptions.put(project, description);
		}
		return description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta delta = event.getDelta();
		processJavaElementDeltas(delta.getAffectedChildren());
	}
	
	/**
	 * Remove projects that get closed or removed.
	 * 
	 * @param deltas
	 */
	private synchronized void processJavaElementDeltas(IJavaElementDelta[] deltas) {
		IJavaElementDelta delta = null;
		for(int i = 0; i < deltas.length; i++) {
			delta = deltas[i];
			switch(delta.getElement().getElementType()) {
				case IJavaElement.JAVA_PROJECT: {
					IJavaProject proj = (IJavaProject) delta.getElement();
					switch (delta.getKind()) {
						case IJavaElementDelta.CHANGED:
							int flags = delta.getFlags();
							if((flags & IJavaElementDelta.F_CLOSED) != 0) {
								fDescriptions.remove(proj);
							}
							break;
						case IJavaElementDelta.REMOVED:
							fDescriptions.remove(proj);
							break;
					}
					break;
				}
			}
		}
	}	

}
