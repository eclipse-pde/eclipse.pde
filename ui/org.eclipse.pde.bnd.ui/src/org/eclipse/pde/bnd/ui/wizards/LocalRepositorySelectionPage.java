/*******************************************************************************
 * Copyright (c) 2010, 2024 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.wizards;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryTreeContentProvider;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryTreeLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;

class LocalRepositorySelectionPage extends WizardPage {
	private static final ILogger		logger				= Logger.getLogger(LocalRepositorySelectionPage.class);

	public static final String			PROP_SELECTED_REPO	= "selectedRepository";

	private final PropertyChangeSupport	propSupport			= new PropertyChangeSupport(this);
	private RepositoryPlugin			selectedRepository	= null;

	private final Workspace workspace;

	LocalRepositorySelectionPage(Workspace workspace, String pageName) {
		this(workspace, pageName, null);
	}

	LocalRepositorySelectionPage(Workspace workspace, String pageName, RepositoryPlugin selectedRepository) {
		super(pageName);
		this.workspace = workspace;
		this.selectedRepository = selectedRepository;
	}

	@Override
	public void createControl(Composite parent) {
		setTitle("Select Local Repository");
		setMessage("Bundle will be imported into the selected repository.");

		Table table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);

		final TableViewer viewer = new TableViewer(table);
		viewer.setContentProvider(new RepositoryTreeContentProvider());
		viewer.setLabelProvider(new RepositoryTreeLabelProvider(false));
		viewer.setFilters(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return (element instanceof RepositoryPlugin) && ((RepositoryPlugin) element).canWrite();
			}
		});

		try {
			viewer.setInput(workspace);
			if (selectedRepository != null)
				viewer.setSelection(new StructuredSelection(selectedRepository));

			validate(workspace);
		} catch (Exception e) {
			logger.logError("Error querying local repositories", e);
			setErrorMessage("Error querying local repositories, see log for details.");
		}

		// LISTENERS
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			setSelectedRepository((RepositoryPlugin) selection.getFirstElement());
		});
		viewer.addOpenListener(evt -> {
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			setSelectedRepository((RepositoryPlugin) selection.getFirstElement());

			IWizardPage nextPage = getNextPage();
			if (nextPage != null)
				getContainer().showPage(nextPage);
		});

		setControl(table);
	}

	private void validate(Workspace workspace) {
		String error = "No writeable local repositories are configured.";
		List<RepositoryPlugin> plugins = workspace.getPlugins(RepositoryPlugin.class);
		if (plugins != null)
			for (RepositoryPlugin plugin : plugins) {
				if (plugin.canWrite()) {
					error = null;
					break;
				}
			}
		setErrorMessage(error);
	}

	@Override
	public boolean isPageComplete() {
		return selectedRepository != null;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(propertyName, listener);
	}

	private void setSelectedRepository(RepositoryPlugin item) {
		RepositoryPlugin old = selectedRepository;
		selectedRepository = item;
		propSupport.firePropertyChange(PROP_SELECTED_REPO, old, selectedRepository);

		getContainer().updateButtons();
	}

	public RepositoryPlugin getSelectedRepository() {
		return selectedRepository;
	}
}
