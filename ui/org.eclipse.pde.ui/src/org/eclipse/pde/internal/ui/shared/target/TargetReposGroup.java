/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.IStatusCodes;
import org.eclipse.equinox.internal.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * UI group that displays a list of URI repository locations that are set on a target definition.  The
 * repos can be removed and new ones added.
 *  
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 */
public class TargetReposGroup {

	private TableViewer fTableViewer;
	private Button fAddButton;
	private Button fRemoveButton;
	private java.util.List fRepos;
	private ITargetDefinition fTarget;

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 * @return generated instance of the table part
	 */
	public static TargetReposGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetReposGroup contentTable = new TargetReposGroup();
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 * @return generated instance of the table part
	 */
	public static TargetReposGroup createInDialog(Composite parent) {
		TargetReposGroup contentTable = new TargetReposGroup();
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Private constructor, use one of {@link #createTableInDialog(Composite, ITargetChangedListener)}
	 * or {@link #createTableInForm(Composite, FormToolkit, ITargetChangedListener)}.
	 * 
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 */
	private TargetReposGroup() {
	}

	/**
	 * @return the list of repositories this table is displaying
	 */
	public URI[] getRepos() {
		return (URI[]) fRepos.toArray(new URI[fRepos.size()]);
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the table's input.  The target will not be modified.  
	 * @param target target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		fRepos = new ArrayList();
		if (target != null) {
			URI[] uris = target.getRepositories();
			if (uris != null && uris.length > 0) {
				for (int i = 0; i < uris.length; i++) {
					fRepos.add(uris[i]);
				}
			}
		}
		updateInput();
		updateButtons();
	}

	private void updateInput() {
		if (fRepos.isEmpty()) {
			fTableViewer.setInput(new String[] {Messages.TargetReposGroup_NoRepos});
			fTableViewer.getTable().setEnabled(false);
		} else {
			fTableViewer.setInput(fRepos.toArray());
			fTableViewer.getTable().setEnabled(true);
		}
	}

	/**
	 * Creates the part contents from a toolkit
	 * @param parent parent composite
	 * @param toolkit form toolkit to create widgets
	 */
	private void createFormContents(Composite parent, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		toolkit.createLabel(comp, Messages.TargetReposGroup_CurrentRepos);

		Table aTable = toolkit.createTable(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		aTable.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 500;
		gd.heightHint = 200;
		aTable.setLayoutData(gd);
		aTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
					handleRemove();
				}

			}
		});

		Composite buttonComp = toolkit.createComposite(comp);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.numColumns = 2;
		buttonComp.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = GridData.END;
		buttonComp.setLayoutData(data);

		fAddButton = toolkit.createButton(buttonComp, Messages.TargetReposGroup_Add, SWT.PUSH);
		fRemoveButton = toolkit.createButton(buttonComp, Messages.TargetReposGroup_Remove, SWT.PUSH);

		initializeTableViewer(aTable);
		initializeButtons();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 * @param parent parent composite
	 */
	private void createDialogContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		SWTFactory.createLabel(comp, Messages.TargetReposGroup_CurrentRepos, 1);

		Table aTable = new Table(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI);
		aTable.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 500;
		gd.heightHint = 200;
		aTable.setLayoutData(gd);
		aTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
					handleRemove();
				}
			}
		});

		Composite buttonComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		((GridData) buttonComp.getLayoutData()).horizontalAlignment = SWT.END;

		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.TargetReposGroup_Add, null);
		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_2, null);

		initializeTableViewer(aTable);
		initializeButtons();
	}

	/**
	 * Sets up the table viewer using the given table
	 * @param table
	 */
	private void initializeTableViewer(Table table) {
		fTableViewer = new TableViewer(table);
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setLabelProvider(new LabelProvider() {
			public Image getImage(Object element) {
				if (element instanceof URI) {
					return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REPOSITORY_OBJ);
				}
				return super.getImage(element);
			}
		});
		fTableViewer.setComparator(new ViewerComparator());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
	}

	/**
	 * Sets up the buttons, the button fields must already be created before calling this method
	 */
	private void initializeButtons() {
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fRemoveButton);

		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		fAddButton.setEnabled(true);
		SWTFactory.setButtonDimensionHint(fAddButton);
	}

	private void handleAdd() {
		IProvisioningAgent agent = null;
		ProvisioningUI provUI = null;
		try {
			agent = TargetPlatformService.getProvisioningAgent();
			String profileID = TargetPlatformService.getProfileID(fTarget);
			provUI = new ProvisioningUI(new ProvisioningSession(agent), profileID, new Policy());
		} catch (CoreException e) {
			PDEPlugin.log(e);
			// If we cannot load the proper agent, we can still use the SDK default to continue
			ProvisioningUI selfProvisioningUI = ProvisioningUI.getDefaultUI();
			provUI = new ProvisioningUI(selfProvisioningUI.getSession(), IProfileRegistry.SELF, new Policy());
		}
		AddRepositoryDialog dialog = new AddRepositoryDialog(fTableViewer.getControl().getShell(), provUI) {
			protected IStatus validateRepositoryURL(boolean contactRepositories) {
				// Override validation to prevent checking for duplicates.  The repo manager know about other repos outside of this target
				IStatus isValid = super.validateRepositoryURL(contactRepositories);
				if (!isValid.isOK() && isValid.getCode() == IStatusCodes.INVALID_REPOSITORY_LOCATION) {
					setOkEnablement(true);
					updateStatus(Status.OK_STATUS);
					return Status.OK_STATUS;
				}
				return isValid;
			}
		};
		dialog.setTitle(Messages.TargetReposGroup_AddRepository);
		dialog.open();
		URI location = dialog.getAddedLocation();
		if (location != null) {
			fRepos.add(location);
			updateInput();
			fTableViewer.setSelection(new StructuredSelection(location));
		}
		if (agent != null) {
			agent.stop();
		}

	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		if (!selection.isEmpty()) {
			fRepos.removeAll(selection.toList());
			fTableViewer.remove(selection.toArray());
			if (fRepos.isEmpty()) {
				updateInput();
			}
			updateButtons();
		}
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		// If any container is selected, allow the remove (the remove ignores non-container entries)
		boolean removeAllowed = false;
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			if (iter.next() instanceof URI) {
				removeAllowed = true;
				break;
			}
		}
		fRemoveButton.setEnabled(removeAllowed);
	}
}
