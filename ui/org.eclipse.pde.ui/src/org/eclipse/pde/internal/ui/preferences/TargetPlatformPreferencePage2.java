/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.target.impl.TargetDefinition;
import org.eclipse.pde.internal.core.target.impl.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.target.EditTargetDefinitionWizard;
import org.eclipse.pde.internal.ui.wizards.target.NewTargetDefinitionWizard2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

public class TargetPlatformPreferencePage2 extends PreferencePage implements IWorkbenchPreferencePage {

	private class TargetLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION);
		}

		public String getText(Object element) {
			String name = ((ITargetDefinition) element).getName();
			if (name == null || name.length() == 0) {
				return ((ITargetDefinition) element).getHandle().toString();
			}
			return name;
		}
	}

	// Table viewer
	private CheckboxTableViewer fTableViewer = null;

	// Buttons
	private Button fAddButton = null;
	private Button fEditButton = null;
	//private Button fDuplicateButton = null;
	private Button fRemoveButton = null;

	// Initial collection of targets (handles are realized into definitions as working copies)
	private List fTargets = new ArrayList();

	// Removed definitions (to be removed on apply)
	private List fRemoved = new ArrayList();

	// Handle that was previous selected on this page or null
	private ITargetHandle fPrevious;

	public TargetPlatformPreferencePage2() {
		// nothing
	}

	public void dispose() {
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public Control createContents(Composite parent) {
		Composite container = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		createTargetProfilesGroup(container);

		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TARGET_PLATFORM_PREFERENCE_PAGE);
		return container;
	}

	private void createTargetProfilesGroup(Composite container) {
		Composite comp = SWTFactory.createComposite(container, 1, 1, GridData.FILL_BOTH, 0, 0);
		((GridData) comp.getLayoutData()).widthHint = 500;
		SWTFactory.createWrapLabel(comp, PDEUIMessages.TargetPlatformPreferencePage2_0, 2);
		SWTFactory.createWrapLabel(comp, PDEUIMessages.TargetPlatformPreferencePage2_1, 2);
		SWTFactory.createVerticalSpacer(comp, 1);

		Composite tableComposite = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createWrapLabel(tableComposite, PDEUIMessages.TargetPlatformPreferencePage2_2, 2);
		Table table = new Table(tableComposite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.CHECK);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		fTableViewer = new CheckboxTableViewer(table);
		fTableViewer.setLabelProvider(new TargetLabelProvider());
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSource() instanceof CheckboxTableViewer) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					if (!((CheckboxTableViewer) event.getSource()).getChecked(selection.getFirstElement())) {
						fTableViewer.setCheckedElements(new Object[] {selection.getFirstElement()});
					}
				}
			}
		});
		fTableViewer.setComparator(new ViewerComparator());

		// add the targets
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			ITargetHandle[] targets = service.getTargets(null);
			for (int i = 0; i < targets.length; i++) {
				try {
					fTargets.add(targets[i].getTargetDefinition());
				} catch (CoreException e) {
					setErrorMessage(e.getMessage());
				}
			}
			fTableViewer.setInput(fTargets);
		}

		// Single check behavior
		fTableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					fTableViewer.setCheckedElements(new Object[] {event.getElement()});
				} else {
					fTableViewer.setCheckedElements(new Object[0]);
				}
			}
		});

		Composite buttonComposite = SWTFactory.createComposite(tableComposite, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		fAddButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_3, null);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				NewTargetDefinitionWizard2 wizard = new NewTargetDefinitionWizard2();
				wizard.setWindowTitle(PDEUIMessages.TargetPlatformPreferencePage2_4);
				WizardDialog dialog = new WizardDialog(fAddButton.getShell(), wizard);
				if (dialog.open() == Window.OK) {
					ITargetDefinition def = wizard.getTargetDefinition();
					fTargets.add(def);
					fTableViewer.refresh();
					fTableViewer.setSelection(new StructuredSelection(def));
				}
			}
		});

		fEditButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_5, null);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ITargetDefinition def = (ITargetDefinition) ((IStructuredSelection) fTableViewer.getSelection()).getFirstElement();
				EditTargetDefinitionWizard wizard = new EditTargetDefinitionWizard(def);
				wizard.setWindowTitle(PDEUIMessages.TargetPlatformPreferencePage2_6);
				WizardDialog dialog = new WizardDialog(fEditButton.getShell(), wizard);
				if (dialog.open() == Window.OK) {
					fTableViewer.refresh();
				}
			}
		});

		// TODO: post M5, implement "duplicate"
//		fDuplicateButton = SWTFactory.createPushButton(buttonComposite, "Duplicate...", null);
//		fDuplicateButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				// for now we always duplicate to local metadata
//				IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
//				ITargetDefinition source = (ITargetDefinition) selection.getFirstElement();
//				ITargetPlatformService service = getTargetService();
//				if (service != null) {
//					ITargetDefinition dup = service.newTarget();
//					try {
//						service.copyTargetDefinition(source, dup);
//						dup.setName(NLS.bind("Copy of {0}", source.getName()));
//						fTargets.add(dup);
//						fTableViewer.refresh();
//						fTableViewer.setSelection(new StructuredSelection(dup));
//					} catch (CoreException ex) {
//						setErrorMessage(ex.getMessage());
//					}
//				}
//			}
//		});

		fRemoveButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_7, null);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
				List selected = selection.toList();
				fRemoved.addAll(selected);
				fTargets.removeAll(selected);
				fTableViewer.refresh();
				// TODO: update selection
			}
		});

		updateButtons();

		if (service != null) {
			try {
				fPrevious = service.getWorkspaceTargetHandle();
				Iterator iterator = fTargets.iterator();
				while (iterator.hasNext()) {
					ITargetDefinition target = (ITargetDefinition) iterator.next();
					if (target.getHandle().equals(fPrevious)) {
						fTableViewer.setCheckedElements(new Object[] {target});
						break;
					}
				}
			} catch (CoreException e) {
				setErrorMessage(e.getMessage());
			}
		}
	}

	/**
	 * Update enabled state of buttons
	 */
	protected void updateButtons() {
		// update enabled state of buttons
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		int size = selection.size();
		fRemoveButton.setEnabled(size > 0);
		fEditButton.setEnabled(size == 1);
		//fDuplicateButton.setEnabled(size == 1);
	}

	/**
	 * Returns the target platform service.
	 * 
	 * @return target platform service
	 */
	private ITargetPlatformService getTargetService() {
		return (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// ensures default targets are created when page is opened (if not created yet)
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.getExternalModelManager();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	public void performDefaults() {
		// add a default target platform and select it (or just select it if present)
		ITargetPlatformService service = getTargetService();
		if (service instanceof TargetPlatformService) {
			TargetPlatformService ts = (TargetPlatformService) service;
			ITargetDefinition deflt = ts.newDefaultTargetDefinition();
			Iterator iterator = fTargets.iterator();
			ITargetDefinition reuse = null;
			while (iterator.hasNext()) {
				TargetDefinition existing = (TargetDefinition) iterator.next();
				if (existing.isContentEquivalent(deflt)) {
					reuse = existing;
					break;
				}
			}
			if (reuse != null) {
				deflt = reuse;
			} else {
				fTargets.add(deflt);
			}
			fTableViewer.refresh();
			fTableViewer.setCheckedElements(new Object[] {deflt});
		}
		super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		ITargetPlatformService service = getTargetService();
		if (service == null) {
			return false;
		}

		// determine if default target has changed
		ITargetDefinition toLoad = null;
		boolean load = false;
		try {
			ITargetHandle currH = null;
			ITargetDefinition currD = null;
			Object[] elements = fTableViewer.getCheckedElements();
			if (elements.length > 0) {
				currD = (ITargetDefinition) elements[0];
				currH = currD.getHandle();
			}
			if (fPrevious == null) {
				if (currH != null) {
					toLoad = currD;
					load = true;
				}
			} else {
				if (currH == null) {
					// load empty
					load = true;
				} else if (!fPrevious.equals(currH)) {
					toLoad = currD;
					load = true;
				} else {
					ITargetDefinition original = fPrevious.getTargetDefinition();
					// TODO: should just check for structural changes
					if (((TargetDefinition) original).isContentEquivalent(currD)) {
						load = false;
					} else {
						load = true;
						toLoad = currD;
					}
				}
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_8, PDEUIMessages.TargetPlatformPreferencePage2_9, e.getStatus());
			return false;
		}

		// Remove any definitions that have been removed
		// TODO should we prompt? (only if workspace files?)
		Iterator iterator = fRemoved.iterator();
		while (iterator.hasNext()) {
			ITargetDefinition target = (ITargetDefinition) iterator.next();
			try {
				service.deleteTarget(target.getHandle());
			} catch (CoreException e) {
				ErrorDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_8, PDEUIMessages.TargetPlatformPreferencePage2_11, e.getStatus());
				return false;
			}
		}
		// save others that are dirty
		iterator = fTargets.iterator();
		while (iterator.hasNext()) {
			ITargetDefinition def = (ITargetDefinition) iterator.next();
			boolean save = true;
			if (def.getHandle().exists()) {
				try {
					ITargetDefinition original = def.getHandle().getTargetDefinition();
					if (((TargetDefinition) original).isContentEqual(def)) {
						save = false;
					}
				} catch (CoreException e) {
					// failed to generate original
					setErrorMessage(e.getMessage());
					return false;
				}
			}
			if (save) {
				try {
					service.saveTargetDefinition(def);
				} catch (CoreException e) {
					setErrorMessage(e.getMessage());
					return false;
				}
			}
		}

		// set workspace target if required
		if (load) {

			// TODO: prompt to warn of build? (like JRE page)

			if (toLoad != null) {
				final ITargetDefinition targetToLoad = toLoad;

				if (!toLoad.isResolved()) {
					// Resolve the target in a progress dialog so if there are any errors we can keep the preference page open
					try {
						new ProgressMonitorDialog(getShell()).run(true, true, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								if (!targetToLoad.isResolved()) {
									targetToLoad.resolve(monitor);
								}
								if (monitor.isCanceled()) {
									throw new InterruptedException();
								}
							}
						});
					} catch (InterruptedException e) {
						return false;
					} catch (InvocationTargetException e) {
						// Completely unexpected error
						PDECore.log(e);
						return false;
					}
				}
				IStatus status = targetToLoad.getBundleStatus();
				if (status == null) {
					ErrorDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_10, PDEUIMessages.TargetPlatformPreferencePage2_12, new Status(IStatus.ERROR, PDEPlugin.getPluginId(), "")); //$NON-NLS-1$
					return false;
				}
				if (!status.isOK()) {
					// TODO Support prompting for differing levels of errors/warnings/info
					ErrorDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_10, PDEUIMessages.TargetPlatformPreferencePage2_12, status);
					return false;
				}
			}

			// Resolution was successful, good to go
			Job job = new LoadTargetDefinitionJob(toLoad);
			job.schedule();

		}

		return super.performOk();
	}

}
