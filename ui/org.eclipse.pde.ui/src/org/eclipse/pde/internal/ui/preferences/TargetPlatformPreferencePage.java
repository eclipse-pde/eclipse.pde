/*******************************************************************************
 * Copyright (c) 2009, 2011 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.shared.target.TargetLocationLabelProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.wizards.target.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.osgi.framework.Version;

/**
 * Preference page for managing all known target definitions and setting one as the active target platform.
 */
public class TargetPlatformPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String ORG_ECLIPSE_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$

	private class TargetLabelProvider extends StyledCellLabelProvider {

		// Definition corresponding to running host
		private TargetDefinition fRunningHost;
		private Font fTextFont;

		public TargetLabelProvider() {
			PDEPlugin.getDefault().getLabelProvider().connect(this);
			ITargetPlatformService service = getTargetService();
			if (service != null) {
				fRunningHost = (TargetDefinition) service.newDefaultTarget();
			}
		}

		/**
		 * @return a bold dialog font
		 */
		private Font getBoldFont() {
			if (fTextFont == null) {
				Font dialogFont = JFaceResources.getDialogFont();
				FontData[] fontData = dialogFont.getFontData();
				for (int i = 0; i < fontData.length; i++) {
					FontData data = fontData[i];
					data.setStyle(SWT.BOLD);
				}
				Display display = getShell().getDisplay();
				fTextFont = new Font(display, fontData);
			}
			return fTextFont;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
		 */
		public void update(ViewerCell cell) {
			final Object element = cell.getElement();
			Styler style = new Styler() {
				public void applyStyles(TextStyle textStyle) {
					if (element.equals(fActiveTarget)) {
						textStyle.font = getBoldFont();
					}
				}
			};

			ITargetDefinition targetDef = (ITargetDefinition) element;
			ITargetHandle targetHandle = targetDef.getHandle();
			String name = targetDef.getName();
			if (name == null || name.length() == 0) {
				name = targetHandle.toString();
			}

			if (targetDef.equals(fActiveTarget)) {
				name = name + PDEUIMessages.TargetPlatformPreferencePage2_1;
			}

			StyledString styledString = new StyledString(name, style);
			if (targetHandle instanceof WorkspaceFileTargetHandle) {
				IFile file = ((WorkspaceFileTargetHandle) targetHandle).getTargetFile();
				String location = " - " + file.getFullPath(); //$NON-NLS-1$
				styledString.append(location, StyledString.DECORATIONS_STYLER);
			} else if (targetHandle instanceof ExternalFileTargetHandle) {
				URI uri = ((ExternalFileTargetHandle) targetHandle).getLocation();
				String location = " - " + uri.toASCIIString(); //$NON-NLS-1$
				styledString.append(location, StyledString.DECORATIONS_STYLER);
			} else {
				String location = (String) cell.getItem().getData(DATA_KEY_MOVED_LOCATION);
				if (location != null) {
					location = " - " + location; //$NON-NLS-1$
					styledString = new StyledString(name, style);
					styledString.append(location, StyledString.QUALIFIER_STYLER);
				}
			}

			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			cell.setImage(getImage(targetDef));
			super.update(cell);
		}

		private Image getImage(ITargetDefinition target) {
			int flag = 0;
			if (target.equals(fActiveTarget) && target.isResolved()) {
				if (target.getStatus().getSeverity() == IStatus.WARNING) {
					flag = SharedLabelProvider.F_WARNING;
				} else if (target.getStatus().getSeverity() == IStatus.ERROR) {
					flag = SharedLabelProvider.F_ERROR;
				}
			}
			if (fRunningHost != null && fRunningHost.isContentEquivalent(target)) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_BRANDING, flag);
			}
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION, flag);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#dispose()
		 */
		public void dispose() {
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
			if (fTextFont != null) {
				fTextFont.dispose();
				fTextFont = null;
			}
			super.dispose();
		}
	}

	/**
	 * Constant key value used to store data in table items if they are moved to a new location
	 */
	private final static String DATA_KEY_MOVED_LOCATION = "movedLocation"; //$NON-NLS-1$

	// Table viewer
	private CheckboxTableViewer fTableViewer;

	// Buttons
	private Button fReloadButton;
	private Button fAddButton;
	private Button fEditButton;
	//private Button fDuplicateButton;
	private Button fRemoveButton;
	private Button fMoveButton;

	// Text displaying additional information
	private TableViewer fDetails;

	/**
	 * Initial collection of targets (handles are realized into definitions as working copies)
	 */
	private List fTargets = new ArrayList();

	/**
	 * Removed definitions (to be removed on apply) 
	 */
	private List fRemoved = new ArrayList();

	/**
	 * Moved definitions (to be moved on apply)
	 */
	private Map fMoved = new HashMap(1);

	/**
	 * The chosen active target (will be loaded on apply)
	 */
	private ITargetDefinition fActiveTarget;

	/**
	 * Previously active target handle or null
	 */
	private ITargetHandle fPrevious;

	/**
	 * Stores whether the current target platform is out of synch with the file system and must be reloaded
	 */
	private boolean isOutOfSynch = false;

	/**
	 * Composite containing a warning image and label for when the backing file for the active target could not be found
	 */
	private Composite fWarningComp;

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
		((GridData) comp.getLayoutData()).widthHint = 350;
		SWTFactory.createWrapLabel(comp, PDEUIMessages.TargetPlatformPreferencePage2_0, 2);
		SWTFactory.createVerticalSpacer(comp, 1);

		Composite tableComposite = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createLabel(tableComposite, PDEUIMessages.TargetPlatformPreferencePage2_2, 2);

		fTableViewer = CheckboxTableViewer.newCheckList(tableComposite, SWT.MULTI | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fTableViewer.getControl().setLayoutData(gd);
		fTableViewer.setLabelProvider(new TargetLabelProvider());
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		fTableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					fTableViewer.setCheckedElements(new Object[] {event.getElement()});
					handleActivate();
				} else {
					handleActivate();
				}

			}
		});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
				updateDetails();
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleEdit();
			}
		});
		fTableViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					handleRemove();
				}
			}
		});

		// add the targets
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			ITargetHandle[] targets = service.getTargets(null);
			for (int i = 0; i < targets.length; i++) {
				try {
					fTargets.add(targets[i].getTargetDefinition());
				} catch (CoreException e) {
					PDECore.log(e);
					setErrorMessage(e.getMessage());
				}
			}
			fTableViewer.setInput(fTargets);
		}

		fTableViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				String name1 = ((TargetDefinition) e1).getName();
				String name2 = ((TargetDefinition) e2).getName();
				if (name1 == null) {
					return -1;
				}
				if (name2 == null) {
					return 1;
				}
				return name1.compareToIgnoreCase(name2);
			}
		});

		Composite buttonComposite = SWTFactory.createComposite(tableComposite, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);

		fReloadButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_16, null);
		fReloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleReload();
			}
		});

		SWTFactory.createVerticalSpacer(buttonComposite, 1);

		fAddButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_3, null);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fEditButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_5, null);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});

		fRemoveButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_7, null);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		fMoveButton = SWTFactory.createPushButton(buttonComposite, PDEUIMessages.TargetPlatformPreferencePage2_13, null);
		fMoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleMove();
			}
		});

		updateButtons();

		Composite detailsComposite = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_HORIZONTAL, 0, 0);
		SWTFactory.createLabel(detailsComposite, PDEUIMessages.TargetPlatformPreferencePage2_25, 1);
		fDetails = new TableViewer(detailsComposite);
		fDetails.setLabelProvider(new TargetLocationLabelProvider(true, true));
		fDetails.setContentProvider(new ArrayContentProvider());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 50;
		fDetails.getControl().setLayoutData(gd);

		if (service != null) {
			try {
				fPrevious = service.getWorkspaceTargetHandle();
				Iterator iterator = fTargets.iterator();
				while (iterator.hasNext()) {
					ITargetDefinition target = (ITargetDefinition) iterator.next();
					if (target.getHandle().equals(fPrevious)) {
						fActiveTarget = target;
						fTableViewer.setCheckedElements(new Object[] {fActiveTarget});
						fTableViewer.refresh(target);
						break;
					}
				}
				if (fPrevious != null && !fPrevious.exists()) {
					setMessage(PDEUIMessages.TargetPlatformPreferencePage2_23, IStatus.WARNING);
					fWarningComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
					Label warningImage = SWTFactory.createLabel(fWarningComp, "", 1); //$NON-NLS-1$
					gd = new GridData();
					gd.verticalAlignment = SWT.TOP;
					warningImage.setLayoutData(gd);
					warningImage.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
					SWTFactory.createWrapLabel(fWarningComp, PDEUIMessages.TargetPlatformPreferencePage2_24, 1);
					fWarningComp.moveAbove(tableComposite);
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
				setErrorMessage(PDEUIMessages.TargetPlatformPreferencePage2_23);
			}
			if (getMessage() == null && fActiveTarget == null) {
				setMessage(PDEUIMessages.TargetPlatformPreferencePage2_22, IMessageProvider.INFORMATION);
			}
		}
	}

	/**
	 * Validates the selected definition and sets it as the active platform
	 */
	private void handleActivate() {
		Object[] checked = fTableViewer.getCheckedElements();
		if (checked.length > 0) {
			fActiveTarget = (ITargetDefinition) checked[0];
			setMessage(null);
			if (fWarningComp != null) {
				Composite parent = fWarningComp.getParent();
				fWarningComp.dispose();
				fWarningComp = null;
				parent.layout();
			}
			fTableViewer.refresh(true);
			fTableViewer.setSelection(new StructuredSelection(fActiveTarget));
		} else {
			setMessage(PDEUIMessages.TargetPlatformPreferencePage2_22, IMessageProvider.INFORMATION);
			fActiveTarget = null;
			fTableViewer.refresh(true);
		}
	}

	private void handleReload() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		if (!selection.isEmpty()) {
			isOutOfSynch = false;
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell()) {
				protected void configureShell(Shell shell) {
					super.configureShell(shell);
					shell.setText(PDEUIMessages.TargetPlatformPreferencePage2_12);
				}
			};
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
						// Resolve the target
						fActiveTarget.resolve(monitor);
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
					}
				});
			} catch (InvocationTargetException e) {
				PDEPlugin.log(e);
				setErrorMessage(e.getMessage());
			} catch (InterruptedException e) {
				// Do nothing, resolve will happen when user presses ok
			}

			if (fActiveTarget.isResolved()) {
				// Check if the bundle resolution has errors
				IStatus bundleStatus = fActiveTarget.getStatus();
				if (bundleStatus.getSeverity() == IStatus.ERROR) {
					ErrorDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_14, PDEUIMessages.TargetPlatformPreferencePage2_15, bundleStatus, IStatus.ERROR);
				}

				// Compare the target to the existing platform
				try {
					if (bundleStatus.getSeverity() != IStatus.ERROR && fActiveTarget.getHandle().equals(fPrevious) && ((TargetDefinition) fPrevious.getTargetDefinition()).isContentEquivalent(fActiveTarget)) {
						IStatus compare = getTargetService().compareWithTargetPlatform(fActiveTarget);
						if (!compare.isOK()) {
							MessageDialog.openInformation(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_17, PDEUIMessages.TargetPlatformPreferencePage2_18);
							isOutOfSynch = true;
						}
					}
				} catch (CoreException e) {
					PDEPlugin.log(e);
					setErrorMessage(e.getMessage());
				}
			}
			fTableViewer.refresh(true);
		}
	}

	/**
	 * Open the new target platform wizard
	 */
	private void handleAdd() {
		NewTargetDefinitionWizard2 wizard = new NewTargetDefinitionWizard2();
		wizard.setWindowTitle(PDEUIMessages.TargetPlatformPreferencePage2_4);
		WizardDialog dialog = new WizardDialog(fAddButton.getShell(), wizard);
		if (dialog.open() == Window.OK) {
			ITargetDefinition def = wizard.getTargetDefinition();
			fTargets.add(def);
			if (fTargets.size() == 1) {
				fActiveTarget = def;
			}
			fTableViewer.refresh(true);
			fTableViewer.setSelection(new StructuredSelection(def));
		}
	}

	/**
	 * Opens the selected target for editing
	 */
	private void handleEdit() {
		if (!fTableViewer.getSelection().isEmpty()) {
			ITargetDefinition original = (ITargetDefinition) ((IStructuredSelection) fTableViewer.getSelection()).getFirstElement();
			EditTargetDefinitionWizard wizard = new EditTargetDefinitionWizard(original, true);
			wizard.setWindowTitle(PDEUIMessages.TargetPlatformPreferencePage2_6);
			WizardDialog dialog = new WizardDialog(fEditButton.getShell(), wizard);
			if (dialog.open() == Window.OK) {
				// Replace all references to the original with the new target
				ITargetDefinition newTarget = wizard.getTargetDefinition();
				int index = fTargets.indexOf(original);
				fTargets.add(index, newTarget);
				fTargets.remove(original);

				if (fMoved.containsKey(original)) {
					Object moveLocation = fMoved.remove(original);
					fMoved.put(newTarget, moveLocation);
				}

				if (original == fActiveTarget) {
					fActiveTarget = newTarget;
				}

				fTableViewer.refresh(true);

				if (fActiveTarget == newTarget) {
					fTableViewer.setCheckedElements(new Object[] {newTarget});
				}

				fTableViewer.setSelection(new StructuredSelection(newTarget));
			}
		}
	}

	/**
	 * Removes the selected targets
	 */
	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		if (!selection.isEmpty()) {
			List selected = selection.toList();

			// If we are going to remove a workspace file, prompt to ask the user first
			boolean isWorkspace = false;
			for (Iterator iterator = selected.iterator(); iterator.hasNext();) {
				ITargetDefinition currentTarget = (ITargetDefinition) iterator.next();
				if (currentTarget.getHandle() instanceof WorkspaceFileTargetHandle) {
					isWorkspace = true;
					break;
				}
			}
			if (isWorkspace) {
				PDEPreferencesManager preferences = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
				String choice = preferences.getString(IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET);
				if (!MessageDialogWithToggle.ALWAYS.equalsIgnoreCase(choice)) {
					MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_19, PDEUIMessages.TargetPlatformPreferencePage2_20, PDEUIMessages.TargetPlatformPreferencePage2_21, false, PDEPlugin.getDefault().getPreferenceStore(), IPreferenceConstants.PROP_PROMPT_REMOVE_TARGET);
					preferences.savePluginPreferences();
					if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
						return;
					}
				}
			}

			if (fActiveTarget != null && selected.contains(fActiveTarget)) {
				fActiveTarget = null;
				setMessage(PDEUIMessages.TargetPlatformPreferencePage2_22, IMessageProvider.INFORMATION);
			}
			fRemoved.addAll(selected);
			fTargets.removeAll(selected);
			// Quick hack because the first refresh loses the checkedState, which is being used to bold the active target
			fTableViewer.refresh(false);
			fTableViewer.refresh(true);
		}
	}

	/**
	 * Move the selected target to a workspace location
	 */
	private void handleMove() {
		MoveTargetDefinitionWizard wizard = new MoveTargetDefinitionWizard(fMoved.values());
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 450);
		if (dialog.open() == Window.OK) {
			TableItem ti = fTableViewer.getTable().getItem(fTableViewer.getTable().getSelectionIndex());
			IPath newTargetLoc = wizard.getTargetFileLocation();
			IFile file = PDECore.getWorkspace().getRoot().getFile(newTargetLoc);
			ti.setData(DATA_KEY_MOVED_LOCATION, file.getFullPath().toString());
			IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
			fMoved.put(selection.getFirstElement(), wizard.getTargetFileLocation());
			fTableViewer.refresh(true);
		}
	}

	/**
	 * Update enabled state of buttons
	 */
	protected void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		int size = selection.size();
		fEditButton.setEnabled(size == 1);
		fRemoveButton.setEnabled(size > 0);
		//fDuplicateButton.setEnabled(size == 1);
		if (selection.getFirstElement() != null) {
			fMoveButton.setEnabled(size == 1 && ((ITargetDefinition) selection.getFirstElement()).getHandle() instanceof LocalTargetHandle);
			fReloadButton.setEnabled(((ITargetDefinition) selection.getFirstElement()) == fActiveTarget && fActiveTarget.getHandle().equals(fPrevious));
		} else {
			fMoveButton.setEnabled(false);
			fReloadButton.setEnabled(false);
		}
	}

	/**
	 * Updates the details text box with information about the currently selected target 
	 */
	protected void updateDetails() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		if (selection.size() == 1) {
			fDetails.setInput(((ITargetDefinition) selection.getFirstElement()).getTargetLocations());
		} else {
			fDetails.setInput(null);
		}
	}

	/**
	 * Returns the target platform service or <code>null</code> if the service could
	 * not be acquired.
	 * 
	 * @return target platform service or <code>null</code>
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
		if (!manager.isInitialized()) {
			manager.getExternalModelManager();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	public void performDefaults() {
		// add a default target platform and select it (or just select it if present)
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			ITargetDefinition deflt = service.newDefaultTarget();
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
				fTableViewer.refresh(false);
			}
			fActiveTarget = deflt;
			fTableViewer.setCheckedElements(new Object[] {fActiveTarget});
			fTableViewer.refresh(true);
			handleActivate();
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
			ITargetHandle activeHandle = null;
			if (fActiveTarget != null) {
				activeHandle = fActiveTarget.getHandle();
			}
			if (fPrevious == null) {
				if (activeHandle != null) {
					toLoad = fActiveTarget;
					load = true;
				}
			} else {
				if (activeHandle == null) {
					// load empty
					load = true;
				} else if (!fPrevious.equals(activeHandle) || isOutOfSynch) {
					toLoad = fActiveTarget;
					load = true;
				} else {
					ITargetDefinition original = fPrevious.getTargetDefinition();
					// TODO: should just check for structural changes
					if (((TargetDefinition) original).isContentEquivalent(fActiveTarget)) {
						load = false;
					} else {
						load = true;
						toLoad = fActiveTarget;
					}
				}
			}
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage2_8, PDEUIMessages.TargetPlatformPreferencePage2_9, e.getStatus());
			return false;
		}

		// Move the marked definitions to workspace
		if (fMoved.size() > 0) {
			Iterator iterator = fMoved.keySet().iterator();
			while (iterator.hasNext()) {
				try {
					ITargetDefinition target = (ITargetDefinition) iterator.next();
					//IPath path = Path.fromPortableString((String) fMoved.get(target));
					IFile targetFile = PDECore.getWorkspace().getRoot().getFile((IPath) fMoved.get(target));

					WorkspaceFileTargetHandle wrkspcTargetHandle = new WorkspaceFileTargetHandle(targetFile);
					ITargetDefinition newTarget = service.newTarget();
					service.copyTargetDefinition(target, newTarget);
					wrkspcTargetHandle.save(newTarget);
					fRemoved.add(target);
					fTargets.remove(target);
					ITargetDefinition workspaceTarget = wrkspcTargetHandle.getTargetDefinition();
					fTargets.add(workspaceTarget);
					fTableViewer.refresh(false);
					if (target == fActiveTarget) {
						load = true;
						toLoad = workspaceTarget;
					}
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}

		// Remove any definitions that have been removed
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

			// Warn about forward compatibility, synchronize java search
			IJobChangeListener listener = new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().getSeverity() == IStatus.OK) {
						if (fActiveTarget != null) {
							PDEPreferencesManager pref = new PDEPreferencesManager(PDEPlugin.getPluginId());
							if (pref.getBoolean(IPreferenceConstants.ADD_TO_JAVA_SEARCH)) {
								AddToJavaSearchJob.synchWithTarget(fActiveTarget);
							}
							// Ignore the qualifier when comparing (otherwise N builds always newer than I builds)
							Version platformOsgiVersion = Platform.getBundle(ORG_ECLIPSE_OSGI).getVersion();
							platformOsgiVersion = new Version(platformOsgiVersion.getMajor(), platformOsgiVersion.getMinor(), platformOsgiVersion.getMicro());
							TargetBundle[] bundles;
							bundles = fActiveTarget.getAllBundles();
							if (bundles != null) {
								for (int index = 0; index < bundles.length; index++) {
									BundleInfo bundleInfo = bundles[index].getBundleInfo();
									if (ORG_ECLIPSE_OSGI.equalsIgnoreCase(bundleInfo.getSymbolicName())) {
										// Ignore the qualifier when comparing (otherwise N builds always newer than I builds)
										Version bundleVersion = Version.parseVersion(bundleInfo.getVersion());
										bundleVersion = new Version(bundleVersion.getMajor(), bundleVersion.getMinor(), bundleVersion.getMicro());
										if (platformOsgiVersion.compareTo(bundleVersion) < 0) {
											Display.getDefault().syncExec(new Runnable() {
												public void run() {
													MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.TargetPlatformPreferencePage2_28, PDEUIMessages.TargetPlatformPreferencePage2_10);
												}
											});
										}
										break;
									}
								}
							}
						}
					}
				}
			};

			LoadTargetDefinitionJob.load(toLoad, listener);
			fPrevious = toLoad == null ? null : toLoad.getHandle();
		}

		fMoved.clear();
		boolean gc = !fRemoved.isEmpty();
		fRemoved.clear();
		if (toLoad != null) {
			fActiveTarget = toLoad;
		}
		fTableViewer.refresh(true);
		updateButtons();
		// start job to do GC
		if (gc) {
			runGC();
		}
		return super.performOk();
	}

	private void runGC() {
		Job job = new Job(PDEUIMessages.TargetPlatformPreferencePage2_26) {
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(PDEUIMessages.TargetPlatformPreferencePage2_27, IProgressMonitor.UNKNOWN);
				P2TargetUtils.garbageCollect();
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
