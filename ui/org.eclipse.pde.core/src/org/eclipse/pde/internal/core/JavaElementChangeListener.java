/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;


public class JavaElementChangeListener implements IElementChangedListener {
	
	private static final String FILENAME = "clean-cache.properties"; //$NON-NLS-1$
	
	private Properties fTable = new Properties();

	public JavaElementChangeListener() {
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
		load();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		handleDelta(event.getDelta());
	}
	
	public void shutdown() {
		JavaCore.removeElementChangedListener(this);
		save();
	}
	
	private void handleDelta(IJavaElementDelta delta) {
		IJavaElement element = delta.getElement();
		
		if (element instanceof IJavaModel) {
			handleChildDeltas(delta);
		} else if (element instanceof IJavaProject) {
			if (isInterestingProject((IJavaProject)element)) {
				if (delta.getKind() == IJavaElementDelta.CHANGED) {
					handleChildDeltas(delta);
				} else if (delta.getKind() == IJavaElementDelta.ADDED) {
					updateTable(element);
				}
			}
		} else if (element instanceof IPackageFragmentRoot) {
			handleChildDeltas(delta);
		}
	}
	
	private void handleChildDeltas(IJavaElementDelta delta) {
		IJavaElementDelta[] deltas = delta.getAffectedChildren();
		for (int i = 0; i < deltas.length; i++) {
			if (ignoreDelta(deltas[i]))
				continue;
			if (isInterestingDelta(deltas[i])) {
				updateTable(deltas[i].getElement());
				break;
			}
			handleDelta(deltas[i]);
		}
	}
	
	private boolean isInterestingDelta(IJavaElementDelta delta) {
		int kind = delta.getKind();
		boolean interestingKind = kind == IJavaElementDelta.ADDED
				|| kind == IJavaElementDelta.REMOVED;
		
		IJavaElement element = delta.getElement();
		boolean interestingElement = element instanceof IPackageFragment
				|| element instanceof IPackageFragmentRoot;

		if (interestingElement && interestingKind)
			return true;
		
		if (kind == IJavaElementDelta.CHANGED && element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot)element;
			return root.isArchive();
		}
		return false;
	}
	
	private boolean ignoreDelta(IJavaElementDelta delta) {
		try {
			IJavaElement element = delta.getElement();
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root = (IPackageFragmentRoot)element;
				IClasspathEntry entry = root.getRawClasspathEntry();
				if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}
	
	private boolean isInterestingProject(IJavaProject jProject) {
		IProject project = jProject.getProject();
		return WorkspaceModelManager.isNonBinaryPluginProject(project)
				&& !WorkspaceModelManager.hasBundleManifest(project);
	}
	
	private void updateTable(IJavaElement element) {		
		IJavaProject jProject = (IJavaProject)element.getAncestor(IJavaElement.JAVA_PROJECT);
		if (jProject != null) {
			IProject project = jProject.getProject();
			ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(project);
			if (entry != null) {
				IPluginModelBase model = entry.getActiveModel();
				String id = model.getPluginBase().getId();
				if (id == null)
					return;
				fTable.put(id, Long.toString(System.currentTimeMillis()));
			}
		}
	}
	
	private void save() {
		// start by cleaning up extraneous keys.
		PluginModelManager mgr = PDECore.getDefault().getModelManager();
		Enumeration keys = fTable.keys();
		while(keys.hasMoreElements()) {
			String id = keys.nextElement().toString();
			IPluginModelBase model = mgr.findModel(id);
			if (model == null || model.getUnderlyingResource() == null)
				fTable.remove(id);
		}
		
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(new File(getDirectory(), FILENAME));
			fTable.store(stream, "Cached timestamps"); //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}			
		}
	}
	
	private File getDirectory() {
		IPath path = PDECore.getDefault().getStateLocation().append(".cache"); //$NON-NLS-1$
		File directory = new File(path.toOSString());
		if (!directory.exists() || !directory.isDirectory())
			directory.mkdirs();
		return directory;
	}
	
	private void load() {
		FileInputStream is = null;
		try {
			File file = new File(getDirectory(), FILENAME);
			if (file.exists() && file.isFile()) {
				is = new FileInputStream(file);
				fTable.load(is);
			}
		} catch (IOException e) {
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e1) {
			}
		}
	}
	
	public void synchronizeManifests(File cacheDirectory) {
		Enumeration keys = fTable.keys();
		while (keys.hasMoreElements()) {
			String id = keys.nextElement().toString();
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(id);
			if (model != null) {
				File file = new File(cacheDirectory, id + "_" + model.getPluginBase().getVersion() + ".MF"); //$NON-NLS-1$ //$NON-NLS-2$
				if (file.exists() && file.isFile() && file.lastModified() < Long.parseLong(fTable.get(id).toString()))
					file.delete();
			}
		}
	}

}
