/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.pde.ui.target.ITargetLocationEditor;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.progress.UIJob;

/**
 * UI part that can be added to a dialog or to a form editor.  Contains a table displaying
 * the bundle containers of a target definition.  Also has buttons to add, edit and remove
 * bundle containers of varying types.
 *
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see ITargetLocation
 */
public class TargetLocationsGroup {

	private TreeViewer fTreeViewer;
	private Action fCopySelectionAction;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fUpdateButton;
	private Button fReloadButton;
	private Button fShowContentButton;

	private ITargetDefinition fTarget;
	private ListenerList<ITargetChangedListener> fChangeListeners = new ListenerList<>();
	private ListenerList<ITargetChangedListener> fReloadListeners = new ListenerList<>();

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 *
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @return generated instance of the table part
	 */
	public static TargetLocationsGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetLocationsGroup contentTable = new TargetLocationsGroup();
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 *
	 * @param parent parent composite
	 * @return generated instance of the table part
	 */
	public static TargetLocationsGroup createInDialog(Composite parent) {
		TargetLocationsGroup contentTable = new TargetLocationsGroup();
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Private constructor, use one of {@link #createInDialog(Composite)} or
	 * {@link #createInForm(Composite, FormToolkit)}.
	 */
	private TargetLocationsGroup() {

	}

	/**
	 * Adds a listener to the set of listeners that will be notified when the bundle containers
	 * are modified.  This method has no effect if the listener has already been added.
	 *
	 * @param listener target changed listener to add
	 */
	public void addTargetChangedListener(ITargetChangedListener listener) {
		fChangeListeners.add(listener);
	}

	/**
	 * Adds a listener to the set of listeners that will be notified when target
	 * is  reloaded.  This method has no effect if the listener has already been added.
	 *
	 * @param listener target changed listener to add
	 */
	public void addTargetReloadListener(ITargetChangedListener listener) {
		fReloadListeners.add(listener);
	}

	/**
	 * Creates the part contents from a toolkit
	 * @param parent parent composite
	 * @param toolkit form toolkit to create widgets
	 */
	private void createFormContents(Composite parent, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		Tree atree = toolkit.createTree(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		atree.setLayoutData(gd);
		initializeTree(atree);

		Composite buttonComp = toolkit.createComposite(comp);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_0, SWT.PUSH);
		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_1, SWT.PUSH);
		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_2, SWT.PUSH);
		fUpdateButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_3, SWT.PUSH);
		fUpdateButton.setToolTipText(Messages.TargetLocationsGroup_update);
		fReloadButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_4, SWT.PUSH);
		fReloadButton.setToolTipText(Messages.TargetLocationsGroup_reload);

		fShowContentButton = toolkit.createButton(comp, Messages.TargetLocationsGroup_1, SWT.CHECK);

		initializeTreeViewer(atree);
		initializeButtons();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 * @param parent parent composite
	 */
	private void createDialogContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		Tree atree = new Tree(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI);
		atree.setFont(comp.getFont());
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		atree.setLayoutData(gd);
		initializeTree(atree);

		Composite buttonComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_0, null);
		fEditButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_1, null);
		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_2, null);
		fUpdateButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_3, null);
		fReloadButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_4, null);

		fShowContentButton = SWTFactory.createCheckButton(comp, Messages.TargetLocationsGroup_1, null, false, 2);

		initializeTreeViewer(atree);
		initializeButtons();
	}

	private void initializeTree(Tree tree) {
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
					handleRemove();
				} else if (e.keyCode == 'c' && (e.stateMask & SWT.CTRL) != 0) {
					fCopySelectionAction.run();
				}
			}
		});
	}

	/**
	 * Sets up the tree viewer using the given tree
	 */
	private void initializeTreeViewer(Tree tree) {
		fTreeViewer = new TreeViewer(tree);
		fTreeViewer.setContentProvider(new TargetLocationContentProvider());
		fTreeViewer.setLabelProvider(new TargetLocationLabelProvider(true, false));
		fTreeViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				// Status at the end of the list
				if (e1 instanceof IStatus && !(e2 instanceof IStatus)) {
					return 1;
				}
				if (e2 instanceof IStatus && !(e1 instanceof IStatus)) {
					return -1;
				}
				return super.compare(viewer, e1, e2);
			}
		});
		fTreeViewer.addSelectionChangedListener(event -> updateButtons());
		fTreeViewer.addDoubleClickListener(event -> {
			if (!event.getSelection().isEmpty()) {
				handleEdit();
			}
		});
		fTreeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		createContextMenu(fTreeViewer.getTree());
	}

	private void createContextMenu(Tree tree) {
		fCopySelectionAction = new CopyTreeSelectionAction(tree);

		MenuManager menuManager = new MenuManager();
		menuManager.add(fCopySelectionAction);

		Menu menu = menuManager.createContextMenu(tree);
		tree.setMenu(menu);
	}

	/**
	 * Sets up the buttons, the button fields must already be created before calling this method
	 */
	private void initializeButtons() {
		fAddButton.addSelectionListener(widgetSelectedAdapter(e -> handleAdd()));
		fAddButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fAddButton);

		fEditButton.addSelectionListener(widgetSelectedAdapter(e -> handleEdit()));
		fEditButton.setLayoutData(new GridData());
		fEditButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fEditButton);

		fRemoveButton.addSelectionListener(widgetSelectedAdapter(e -> handleRemove()));
		fRemoveButton.setLayoutData(new GridData());
		fRemoveButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fRemoveButton);

		fUpdateButton.addSelectionListener(widgetSelectedAdapter(e -> handleUpdate()));
		fUpdateButton.setLayoutData(new GridData());
		fUpdateButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fUpdateButton);

		fReloadButton.addSelectionListener(widgetSelectedAdapter(e -> handleReload()));
		fReloadButton.setLayoutData(new GridData());
		fReloadButton.setEnabled(true);
		SWTFactory.setButtonDimensionHint(fReloadButton);

		fShowContentButton.addSelectionListener(widgetSelectedAdapter(e -> {
			((TargetLocationContentProvider) fTreeViewer.getContentProvider()).setShowLocationContent(fShowContentButton.getSelection());
			fTreeViewer.refresh();
			fTreeViewer.expandAll();
		}));
		fShowContentButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fShowContentButton);
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the tree's input.
	 * @param target target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		fTreeViewer.setInput(fTarget);
		updateButtons();
	}

	private void handleAdd() {
		AddBundleContainerWizard wizard = new AddBundleContainerWizard(fTarget);
		Shell parent = fTreeViewer.getTree().getShell();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		if (dialog.open() != Window.CANCEL) {
			contentsChanged(false);
			fTreeViewer.refresh();
			updateButtons();
		}
	}

	private void handleEdit() {
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object currentSelection = iterator.next();
			if (currentSelection instanceof ITargetLocation) {
				ITargetLocation location = (ITargetLocation) currentSelection;
				ITargetLocationEditor editor = Platform.getAdapterManager().getAdapter(location, ITargetLocationEditor.class);
				if (editor != null) {
					if (editor.canEdit(fTarget, location)) {
						IWizard editWizard = editor.getEditWizard(fTarget, location);
						if (editWizard != null) {
							Shell parent = fTreeViewer.getTree().getShell();
							WizardDialog wizard = new WizardDialog(parent, editWizard);
							if (wizard.open() == Window.OK) {
								// Update the table
								// TODO Do we need to force a resolve for IUBundleContainers?
								contentsChanged(false);
								fTreeViewer.refresh();
								updateButtons();
								// TODO We can't restore selection if they replace the location
								fTreeViewer.setSelection(new StructuredSelection(location), true);
							}
						}
						break; //Only open for one selected item
					}
				} else if (location instanceof AbstractBundleContainer) {
					// TODO Custom code for locations that don't use adapters yet
					Shell parent = fTreeViewer.getTree().getShell();
					EditBundleContainerWizard wizard = new EditBundleContainerWizard(fTarget, location);
					WizardDialog dialog = new WizardDialog(parent, wizard);
					if (dialog.open() == Window.OK) {
						contentsChanged(false);
						fTreeViewer.refresh();
						updateButtons();
						// TODO We can't restore selection if they replace the location
						fTreeViewer.setSelection(new StructuredSelection(location), true);
					}
					break; //Only open for one selected item
				}
			} else if (currentSelection instanceof IUWrapper) {
				// TODO Custom code to allow editing of individual IUs
				IUWrapper wrapper = (IUWrapper) currentSelection;
				Shell parent = fTreeViewer.getTree().getShell();
				EditBundleContainerWizard editWizard = new EditBundleContainerWizard(fTarget, wrapper.getParent());
				WizardDialog wizard = new WizardDialog(parent, editWizard);
				if (wizard.open() == Window.OK) {
					// Update the table
					// TODO Do we need to force a resolve for IUBundleContainers?
					contentsChanged(false);
					fTreeViewer.refresh();
					updateButtons();
					// TODO We can't restore selection if they replace the location
					fTreeViewer.setSelection(new StructuredSelection(wrapper.getParent()), true);
				}
				break; //Only open for one selected item
			}
		}
	}

	private void handleRemove() {
		// TODO Contains custom code to remove individual IUWrappers
		// TODO Contains custom code to force re-resolve if IUBundleContainer removed

		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		ITargetLocation[] containers = fTarget.getTargetLocations();
		if (!selection.isEmpty() && containers != null && containers.length > 0) {
			List<Object> toRemove = new ArrayList<>();
			boolean removedSite = false;
			boolean removedContainer = false;
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				Object currentSelection = iterator.next();
				if (currentSelection instanceof ITargetLocation) {
					if (currentSelection instanceof IUBundleContainer) {
						removedSite = true;
					}
					removedContainer = true;
					toRemove.add(currentSelection);
				}
				if (currentSelection instanceof IUWrapper) {
					toRemove.add(currentSelection);
				}
			}

			if (removedContainer) {
				Set<ITargetLocation> newContainers = new HashSet<>();
				if (fTarget.getTargetLocations() != null) {
					newContainers.addAll(Arrays.asList(fTarget.getTargetLocations()));
				}
				newContainers.removeAll(toRemove);
				if (!newContainers.isEmpty()) {
					fTarget.setTargetLocations(newContainers.toArray(new ITargetLocation[newContainers.size()]));
				} else {
					fTarget.setTargetLocations(null);
				}

				// If we remove a site container, the content change update must force a re-resolve bug 275458 / bug 275401
				// also if the container has errors and has been removed.
				// refresh will refresh the error
				contentsChanged(removedSite || !fTarget.getStatus().isOK());
				fTreeViewer.refresh(false);
				updateButtons();
			} else {
				for (Object object : toRemove) {
					if (object instanceof IUWrapper) {
						((IUWrapper) object).getParent().removeInstallableUnit(((IUWrapper) object).getIU());
					}
				}
				contentsChanged(removedSite);
				fTreeViewer.refresh(true);
				updateButtons();
			}
		}
	}

	private void handleUpdate() {
		// TODO Only IUWrapper children are added to the map for special update processing
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		Map<ITargetLocation, Set<Object>> toUpdate = new HashMap<>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object currentSelection = iterator.next();
			if (currentSelection instanceof ITargetLocation)
				toUpdate.put((ITargetLocation) currentSelection, new HashSet<>(0));
			else if (currentSelection instanceof IUWrapper) {
				IUWrapper wrapper = (IUWrapper) currentSelection;
				Set<Object> iuSet = toUpdate.get(wrapper.getParent());
				if (iuSet == null) {
					iuSet = new HashSet<>();
					iuSet.add(wrapper.getIU().getId());
					toUpdate.put(wrapper.getParent(), iuSet);
				} else if (!iuSet.isEmpty())
					iuSet.add(wrapper.getIU().getId());
			}
		}
		if (toUpdate.isEmpty())
			return;

		JobChangeAdapter listener = new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				UIJob job = new UIJob(Messages.UpdateTargetJob_UpdateJobName) {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						IStatus result = event.getJob().getResult();
						if (!result.isOK()) {
							if (!fTreeViewer.getControl().isDisposed()) {
								ErrorDialog.openError(fTreeViewer.getTree().getShell(), Messages.TargetLocationsGroup_TargetUpdateErrorDialog, result.getMessage(), result);
							}
						} else if (result.getCode() != ITargetLocationUpdater.STATUS_CODE_NO_CHANGE) {
							// Update was successful and changed the target, if dialog/editor still open, update it
							if (!fTreeViewer.getControl().isDisposed()) {
								contentsChanged(true);
								fTreeViewer.refresh(true);
								updateButtons();
							}

							// If the target is the current platform, run a load job for the user
							try {
								ITargetPlatformService service = PDECore.getDefault()
										.acquireService(ITargetPlatformService.class);
								if (service != null) {
									ITargetHandle currentTarget = service.getWorkspaceTargetHandle();
									if (fTarget.getHandle().equals(currentTarget))
										LoadTargetDefinitionJob.load(fTarget);
								}
							} catch (CoreException e) {
								// do nothing if we could not set the current target.
							}
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		};
		UpdateTargetJob.update(fTarget, toUpdate, listener);
	}

	private void updateButtons() {

		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		if (selection.isEmpty()) {
			fRemoveButton.setEnabled(false);
			fUpdateButton.setEnabled(false);
			fEditButton.setEnabled(false);
		}

		boolean canRemove = false;
		boolean canEdit = false;
		boolean canUpdate = false;
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			Object currentSelection = iterator.next();
			if (currentSelection instanceof ITargetLocation) {
				canRemove = true;
				if (!canEdit) {
					ITargetLocation location = (ITargetLocation) currentSelection;
					ITargetLocationEditor editor = Platform.getAdapterManager().getAdapter(location, ITargetLocationEditor.class);
					if (editor != null) {
						canEdit = editor.canEdit(fTarget, location);
					}
					if (location instanceof AbstractBundleContainer) {
						// TODO Custom code for locations that don't use adapters yet
						canEdit = true;
					}
				}
				if (!canUpdate) {
					ITargetLocation location = (ITargetLocation) currentSelection;
					ITargetLocationUpdater updater = Platform.getAdapterManager().getAdapter(location, ITargetLocationUpdater.class);
					if (updater != null) {
						canUpdate = updater.canUpdate(fTarget, location);
					}
				}

			} else if (currentSelection instanceof IUWrapper) {
				// TODO Custom code to support editing/updating/removal of individual IUs
				canRemove = true;
				canEdit = true;
				canUpdate = true;
			}
			if (canRemove && canEdit && canUpdate) {
				break;
			}

		}
		fRemoveButton.setEnabled(canRemove);
		fEditButton.setEnabled(canEdit && fTarget.isResolved());
		fUpdateButton.setEnabled(canUpdate);

		// TODO Some code to find the parent location of items in the tree
		// For each selected item, find it's parent location and add it to the set
//		for (int i = 0; i < treeSelection.length; i++) {
//			TreeItem current = treeSelection[i];
//			while (current != null){
//				if (current instanceof ITargetLocation){
//					selectedLocations.add(current);
//					break;
//				}
//				current = current.getParentItem();
//			}
//		}

	}

	private void handleReload() {

		//delete profile
		try {
			// TODO might want to merge forceCheckTarget into delete Profile?
			P2TargetUtils.forceCheckTarget(fTarget);
			P2TargetUtils.deleteProfile(fTarget.getHandle());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}

		Job job = new UIJob("Reloading...") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				contentsReload();
				return Status.OK_STATUS;
			}
		};
		job.schedule();

	}
	/**
	 * Informs the reporter for this table that something has changed
	 * and is dirty.
	 */
	private void contentsChanged(boolean force) {
		for (ITargetChangedListener listener : fChangeListeners) {
			listener.contentsChanged(fTarget, this, true, force);
		}
	}

	/**
	 * Reloads the target
	 *
	 */
	private void contentsReload() {
		for (ITargetChangedListener listener : fReloadListeners) {
			listener.contentsChanged(fTarget, this, true, true);
		}

	}

}
