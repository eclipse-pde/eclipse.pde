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
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.ui.progress.UIJob;

/**
 * Operation for creating a new api problem filter
 * 
 * @see IApiProblemFilter
 * @see IApiFilterStore
 * 
 * @since 1.0.0
 */
public class CreateApiFilterOperation extends UIJob {

	private IJavaElement fBackingElement = null;
	private IMarker fBackingMarker = null;
	private String fKind = null;
	
	/**
	 * Constructor
	 * @param element the element to create the filter for (method, field, class, enum, etc)
	 * @param kind the kind of filter to create
	 * 
	 * @see IApiProblemFilter#getKinds()
	 */
	public CreateApiFilterOperation(IMarker marker, IJavaElement element, String kind) {
		super(MarkerMessages.CreateApiFilterOperation_0);
		fBackingElement = element;
		fBackingMarker = marker;
		fKind = kind;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInUIThread(IProgressMonitor monitor) {
		if(fBackingElement != null) {
			IJavaProject project = fBackingElement.getJavaProject();
			IApiComponent component = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile().getApiComponent(project.getElementName());
			if(component == null) {
				return Status.CANCEL_STATUS;
			}
			try {
				IApiFilterStore store = component.getFilterStore();
				IElementDescriptor element = null;
				boolean childmarkers = false;
				switch(fBackingElement.getElementType()) {
					case IJavaElement.TYPE: {
						IType type = (IType) fBackingElement;
						element = Factory.typeDescriptor(type.getFullyQualifiedName());
						break;
					}
					case IJavaElement.METHOD: {
						IMethod method = (IMethod) fBackingElement;
						element = Factory.methodDescriptor(method.getDeclaringType().getFullyQualifiedName(), method.getElementName(), method.getSignature());
						break;
					}
					case IJavaElement.FIELD: {
						IField field = (IField) fBackingElement;
						element = Factory.fieldDescriptor(field.getDeclaringType().getFullyQualifiedName(), field.getElementName());
						break;
					}
					case IJavaElement.PACKAGE_FRAGMENT: {
						IPackageFragment fragment = (IPackageFragment) fBackingElement;
						element = Factory.packageDescriptor(fragment.getElementName());
						childmarkers = true;
						break;
					}
				}
				if(element != null) {
					store.addFilter(component.newProblemFilter(element, new String[] {fKind}));
					cleanupMarkers(childmarkers);
					return Status.OK_STATUS;
				}
			}
			catch(CoreException ce) {
				ApiUIPlugin.log(ce);
			}
		}
		return Status.CANCEL_STATUS;
	}
	
	/**
	 * Cleans up all of the marker this operation is acting on, and optionally 
	 * removes similar markers from the child resources
	 * @param childmarkers if child markers should also be cleaned 
	 * @throws CoreException
	 */
	private void cleanupMarkers(boolean childmarkers) throws CoreException {
		fBackingMarker.delete();
		if(childmarkers) {
			IResource res = fBackingMarker.getResource();
			int backingkind = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, -1);
			if(backingkind == -1) {
				//nothing we can do if there are no kinds to compare: we don not ever want to just remove all 
				//markers of the same marker id
				return;
			}
			int backingflag = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FLAGS, -1);
			IMarker[] children = res.findMarkers(fBackingMarker.getType(), true, IResource.DEPTH_INFINITE);
			IMarker marker = null;
			for(int i = 0; i < children.length; i++) {
				marker = children[i];
				if (backingkind == marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, -1) &&
						backingflag == marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FLAGS, -1)) {
					marker.delete();
				}
			}
		}
	}
}
