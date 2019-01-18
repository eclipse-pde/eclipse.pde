/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.project.PDEProject;

public class JavaElementChangeListener implements IElementChangedListener {

	private static final String FILENAME = "clean-cache.properties"; //$NON-NLS-1$

	private final Properties fTable = new Properties();

	public void start() {
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
		load();
	}

	@Override
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
			if (isInterestingProject((IJavaProject) element)) {
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
		for (IJavaElementDelta childDelta : deltas) {
			if (ignoreDelta(childDelta)) {
				continue;
			}
			if (isInterestingDelta(childDelta)) {
				updateTable(childDelta.getElement());
				break;
			}
			handleDelta(childDelta);
		}
	}

	private boolean isInterestingDelta(IJavaElementDelta delta) {
		int kind = delta.getKind();
		boolean interestingKind = kind == IJavaElementDelta.ADDED || kind == IJavaElementDelta.REMOVED;

		IJavaElement element = delta.getElement();
		boolean interestingElement = element instanceof IPackageFragment || element instanceof IPackageFragmentRoot;

		if (interestingElement && interestingKind) {
			return true;
		}

		if (kind == IJavaElementDelta.CHANGED && element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root = (IPackageFragmentRoot) element;
			return root.isArchive();
		}
		return false;
	}

	private boolean ignoreDelta(IJavaElementDelta delta) {
		try {
			IJavaElement element = delta.getElement();
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root = (IPackageFragmentRoot) element;
				IClasspathEntry entry = root.getRawClasspathEntry();
				if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					return true;
				}
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	private boolean isInterestingProject(IJavaProject jProject) {
		IProject project = jProject.getProject();
		return WorkspaceModelManager.isPluginProject(project) && !WorkspaceModelManager.isBinaryProject(project) && !PDEProject.getManifest(project).exists();
	}

	private void updateTable(IJavaElement element) {
		IJavaProject jProject = (IJavaProject) element.getAncestor(IJavaElement.JAVA_PROJECT);
		if (jProject != null) {
			IProject project = jProject.getProject();
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model != null) {
				String id = model.getPluginBase().getId();
				if (id != null) {
					fTable.put(id, Long.toString(System.currentTimeMillis()));
				}
			}
		}
	}

	private void save() {
		// start by cleaning up extraneous keys.
		Enumeration<Object> keys = fTable.keys();
		while (keys.hasMoreElements()) {
			String id = keys.nextElement().toString();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model == null || model.getUnderlyingResource() == null) {
				fTable.remove(id);
			}
		}

		try (FileOutputStream stream = new FileOutputStream(new File(getDirectory(), FILENAME))) {
			fTable.store(stream, "Cached timestamps"); //$NON-NLS-1$
			stream.flush();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	private File getDirectory() {
		IPath path = PDECore.getDefault().getStateLocation().append(".cache"); //$NON-NLS-1$
		File directory = new File(path.toOSString());
		if (!directory.exists() || !directory.isDirectory()) {
			directory.mkdirs();
		}
		return directory;
	}

	private void load() {
		File file = new File(getDirectory(), FILENAME);
		if (file.exists() && file.isFile()) {
			try (FileInputStream is = new FileInputStream(file)) {
				fTable.load(is);
			} catch (IOException e) {
			}
		}
	}

	public void synchronizeManifests(File cacheDirectory) {
		Enumeration<Object> keys = fTable.keys();
		while (keys.hasMoreElements()) {
			String id = keys.nextElement().toString();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				File file = new File(cacheDirectory, id + "_" + model.getPluginBase().getVersion() + ".MF"); //$NON-NLS-1$ //$NON-NLS-2$
				if (file.exists() && file.isFile() && file.lastModified() < Long.parseLong(fTable.get(id).toString())) {
					file.delete();
				}
			}
		}
	}

}
