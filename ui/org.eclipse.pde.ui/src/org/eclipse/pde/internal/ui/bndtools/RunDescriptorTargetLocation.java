/*******************************************************************************
 * Copyright (c) 2017, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Elias N Vasylenko <eliasvasylenko@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aqute.biz> - ongoing enhancements
 *     Juergen Albert <j.albert@data-in-motion.biz> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.build.Container;
import aQute.bnd.build.Workspace;
import biz.aQute.resolve.Bndrun;

public class RunDescriptorTargetLocation extends BndTargetLocation {
	static final String	TYPE							= "BndRunDescriptorLocation"; //$NON-NLS-1$

	static final String	ELEMENT_RUN_DESCRIPTOR			= "bndrun"; //$NON-NLS-1$
	static final String	ATTRIBUTE_RUN_DESCRIPTOR_FILE	= "file"; //$NON-NLS-1$

	private String		bndrunFileName;
	private IFile		bndrunFile;

	public RunDescriptorTargetLocation() {
		super(TYPE, "bndrun.gif"); //$NON-NLS-1$
	}

	public RunDescriptorTargetLocation setRunDescriptor(IFile bndrunFile) {
		this.bndrunFileName = bndrunFile.getFullPath()
			.toString();
		this.bndrunFile = bndrunFile;
		clearResolutionStatus();
		return this;

	}

	public RunDescriptorTargetLocation setRunDescriptor(String bndrunFileName) {
		this.bndrunFileName = bndrunFileName;
		this.bndrunFile = null;
		clearResolutionStatus();
		return this;
	}

	public IFile getFile() {
		return bndrunFile;
	}

	@Override
	public String getText(Object element) {
		return bndrunFile != null ? bndrunFile.getFullPath()
			.toString() : bndrunFileName;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
		RunDescriptorTargetLocationWizard wizard = new RunDescriptorTargetLocationWizard();
		wizard.setTarget(target);
		wizard.setTargetLocation(this);
		return wizard;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
		throws CoreException {
		resolveBndrunFile();

		Workspace workspace;
		workspace = BndTargetLocation.getWorkspace();

		try (Bndrun bndRun = new Bndrun(workspace, bndrunFile.getRawLocation()
			.makeAbsolute()
			.toFile())) {
			Collection<Container> containers = bndRun.getRunbundles();
			List<TargetBundle> bundles = new ArrayList<>(containers.size());

			monitor.beginTask("Resolving Bundles", containers.size()); //$NON-NLS-1$

			int i = 0;
			for (Container container : containers) {
				try {
					bundles.add(new TargetBundle(container.getFile()));
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "Invalid plugin in run descriptor: " //$NON-NLS-1$
						+ container.getBundleSymbolicName() + " @ " + getLocation(false), e)); //$NON-NLS-1$
				}

				if (monitor.isCanceled()) {
					return null;
				}
				monitor.worked(++i);
			}

			monitor.done();

			return bundles.toArray(new TargetBundle[0]);
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, MESSAGE_UNABLE_TO_RESOLVE_BUNDLES, e));
		}
	}

	private void resolveBndrunFile() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath path = new Path(bndrunFileName);
		IResource resource = workspace.getRoot()
			.findMember(path);

		if (resource instanceof IFile) {
			bndrunFile = (IFile) resource;
		} else {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "No file at path: " + bndrunFileName)); //$NON-NLS-1$
		}
	}

	@Override
	public String getLocation(boolean resolve) {
		return bndrunFileName;
	}

	@Override
	protected void serialize(Document document, Element locationElement) {
		Element runDescriptorElement = document.createElement(ELEMENT_RUN_DESCRIPTOR);
		runDescriptorElement.setAttribute(ATTRIBUTE_RUN_DESCRIPTOR_FILE, getLocation(true));
		locationElement.appendChild(runDescriptorElement);
	}

	public static class Factory extends BndTargetLocationFactory {
		public Factory() {
			super(TYPE);
		}

		@Override
		public ITargetLocation getTargetLocation(Element locationElement) throws CoreException {
			NodeList children = locationElement.getChildNodes();

			for (int i = 0; i < children.getLength(); ++i) {
				Node node = children.item(i);

				if (isElement(node, ELEMENT_RUN_DESCRIPTOR)) {
					String file = ((Element) node).getAttribute(ATTRIBUTE_RUN_DESCRIPTOR_FILE);

					return new RunDescriptorTargetLocation().setRunDescriptor(file);
				}
			}

			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "No run descriptor file specified")); //$NON-NLS-1$
		}
	}

	@Override
	public void dispose() {}
}
