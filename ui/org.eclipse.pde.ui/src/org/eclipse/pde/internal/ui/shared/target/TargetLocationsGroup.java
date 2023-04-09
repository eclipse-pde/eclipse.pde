/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich 	[Bug 567506] - TargetLocationsGroup.handleEdit() should activate bundles if necessary
 *     						[Bug 568865] - add advanced editing capabilities for custom target platforms
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.progress.UIJob;

/**
 * UI part that can be added to a dialog or to a form editor. Contains a table
 * displaying the bundle containers of a target definition. Also has buttons to
 * add, edit and remove bundle containers of varying types.
 *
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see ITargetLocation
 */
public class TargetLocationsGroup {

	private static final String BUTTON_STATE = "ButtonState"; //$NON-NLS-1$

	private enum DeleteButtonState {
		NONE, REMOVE, ENABLE, DISABLE, TOGGLE;

		static DeleteButtonState computeState(boolean canRemove, boolean canEnable, boolean canDisable) {
			if (canRemove) {
				if (canEnable || canDisable) {
					// a mixture of actions is currently selected
					return NONE;
				}
				return REMOVE;
			}
			if (canEnable) {
				if (canDisable) {
					return TOGGLE;
				}
				return ENABLE;
			} else if (canDisable) {
				return DISABLE;
			}
			return NONE;
		}
	}

	private TreeViewer fTreeViewer;
	private Action fCopySelectionAction;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fUpdateButton;
	private Button fRefreshButton;
	private Button fExpandCollapseButton;
	private Button fShowContentButton;

	private ITargetDefinition fTarget;
	private ListenerList<ITargetChangedListener> fChangeListeners = new ListenerList<>();
	private ListenerList<ITargetChangedListener> fReloadListeners = new ListenerList<>();
	private static final TargetLocationHandlerAdapter ADAPTER = new TargetLocationHandlerAdapter();

	/**
	 * Creates this part using the form toolkit and adds it to the given
	 * composite.
	 *
	 * @param parent
	 *            parent composite
	 * @param toolkit
	 *            toolkit to create the widgets with
	 * @return generated instance of the table part
	 */
	public static TargetLocationsGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetLocationsGroup contentTable = new TargetLocationsGroup();
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given
	 * composite.
	 *
	 * @param parent
	 *            parent composite
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
	 * Adds a listener to the set of listeners that will be notified when the
	 * bundle containers are modified. This method has no effect if the listener
	 * has already been added.
	 *
	 * @param listener
	 *            target changed listener to add
	 */
	public void addTargetChangedListener(ITargetChangedListener listener) {
		fChangeListeners.add(listener);
	}

	/**
	 * Adds a listener to the set of listeners that will be notified when target
	 * is reloaded. This method has no effect if the listener has already been
	 * added.
	 *
	 * @param listener
	 *            target changed listener to add
	 */
	public void addTargetReloadListener(ITargetChangedListener listener) {
		fReloadListeners.add(listener);
	}

	/**
	 * Creates the part contents from a toolkit
	 *
	 * @param parent
	 *            parent composite
	 * @param toolkit
	 *            form toolkit to create widgets
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

		fAddButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Add, SWT.PUSH);
		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Edit, SWT.PUSH);
		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Remove, SWT.PUSH);
		fUpdateButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Update, SWT.PUSH);
		fUpdateButton.setToolTipText(Messages.TargetLocationsGroup_update);
		fRefreshButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Refresh, SWT.PUSH);
		fRefreshButton.setToolTipText(Messages.TargetLocationsGroup_refresh);
		fExpandCollapseButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_Btn_Text_ExpandAll,
				SWT.PUSH);

		fShowContentButton = toolkit.createButton(comp, Messages.TargetLocationsGroup_1, SWT.CHECK);

		initializeTreeViewer(atree);
		initializeButtons();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 *
	 * @param parent
	 *            parent composite
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

		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Add, null);
		fEditButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Edit, null);
		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Remove, null);
		fUpdateButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Update, null);
		fRefreshButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_Btn_Text_Refresh, null);
		fExpandCollapseButton = SWTFactory.createPushButton(buttonComp,
				Messages.BundleContainerTable_Btn_Text_ExpandAll, null);

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
		fTreeViewer.setLabelProvider(new StyledBundleLabelProvider(true, false));
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
		fTreeViewer.getTree().addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				setExpandCollapseState();
			}

			@Override
			public void mouseDown(MouseEvent e) {
			}

			@Override
			public void mouseUp(MouseEvent e) {
				setExpandCollapseState();
			}

		});
		fTreeViewer.getTree().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				setExpandCollapseState();
			}

		});
	}

	private void setExpandCollapseState() {
		if (fTreeViewer == null)
			return;
		if (fTreeViewer.getVisibleExpandedElements().length == 0) {
			fExpandCollapseButton.setText(Messages.BundleContainerTable_Btn_Text_ExpandAll);
		} else {
			fExpandCollapseButton.setText(Messages.BundleContainerTable_Btn_Text_CollapseAll);
		}

	}

	private void createContextMenu(Tree tree) {
		fCopySelectionAction = new CopyTreeSelectionAction(tree);

		MenuManager menuManager = new MenuManager();
		menuManager.add(fCopySelectionAction);

		Menu menu = menuManager.createContextMenu(tree);
		tree.setMenu(menu);
	}

	/**
	 * Sets up the buttons, the button fields must already be created before
	 * calling this method
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

		fRefreshButton.addSelectionListener(widgetSelectedAdapter(e -> handleReload()));
		fRefreshButton.setLayoutData(new GridData());
		fRefreshButton.setEnabled(true);
		SWTFactory.setButtonDimensionHint(fRefreshButton);

		fExpandCollapseButton.addSelectionListener(widgetSelectedAdapter(e -> toggleCollapse()));
		fExpandCollapseButton.setLayoutData(new GridData());
		fExpandCollapseButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fExpandCollapseButton);

		fShowContentButton.addSelectionListener(widgetSelectedAdapter(e -> {
			((TargetLocationContentProvider) fTreeViewer.getContentProvider())
					.setShowLocationContent(fShowContentButton.getSelection());
			fTreeViewer.refresh();
			fTreeViewer.expandAll();
			fExpandCollapseButton.setText(Messages.BundleContainerTable_Btn_Text_CollapseAll);
		}));
		fShowContentButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fShowContentButton);
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be
	 * called with different models to change the tree's input.
	 *
	 * @param target
	 *            target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		boolean isCollapsed = fTreeViewer.getVisibleExpandedElements().length == 0;
		fTreeViewer.setInput(fTarget);
		if (isCollapsed)
			fTreeViewer.collapseAll();
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
		ITreeSelection selection = fTreeViewer.getStructuredSelection();
		TreePath[] paths = selection.getPaths();
		if (paths.length == 1) {
			IWizard editWizard = ADAPTER.getEditWizard(fTarget, paths[0]);
			if (editWizard != null) {
				Shell parent = fTreeViewer.getTree().getShell();
				WizardDialog wizard = new WizardDialog(parent, editWizard);
				if (wizard.open() == Window.OK) {
					updateXML();
					contentsChanged(false);
					fTreeViewer.refresh();
					updateButtons();
				}
			}
		}
	}

	private void updateXML() {
		fTarget.setTargetLocations(fTarget.getTargetLocations());
	}

	private void handleRemove() {
		ITreeSelection selection = fTreeViewer.getStructuredSelection();
		DeleteButtonState state = (DeleteButtonState) Objects.requireNonNullElse(fRemoveButton.getData(BUTTON_STATE),
				DeleteButtonState.NONE);
		if (selection.isEmpty() || state == DeleteButtonState.NONE) {
			fRemoveButton.setEnabled(false);
			return;
		}
		IStatus tstatus = fTarget.getStatus();
		IStatus status;
		if (state == DeleteButtonState.REMOVE) {
			status = log(ADAPTER.remove(fTarget, selection.getPaths()));
		} else {
			status = log(ADAPTER.toggle(fTarget, selection.getPaths()));
		}
		boolean forceReload = (tstatus != null && !tstatus.isOK())
				|| (status != null && status.isOK() && status.getCode() == ITargetLocationHandler.STATUS_FORCE_RELOAD);
		updateXML();
		contentsChanged(forceReload);
		fTreeViewer.refresh();
		updateButtons();
	}

	private void handleUpdate() {
		ITreeSelection selection = fTreeViewer.getStructuredSelection();
		if (selection.isEmpty()) {
			fUpdateButton.setEnabled(false);
			return;
		}
		List<IJobFunction> updateActions = Collections
				.singletonList(monitor -> log(ADAPTER.update(fTarget, selection.getPaths(), monitor)));
		JobChangeAdapter listener = new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				UIJob job = UIJob.create(Messages.UpdateTargetJob_UpdateJobName, monitor -> {
					IStatus result = event.getJob().getResult();
					if (!result.isOK()) {
						if (!fTreeViewer.getControl().isDisposed()) {
							ErrorDialog.openError(fTreeViewer.getTree().getShell(),
									Messages.TargetLocationsGroup_TargetUpdateErrorDialog, result.getMessage(), result);
						}
					} else if (result.getCode() != ITargetLocationHandler.STATUS_CODE_NO_CHANGE) {
						// Update was successful and changed the target, if
						// dialog/editor still open, update it
						if (!fTreeViewer.getControl().isDisposed()) {
							contentsChanged(true);
							fTreeViewer.refresh(true);
							updateButtons();
						}

						// If the target is the current platform, run a load
						// job for the user
						try {
							ITargetPlatformService service = PDECore.getDefault()
									.acquireService(ITargetPlatformService.class);
							if (service != null) {
								ITargetHandle currentTarget = service.getWorkspaceTargetHandle();
								if (fTarget.getHandle().equals(currentTarget))
									LoadTargetDefinitionJob.load(fTarget);
							}
						} catch (CoreException e) {
							// do nothing if we could not set the current
							// target.
						}
					}
				});
				job.schedule();
			}
		};
		UpdateTargetJob.update(updateActions, listener);
	}

	private void updateButtons() {

		ITreeSelection selection = fTreeViewer.getStructuredSelection();
		if (selection.isEmpty()) {
			fRemoveButton.setEnabled(false);
			fRemoveButton.setText(Messages.BundleContainerTable_Btn_Text_Remove);
			fRemoveButton.setData(BUTTON_STATE, DeleteButtonState.NONE);
			fUpdateButton.setEnabled(false);
			fEditButton.setEnabled(false);
			if (fTreeViewer != null) {
				setExpandCollapseState();
			}
			return;
		}
		boolean canRemove = false;
		boolean canEdit = false;
		boolean canUpdate = false;
		boolean canEnable = false;
		boolean canDisable = false;

		TreePath[] paths = selection.getPaths();
		for (TreePath path : paths) {
			canRemove |= ADAPTER.canRemove(fTarget, path);
			canDisable |= ADAPTER.canDisable(fTarget, path);
			canEnable |= ADAPTER.canEnable(fTarget, path);
			canUpdate |= ADAPTER.canUpdate(fTarget, path);
			canEdit = paths.length == 1 && ADAPTER.canEdit(fTarget, path);
		}
		fEditButton.setEnabled(canEdit);
		fUpdateButton.setEnabled(canUpdate);
		DeleteButtonState state = DeleteButtonState.computeState(canRemove, canEnable, canDisable);
		switch (state)
			{
			case DISABLE -> fRemoveButton.setText(Messages.BundleContainerTable_Btn_Text_Disable);
			case ENABLE -> fRemoveButton.setText(Messages.BundleContainerTable_Btn_Text_Enable);
			case TOGGLE -> fRemoveButton.setText(Messages.BundleContainerTable_Btn_Text_Toggle);
			default -> fRemoveButton.setText(Messages.BundleContainerTable_Btn_Text_Remove);
			};
		fRemoveButton.setEnabled(state != DeleteButtonState.NONE);
		fRemoveButton.setData(BUTTON_STATE, state);
	}

	private void handleReload() {
		log(ADAPTER.reload(fTarget, fTarget.getTargetLocations(), new NullProgressMonitor()));
		Job job = UIJob.create("Refreshing...", (ICoreRunnable) monitor -> contentsReload()); //$NON-NLS-1$
		job.schedule();

	}

	private void toggleCollapse() {
		if (fTreeViewer == null)
			return;
		if (fTreeViewer.getVisibleExpandedElements().length == 0) {
			fTreeViewer.expandAll();
			fExpandCollapseButton.setText(Messages.BundleContainerTable_Btn_Text_CollapseAll);
		} else {
			fTreeViewer.collapseAll();
			fExpandCollapseButton.setText(Messages.BundleContainerTable_Btn_Text_ExpandAll);
		}

	}

	/**
	 * Informs the reporter for this table that something has changed and is
	 * dirty.
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

	private static IStatus log(IStatus status) {
		if (status != null && !status.isOK()) {
			PDEPlugin.log(status);
		}
		return status;
	}

	public void setExpandCollapseState(boolean b) {
		if (fExpandCollapseButton != null)
			fExpandCollapseButton.setEnabled(b);
	}

}
