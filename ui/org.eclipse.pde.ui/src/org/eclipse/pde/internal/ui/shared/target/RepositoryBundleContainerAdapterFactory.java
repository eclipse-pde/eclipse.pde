/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.RepositoryBundleContainer;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Requirement;

public class RepositoryBundleContainerAdapterFactory implements IAdapterFactory {

	private static final Object[] EMPTY_OBJECTS = new Object[0];

	public static final ILabelProvider LABEL_PROVIDER = new LabelProvider() {

		private Image repositoryImage;
		private Image requirementImage;

		@Override
		public String getText(Object element) {
			if (element instanceof RepositoryBundleContainer container) {
				try {
					return container.getLocation(true);
				} catch (CoreException e) {
					return container.getUri();
				}
			}
			if (element instanceof RequirementNode requirement) {
				return requirement.requirement().toString();
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof RepositoryBundleContainer) {
				if (repositoryImage == null) {
					repositoryImage = PDEPluginImages.DESC_TARGET_REPO.createImage();
				}
				return repositoryImage;
			}
			if (element instanceof RequirementNode) {
				if (requirementImage == null) {
					requirementImage = PDEPluginImages.DESC_FILTER.createImage();
				}
				return requirementImage;
			}
			return null;
		}

		@Override
		public void dispose() {
			super.dispose();
			if (repositoryImage != null) {
				repositoryImage.dispose();
				repositoryImage = null;
			}
		}
	};

	private static final ITargetLocationHandler LOCATION_HANDLER = new ITargetLocationHandler() {

		@Override
		public IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
			for (ITargetLocation location : targetLocations) {
				if (location instanceof RepositoryBundleContainer container) {
					container.reload();
				}
			}
			return Status.OK_STATUS;
		}

		@Override
		public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
			Object segment = treePath.getLastSegment();
			if (segment instanceof RequirementNode node) {
				// TODO maybe we can support a requirements editor?
				segment = node.container;
			}
			if (segment instanceof RepositoryBundleContainer container) {
				RepositoryLocationWizard wizard = new RepositoryLocationWizard();
				wizard.setTarget(target);
				wizard.setBundleContainer(container);
				return wizard;
			}
			return null;
		}

		@Override
		public boolean canEdit(ITargetDefinition target, TreePath treePath) {
			Object segment = treePath.getLastSegment();
			return segment instanceof RepositoryBundleContainer || segment instanceof RequirementNode;
		}

		public boolean canRemove(ITargetDefinition target, TreePath treePath) {
			return treePath.getLastSegment() instanceof RequirementNode;
		}

		public IStatus remove(ITargetDefinition target, TreePath[] treePaths) {
			boolean reload = false;
			for (TreePath path : treePaths) {
				Object lastSegment = path.getLastSegment();
				if (lastSegment instanceof RequirementNode node) {
					RepositoryBundleContainer container = node.container;
					RepositoryBundleContainer newContainer = new RepositoryBundleContainer(container.getUri(),
							container.getRequirements().stream().filter(req -> req != node.requirement()).toList());
					ITargetLocation[] targetLocations = target.getTargetLocations();
					for (int i = 0; i < targetLocations.length; i++) {
						ITargetLocation loc = targetLocations[i];
						if (loc == container) {
							targetLocations[i] = newContainer;
						}

					}
				}
			}
			return reload
					? new Status(IStatus.OK, IPDEConstants.UI_PLUGIN_ID, ITargetLocationHandler.STATUS_FORCE_RELOAD,
							"reloaded", null) //$NON-NLS-1$
					: Status.OK_STATUS;
		}
	};

	private static final ITreeContentProvider TREE_CONTENT_PROVIDER = new ITreeContentProvider() {

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof RepositoryBundleContainer container) {
				return !container.getRequirements().isEmpty();
			}
			return false;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof RequirementNode node) {
				return node.container;
			}
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return EMPTY_OBJECTS; // will never be called...
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof RepositoryBundleContainer container) {
				return container.getRequirements().stream().map(req -> new RequirementNode(req, container)).toArray();
			}
			return EMPTY_OBJECTS;
		}
	};

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof RepositoryBundleContainer
				|| adaptableObject instanceof RequirementNode) {
			if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			}
			if (adapterType == ITargetLocationHandler.class) {
				return adapterType.cast(LOCATION_HANDLER);
			}
			if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ILabelProvider.class, ITargetLocationHandler.class, ITreeContentProvider.class };
	}

	/**
	 * Simple wrapper class to identify it uniquly when adaption take place and
	 * to record the parent
	 */
	public static final record RequirementNode(Requirement requirement, RepositoryBundleContainer container) {

	}


}
