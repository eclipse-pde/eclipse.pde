/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;

/**
 * A marker annotation model whose underlying source of markers is 
 * a resource in the workspace.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class SystemFileMarkerAnnotationModel
	extends AbstractMarkerAnnotationModel {

	/**
	 * Internal resource change listener.
	 */
	class ResourceChangeListener implements IResourceChangeListener {
		/*
		 * @see IResourceChangeListener#resourceChanged
		 */
		public void resourceChanged(IResourceChangeEvent e) {
			IResourceDelta delta = e.getDelta();
			try {
				if (delta != null)
					delta.accept(fResourceDeltaVisitor);
			} catch (CoreException x) {
				PDEPlugin.logException(x);
			}
		}
	}

	/**
	 * Internal resource delta visitor.
	 */
	class ResourceDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * @see IResourceDeltaVisitor#visit
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta != null && fResource.equals(delta.getResource())) {
				update(delta.getMarkerDeltas());
				return false;
			}
			return true;
		}
	}

	/** The workspace */
	private IWorkspace fWorkspace;
	/** The resource */
	private IResource fResource;
	/** The resource change listener */
	private IResourceChangeListener fResourceChangeListener =
		new ResourceChangeListener();
	/** The resource delta visitor */
	private IResourceDeltaVisitor fResourceDeltaVisitor =
		new ResourceDeltaVisitor();
	private File file;

	/**
	 * Creates a marker annotation model with the given resource as the source
	 * of the markers.
	 *
	 * @param resource the resource
	 */
	public SystemFileMarkerAnnotationModel(File file) {
		fWorkspace = PDEPlugin.getWorkspace();
		fResource = fWorkspace.getRoot();
		this.file = file;
	}

	/*
	 * @see AnnotationModel#isAcceptable
	 */
	protected boolean isAcceptable(IMarker marker) {
		if (marker == null)
			return false;
		if (!fResource.equals(marker.getResource()))
			return false;
		// check extra fields
		try {
			String path =
				(String) marker.getAttribute(
					IPDEUIConstants.MARKER_SYSTEM_FILE_PATH);
			if (path == null)
				return false;
			return new Path(path).equals(new Path(file.getPath()));
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * Updates this model to the given marker deltas.
	 *
	 * @param markerDeltas the list of marker deltas
	 */
	private void update(IMarkerDelta[] markerDeltas) {

		if (markerDeltas.length == 0)
			return;

		for (int i = 0; i < markerDeltas.length; i++) {
			IMarkerDelta delta = markerDeltas[i];
			switch (delta.getKind()) {
				case IResourceDelta.ADDED :
					addMarkerAnnotation(delta.getMarker());
					break;
				case IResourceDelta.REMOVED :
					removeMarkerAnnotation(delta.getMarker());
					break;
				case IResourceDelta.CHANGED :
					modifyMarkerAnnotation(delta.getMarker());
					break;
			}
		}

		fireModelChanged();
	}

	/*
	 * @see AbstractMarkerAnnotationModel#listenToMarkerChanges(boolean)
	 */
	protected void listenToMarkerChanges(boolean listen) {
		if (listen)
			fWorkspace.addResourceChangeListener(fResourceChangeListener);
		else
			fWorkspace.removeResourceChangeListener(fResourceChangeListener);
	}

	/*
	 * @see AbstractMarkerAnnotationModel#deleteMarkers(IMarker[])
	 */
	protected void deleteMarkers(final IMarker[] markers)
		throws CoreException {
		fWorkspace.run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				for (int i = 0; i < markers.length; ++i) {
					markers[i].delete();
				}
			}
		}, null);
	}

	/*
	 * @see AbstractMarkerAnnotationModel#retrieveMarkers()
	 */
	protected IMarker[] retrieveMarkers() throws CoreException {
		IMarker [] markers = fResource.findMarkers(
			IMarker.MARKER,
			true,
			IResource.DEPTH_ZERO);
		if (markers.length==0) return markers;
		ArrayList result = new ArrayList();
		for (int i=0; i<markers.length; i++) {
			IMarker marker = markers[i];
			String path = (String)marker.getAttribute(IPDEUIConstants.MARKER_SYSTEM_FILE_PATH);
			if (path!=null && new Path(path).equals(new Path(file.getPath())))
				result.add(marker);
		}
		return (IMarker[])result.toArray(new IMarker[result.size()]);
	}

	/**
	 * Returns the resource serving as the source of markers for this annotation model.
	 * 
	 * @return the resource serving as the source of markers for this annotation model
	 * @since 2.0
	 */
	protected IResource getResource() {
		return fResource;
	}
}
