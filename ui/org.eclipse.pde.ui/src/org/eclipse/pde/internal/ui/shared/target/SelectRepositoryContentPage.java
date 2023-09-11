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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.RepositoryBundleContainer;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;

public class SelectRepositoryContentPage extends WizardPage implements IEditBundleContainerPage {

	private final EditRepositoryContainerPage repositoryPage;
	private String lastLocation;
	private CheckboxTableViewer viewer;
	private Collection<Requirement> requirements;
	private final Set<Resource> selected = ConcurrentHashMap.newKeySet();

	protected SelectRepositoryContentPage(EditRepositoryContainerPage repositoryPage) {
		super("SelectRepositoryContentPage"); //$NON-NLS-1$
		this.repositoryPage = repositoryPage;
		setTitle(Messages.SelectRepositoryContentPage_Title);
		setDescription(Messages.SelectRepositoryContentPage_Description);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			try {
				RepositoryBundleContainer container = repositoryPage.getBundleContainer();
				if (requirements == null) {
					// only init the requirements once...
					requirements = new HashSet<>(container.getRequirements());
				}
				String location = container.getLocation(true);
				if (lastLocation != location) {
					// Load the repository!!
					getContainer().run(true, true, monitor -> {
						try {
							ResourcesRepository repository = container.getRepository(monitor);
							selected.clear();
							List<Resource> resources = repository.getResources();
							repository.findProviders(requirements).values().stream().flatMap(Collection::stream)
									.map(Capability::getResource).distinct().forEach(selected::add);
							if (viewer != null) {
								viewer.getControl().getDisplay().execute(() -> {
									if (viewer.getControl().isDisposed()) {
										return;
									}
									viewer.setInput(resources);
								});
							}
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					});
				}
				setErrorMessage(null);
			} catch (CoreException e) {
				setErrorMessage(e.getStatus().getMessage());
			} catch (InvocationTargetException e) {
				setErrorMessage(e.getMessage());
			} catch (InterruptedException e) {
				return;
			}
		}
		super.setVisible(visible);
	}

	@Override
	public void createControl(Composite parent) {

		viewer = CheckboxTableViewer.newCheckList(parent, SWT.NONE);
		viewer.setCheckStateProvider(new ICheckStateProvider() {

			@Override
			public boolean isGrayed(Object element) {
				return false;
			}

			@Override
			public boolean isChecked(Object element) {
				if (element instanceof Resource resource) {
					return selected.contains(resource);
				}
				return false;
			}
		});
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider() {
			private Image pluginImage;

			@Override
			public String getText(Object element) {
				if (element instanceof Resource resource) {
					return resource.toString();
				}
				return ""; //$NON-NLS-1$
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof Resource resource) {
					if (ResourceUtils.getBundleCapability(resource) != null) {
						if (pluginImage == null) {
							pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
							viewer.getControl().addDisposeListener(e -> pluginImage.dispose());
						}
						return pluginImage;
					}
				}
				return null;
			}
		});
		viewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof Resource resource) {
					Requirement requirement = getRequirement(resource);
					if (requirement != null) {
						if (event.getChecked()) {
							selected.add(resource);
							requirements.add(requirement);
						} else {
							selected.remove(resource);
							requirements.remove(requirement);
						}
					}
				}
			}
		});
		setControl(viewer.getControl());
	}

	protected Requirement getRequirement(Resource resource) {
		IdentityCapability identity = ResourceUtils.getIdentityCapability(resource);
		String v = identity.version().toString();
		return CapReqBuilder.createSimpleRequirement(identity.getNamespace(), identity.osgi_identity(),
				String.format("[%s,%s]", v, v)) //$NON-NLS-1$
				.setResource(resource).buildRequirement();
	}

	@Override
	public ITargetLocation getBundleContainer() {
		RepositoryBundleContainer container = repositoryPage.getBundleContainer();
		return new RepositoryBundleContainer(container.getUri(),
				requirements == null ? container.getRequirements() : List.copyOf(requirements));
	}

	@Override
	public void storeSettings() {

	}

}
