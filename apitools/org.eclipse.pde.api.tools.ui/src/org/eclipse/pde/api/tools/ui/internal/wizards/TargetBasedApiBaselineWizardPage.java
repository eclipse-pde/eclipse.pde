/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.SystemLibraryApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.ExternalFileTargetHandle;
import org.eclipse.pde.internal.core.target.WorkspaceFileTargetHandle;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

/**
 * Define a baseline from a Target Definition
 *
 * @since 1.0.600
 */
public class TargetBasedApiBaselineWizardPage extends ApiBaselineWizardPage {
	public static boolean isApplicable(IApiBaseline profile) {
		return ApiModelFactory.isDerivedFromTarget(profile);
	}

	/**
	 * Resets the baseline contents based on current settings and a target
	 * definition from which to read plug-ins.
	 */
	class ReloadTargetOperation implements IRunnableWithProgress {
		private ITargetDefinition definition;
		private String name;

		/**
		 * Constructor
		 *
		 * @param platformPath
		 */
		public ReloadTargetOperation(ITargetDefinition definition, String name) {
			this.definition = definition;
			this.name = name;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				fProfile = ApiModelFactory.newApiBaselineFromTarget(name, definition, monitor);
				TargetBasedApiBaselineWizardPage.this.contentchange = true;
			} catch (CoreException e) {
				// error reported via the definition's status in pageValid()
				ApiPlugin.log(e);
			}
		}
	}

	private static class TargetLabelProvider extends StyledCellLabelProvider {
		private PDELabelProvider pdeLabelProvider = PDEPlugin.getDefault().getLabelProvider();

		public TargetLabelProvider() {
			pdeLabelProvider.connect(this);
		}

		@Override
		public void update(ViewerCell cell) {
			final Object element = cell.getElement();

			ITargetDefinition targetDef = (ITargetDefinition) element;
			ITargetHandle targetHandle = targetDef.getHandle();
			String name = targetDef.getName();
			if (name == null || name.length() == 0) {
				name = targetHandle.toString();
			}

			StyledString styledString = new StyledString(name);
			if (targetHandle instanceof WorkspaceFileTargetHandle) {
				IFile file = ((WorkspaceFileTargetHandle) targetHandle).getTargetFile();
				String location = " - " + file.getFullPath(); //$NON-NLS-1$
				styledString.append(location, StyledString.DECORATIONS_STYLER);
			} else if (targetHandle instanceof ExternalFileTargetHandle) {
				URI uri = ((ExternalFileTargetHandle) targetHandle).getLocation();
				String location = " - " + uri.toASCIIString(); //$NON-NLS-1$
				styledString.append(location, StyledString.DECORATIONS_STYLER);
			}

			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			cell.setImage(getImage(targetDef));
			super.update(cell);
		}

		public Image getImage(Object e) {
			return pdeLabelProvider.get(PDEPluginImages.DESC_TARGET_DEFINITION);
		}

		@Override
		public void dispose() {
			pdeLabelProvider.disconnect(this);
			super.dispose();
		}
	}

	/**
	 * Initial collection of targets (handles are realized into definitions as
	 * working copies)
	 */
	private List<ITargetDefinition> fTargets = new ArrayList<>();

	private ITargetDefinition selectedTargetDefinition;

	/**
	 * widgets
	 */
	private Text nametext = null;
	private CheckboxTableViewer targetsViewer;
	private TreeViewer treeviewer = null;
	private Button reloadbutton = null;

	public TargetBasedApiBaselineWizardPage(IApiBaseline profile) {
		super(profile);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 4, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_5, 1);
		nametext = SWTFactory.createText(comp, SWT.BORDER | SWT.SINGLE, 3, GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		nametext.addModifyListener(e -> setPageComplete(pageValid()));

		SWTFactory.createVerticalSpacer(comp, 1);
		targetsViewer = CheckboxTableViewer.newCheckList(comp, SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().span(3, 1).grab(true, true).applyTo(targetsViewer.getControl());

		targetsViewer.setLabelProvider(new TargetLabelProvider());
		targetsViewer.setContentProvider(ArrayContentProvider.getInstance());
		targetsViewer.addCheckStateListener(event -> {
			if (event.getChecked()) {
				targetsViewer.setCheckedElements(new Object[] { event.getElement() });
				selectedTargetDefinition = (ITargetDefinition) event.getElement();
			} else {
				selectedTargetDefinition = null;
			}
			updateButtons();
			setPageComplete(pageValid());
		});

		// add the targets
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			for (ITargetHandle handle : service.getTargets(null)) {
				try {
					fTargets.add(handle.getTargetDefinition());
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
			if (fTargets.isEmpty()) {
				// Ensure we have at least a default TP
				try {
					fTargets.add(service.getWorkspaceTargetDefinition());
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
			targetsViewer.setInput(fTargets);
		}

		targetsViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				String name1 = ((ITargetDefinition) e1).getName();
				String name2 = ((ITargetDefinition) e2).getName();
				if (name1 == null) {
					return -1;
				}
				if (name2 == null) {
					return 1;
				}
				return name1.compareToIgnoreCase(name2);
			}
		});

		reloadbutton = SWTFactory.createPushButton(comp, WizardMessages.ApiProfileWizardPage_12, null);
		reloadbutton.setEnabled(targetsViewer.getCheckedElements().length == 1);
		reloadbutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doReload()));

		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_13, 4);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.horizontalSpan = 4;
		tree.setLayoutData(gd);
		treeviewer = new TreeViewer(tree);
		treeviewer.setLabelProvider(new ApiToolsLabelProvider());
		treeviewer.setContentProvider(new ContentProvider());
		treeviewer.setComparator(new ViewerComparator());
		treeviewer.setInput(getCurrentComponents());
		treeviewer.addSelectionChangedListener(event -> updateButtons());
		treeviewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IApiComponent) {
					IApiComponent component = (IApiComponent) element;
					try {
						if (component.isSourceComponent() || component.isSystemComponent()) {
							return false;
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
					return true;
				}
				return !(element instanceof SystemLibraryApiComponent);
			}
		});

		setControl(comp);
		if (fProfile != null) {
			nametext.setText(fProfile.getName());

			ITargetDefinition found = null;
			// Use isDerivedFromTarget as the target may have had its
			// sequence number bumped several times
			for (ITargetDefinition target : fTargets) {
				if (ApiModelFactory.isDerivedFromTarget(fProfile, target)) {
					found = target;
					break;
				}
			}
			if (found != null) {
				selectedTargetDefinition = found;
				targetsViewer.setCheckedElements(new Object[] { found });
			}
		}
		initialize();
		updateButtons();
		setPageComplete(pageValid());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_WIZARD_PAGE);
		Dialog.applyDialogFont(comp);
	}

	/**
	 * @return if the page is valid, such that it is considered complete and can
	 *         be 'finished'
	 */
	protected boolean pageValid() {
		setErrorMessage(null);
		// If a target is selected then report any resolving problems before
		// any missing-name errors. It's strange to click on 'reset' and see
		// the progress bar and having any resolving errors masked by a
		// missing-name or no-bundles-error
		ITargetDefinition selected = getSelectedTargetDefinition();
		if (selected != null && selected.getStatus() != null && !selected.getStatus().isOK()) {
			setErrorMessage(WizardMessages.TargetBasedApiBaselineWizardPage_loading_error);
			return false;
		}
		if (!isNameValid(nametext.getText().trim())) {
			return false;
		}
		if(targetsViewer.getCheckedElements().length != 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_select_target);
			reloadbutton.setEnabled(false);
			return false;
		}
		if (fProfile != null) {
			if (fProfile.getApiComponents().length == 0) {
				setErrorMessage(WizardMessages.ApiProfileWizardPage_2);
				return false;
			}
			IStatus status = fProfile.getExecutionEnvironmentStatus();
			if (status.getSeverity() == IStatus.ERROR) {
				setErrorMessage(status.getMessage());
				return false;
			}
			if (!ApiModelFactory.isUpToDateWithTarget(fProfile, selected)) {
				setErrorMessage(WizardMessages.ApiProfileWizardPage_location_needs_reset);
				return false;
			}
		} else {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_location_needs_reset);
			return false;
		}
		return true;
	}

	private ITargetDefinition getSelectedTargetDefinition() {
		// don't query the table directly as may be called from non-UI thread
		return selectedTargetDefinition;
	}

	/**
	 * Updates the state of a variety of buttons on this page
	 */
	protected void updateButtons() {
		reloadbutton.setEnabled(getSelectedTargetDefinition() != null);
	}

	@Override
	public IApiBaseline finish() throws IOException, CoreException {
		if (fProfile != null) {
			fProfile.setName(nametext.getText().trim());
		}
		return fProfile;
	}

	/**
	 * Reloads all of the plugins from the location specified in the location
	 * text field.
	 */
	protected void doReload() {
		IRunnableWithProgress op = new ReloadTargetOperation(getSelectedTargetDefinition(), nametext.getText().trim());
		try {
			getContainer().run(true, true, op);
			treeviewer.setInput(getCurrentComponents());
			treeviewer.refresh();
			setPageComplete(pageValid());
		} catch (InvocationTargetException ite) {
		} catch (InterruptedException ie) {
		}
	}

	private ITargetPlatformService getTargetService() {
		return ApiPlugin.getDefault().acquireService(ITargetPlatformService.class);
	}
}
