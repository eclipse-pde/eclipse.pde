/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.core.target.*;

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.*;

/**
 * Section for editing implicit dependencies in the target definition editor
 * @see EnvironmentPage
 * @see TargetEditor
 */
public class ImplicitDependenciesSection extends SectionPart {

	private TableViewer fViewer;
	private TargetEditor fEditor;
	private Button fAdd;
	private Button fRemove;
	private Button fRemoveAll;
	private Label fCount;

	public ImplicitDependenciesSection(FormPage page, Composite parent) {
		super(parent, page.getManagedForm().getToolkit(), Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fEditor = (TargetEditor) page.getEditor();
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/**
	 * @return The target model backing this editor
	 */
	private ITargetDefinition getTarget() {
		return fEditor.getTarget();
	}

	/**
	 * Creates the UI for this section.
	 * 
	 * @param section section the UI is being added to
	 * @param toolkit form toolkit used to create the widgets
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setText(PDEUIMessages.ImplicitDependenicesSection_Title);
		section.setDescription(PDEUIMessages.TargetImplicitPluginsTab_desc);
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTableViewer(toolkit, container);

		Composite buttonComp = toolkit.createComposite(container);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		createButtons(toolkit, buttonComp);

		toolkit.paintBordersFor(container);
		section.setClient(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(gd);
		updateButtons();
	}

	/**
	 * Creates the table viewer in this section
	 * @param toolkit toolkit used to create the widgets
	 * @param parent parent composite
	 */
	private void createTableViewer(FormToolkit toolkit, Composite parent) {
		// TODO Support global delete action, maybe copy/paste as well
		Table table = toolkit.createTable(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		fViewer = new TableViewer(table);
		fViewer.setContentProvider(new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				NameVersionDescriptor[] bundles = getTarget().getImplicitDependencies();
				if (bundles == null) {
					return new NameVersionDescriptor[0];
				}
				return bundles;
			}
		});
		fViewer.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				NameVersionDescriptor bundle1 = (NameVersionDescriptor) e1;
				NameVersionDescriptor bundle2 = (NameVersionDescriptor) e2;
				return super.compare(viewer, bundle1.getId(), bundle2.getId());
			}
		});
		fViewer.setInput(getTarget());
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				Object object = ((IStructuredSelection) event.getSelection()).getFirstElement();
				ManifestEditor.openPluginEditor(((NameVersionDescriptor) object).getId());
			}
		});
	}

	/**
	 * Creates the buttons that sit beside the table
	 * @param toolkit toolkit used to create widgets
	 * @param parent parent composite
	 */
	private void createButtons(FormToolkit toolkit, Composite parent) {
		fAdd = toolkit.createButton(parent, PDEUIMessages.ImplicitDependenicesSection_Add, SWT.PUSH);
		fAdd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		fAdd.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		SWTFactory.setButtonDimensionHint(fAdd);
		fRemove = toolkit.createButton(parent, PDEUIMessages.ImplicitDependenicesSection_Remove, SWT.PUSH);
		fRemove.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		SWTFactory.setButtonDimensionHint(fRemove);
		fRemoveAll = toolkit.createButton(parent, PDEUIMessages.ImplicitDependenicesSection_RemoveAll, SWT.PUSH);
		fRemoveAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		fRemoveAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		SWTFactory.setButtonDimensionHint(fRemoveAll);
		Composite countComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		countComp.setLayout(layout);
		countComp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END | GridData.FILL_BOTH));
		fCount = toolkit.createLabel(parent, ""); //$NON-NLS-1$
		fCount.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		fCount.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void updateButtons() {
		fRemove.setEnabled(!fViewer.getSelection().isEmpty());
		fRemoveAll.setEnabled(fViewer.getTable().getItemCount() > 0);
	}

	/**
	 * Updates the label in the bottom right with the current count of implicit plug-ins
	 */
	private void updateCount() {
		if (fCount != null && !fCount.isDisposed())
			fCount.setText(NLS.bind(PDEUIMessages.TableSection_itemCount, Integer.toString(fViewer.getTable().getItemCount())));
	}

	protected void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new StyledBundleLabelProvider(false, false));
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title);
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		try {
			dialog.setElements(getValidBundles());
		} catch (CoreException e) {
			dialog.setMessage(e.getMessage());
		}
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.IMPLICIT_DEPENDENCIES_SELECTION_DIALOG);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			ArrayList pluginsToAdd = new ArrayList();
			for (int i = 0; i < models.length; i++) {
				BundleInfo selected = ((BundleInfo) models[i]);
				pluginsToAdd.add(new NameVersionDescriptor(selected.getSymbolicName(), null));
			}
			Set allDependencies = new HashSet();
			allDependencies.addAll(pluginsToAdd);
			NameVersionDescriptor[] currentBundles = getTarget().getImplicitDependencies();
			if (currentBundles != null) {
				allDependencies.addAll(Arrays.asList(currentBundles));
			}
			getTarget().setImplicitDependencies((NameVersionDescriptor[]) allDependencies.toArray(new NameVersionDescriptor[allDependencies.size()]));
			markDirty();
			refresh();
		}
	}

	/**
	 * Gets a list of all the bundles that can be added as implicit dependencies
	 * @return list of possible dependencies
	 */
	protected BundleInfo[] getValidBundles() throws CoreException {
		NameVersionDescriptor[] current = getTarget().getImplicitDependencies();
		Set currentBundles = new HashSet();
		if (current != null) {
			for (int i = 0; i < current.length; i++) {
				currentBundles.add(current[i].getId());
			}
		}

		List targetBundles = new ArrayList();
		TargetBundle[] allTargetBundles = getTarget().getAllBundles();
		if (allTargetBundles == null || allTargetBundles.length == 0) {
			throw new CoreException(new Status(IStatus.WARNING, PDEPlugin.getPluginId(), PDEUIMessages.ImplicitDependenciesSection_0));
		}
		for (int i = 0; i < allTargetBundles.length; i++) {
			if (!currentBundles.contains(allTargetBundles[i].getBundleInfo().getSymbolicName())) {
				targetBundles.add(allTargetBundles[i].getBundleInfo());
			}
		}

		return (BundleInfo[]) targetBundles.toArray(new BundleInfo[targetBundles.size()]);
	}

	private void handleRemove() {
		LinkedList bundles = new LinkedList();
		bundles.addAll(Arrays.asList(getTarget().getImplicitDependencies()));
		Object[] removeBundles = ((IStructuredSelection) fViewer.getSelection()).toArray();
		if (removeBundles.length > 0) {
			for (int i = 0; i < removeBundles.length; i++) {
				if (removeBundles[i] instanceof NameVersionDescriptor) {
					bundles.remove(removeBundles[i]);
				}
			}
			getTarget().setImplicitDependencies((NameVersionDescriptor[]) bundles.toArray((new NameVersionDescriptor[bundles.size()])));
			markDirty();
			refresh();
		}
	}

	private void handleRemoveAll() {
		getTarget().setImplicitDependencies(null);
		markDirty();
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		// TODO Try to retain selection during refresh, add and remove operations
		fViewer.setInput(getTarget());
		fViewer.refresh();
		updateButtons();
		updateCount();
		super.refresh();
	}

}
