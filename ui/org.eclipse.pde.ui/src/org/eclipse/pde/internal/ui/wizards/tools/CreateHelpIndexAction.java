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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.help.search.HelpIndexBuilder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Creates the help search index by parsing the selected
 * plugin.xml file and generating index for TOC extensions.
 *
 * @since 3.1
 */

public class CreateHelpIndexAction implements IObjectActionDelegate {
	private ISelection selection;

	private HelpIndexBuilder indexBuilder;

	public CreateHelpIndexAction() {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		File file = getManifest();
		if (file == null)
			return;
		if (indexBuilder == null)
			indexBuilder = new HelpIndexBuilder();
		indexBuilder.setManifest(file);
		File target = getTarget();
		if (target==null)
			return;
			indexBuilder.setTarget(target);
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					monitor.beginTask("Creating index...", 10);
					indexBuilder.execute(new SubProgressMonitor(monitor, 9));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				finally {
					try {
						refreshTarget(new SubProgressMonitor(monitor, 1));
					}
					catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		}
	}

	private File getManifest() {
		if (selection == null)
			return null;
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj instanceof IFile) {
			IFile fileResource = (IFile) obj;
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			ModelEntry entry = manager.findEntry(fileResource.getProject());
			if (entry!=null) {
				IPluginModelBase modelBase = entry.getActiveModel();
				return getManifest(manager, fileResource, modelBase);
			}
		}
		return null;
	}
	
	private File getManifest(PluginModelManager manager, IFile file, IPluginModelBase modelBase) {
		IPluginBase pluginBase = modelBase.getPluginBase();
		if (pluginBase instanceof IFragment) {
			// fragment
			IFragment fragment = (IFragment)pluginBase;
			String pluginId = fragment.getPluginId();
			String pluginVersion = fragment.getPluginVersion();
			int match = fragment.getRule();
			IPluginModelBase pluginModel = manager.findPlugin(pluginId, pluginVersion, match);
			if (pluginModel.getUnderlyingResource()==null) {
				return null;
			}
			IFile pluginFile = pluginModel.getUnderlyingResource().getProject().getFile("plugin.xml");
			return (pluginFile.exists()) ? pluginFile.getLocation().toFile() : null;
		}
		return file.getLocation().toFile();
	}

	private File getTarget() {
		if (selection == null)
			return null;
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj instanceof IFile) {
			IFile fileResource = (IFile) obj;
			return fileResource.getLocation().toFile().getParentFile();
		}
		return null;
	}
	
	private void refreshTarget(IProgressMonitor monitor) throws CoreException {
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		IFile fileResource = (IFile) obj;
		fileResource.getParent().refreshLocal(IResource.DEPTH_INFINITE, monitor);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}