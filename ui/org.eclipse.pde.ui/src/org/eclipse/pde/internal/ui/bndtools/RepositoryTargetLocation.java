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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;

public class RepositoryTargetLocation extends BndTargetLocation {
	static final String			TYPE									= "BndRepositoryLocation"; //$NON-NLS-1$

	static final String			MESSAGE_UNABLE_TO_RESOLVE_REPOSITORIES	= "Unable to resolve Bnd repository plugins"; //$NON-NLS-1$

	static final String			ELEMENT_REPOSITORY						= "repository"; //$NON-NLS-1$
	static final String			ATTRIBUTE_REPOSITORY_NAME				= "name"; //$NON-NLS-1$

	private String				repositoryName;
	private RepositoryPlugin	repository;

	public RepositoryTargetLocation() {
		super(TYPE, "database.png"); //$NON-NLS-1$
	}

	public RepositoryTargetLocation setRepository(String repositoryName) {
		this.repositoryName = repositoryName;
		this.repository = null;
		clearResolutionStatus();
		return this;
	}

	public RepositoryTargetLocation setRepository(RepositoryPlugin repository) {
		this.repositoryName = repository.getName();
		this.repository = repository;
		clearResolutionStatus();
		return this;
	}

	public RepositoryPlugin getRepository() {
		return repository;
	}

	@Override
	public String getText(Object element) {
		return repositoryName;
	}


	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
		RepositoryTargetLocationWizard wizard = new RepositoryTargetLocationWizard();
		wizard.setTarget(target);
		wizard.setTargetLocation(this);
		return wizard;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
		throws CoreException {
		resolveRepository();

		try {
			List<TargetBundle> bundles = new ArrayList<>();

			List<String> bsns = repository.list("*"); //$NON-NLS-1$
			monitor.beginTask("Resolving Bundles", bsns.size()); //$NON-NLS-1$

			int i = 0;
			for (String bsn : bsns) {
				if (bsn.contains(":")) { //$NON-NLS-1$
					continue;
				}
				Version version = repository.versions(bsn)
					.last();

				File download = repository.get(bsn, version, new HashMap<>());
				try {
					bundles.add(new TargetBundle(download));
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.WARNING, PLUGIN_ID,
						"Invalid plugin in repository: " + bsn + " @ " + getLocation(false), e)); //$NON-NLS-1$ //$NON-NLS-2$
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

	private void resolveRepository() throws CoreException {
		Workspace workspace;
		try {
			workspace = BndTargetLocation.getWorkspace();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, MESSAGE_UNABLE_TO_LOCATE_WORKSPACE, e));
		}

		try {
			if (repositoryName.equals(workspace.getWorkspaceRepository()
				.getName())) {
				this.repository = workspace.getWorkspaceRepository();
			} else {
				for (RepositoryPlugin repository : workspace.getPlugins(RepositoryPlugin.class)) {
					if (repositoryName.equalsIgnoreCase(repository.getName())) {
						this.repository = repository;
					}
				}
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, MESSAGE_UNABLE_TO_RESOLVE_REPOSITORIES, e));
		}

		if (this.repository == null) {
			throw new CoreException(
				new Status(IStatus.ERROR, PLUGIN_ID, "Unable to locate the named repository: " + repositoryName)); //$NON-NLS-1$
		}
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			resolveRepository();
		}
		return repository != null ? repository.getLocation() : ""; //$NON-NLS-1$
	}

	@Override
	protected void serialize(Document document, Element locationElement) {
		Element repositoryElement = document.createElement(ELEMENT_REPOSITORY);
		repositoryElement.setAttribute(ATTRIBUTE_REPOSITORY_NAME, repositoryName);
		locationElement.appendChild(repositoryElement);
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

				if (isElement(node, ELEMENT_REPOSITORY)) {
					String name = ((Element) node).getAttribute(ATTRIBUTE_REPOSITORY_NAME);

					return new RepositoryTargetLocation().setRepository(name);
				}
			}

			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, "No repository name specified")); //$NON-NLS-1$
		}
	}

	@Override
	public void dispose() {
	}
}
