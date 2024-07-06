/*******************************************************************************
 *  Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.Constants;

public class ExecutionEnvironmentSection extends TableSection {

	private TableViewer fEETable;
	private Action fRemoveAction;
	private Action fAddAction;

	private static class EELabelProvider extends LabelProvider {

		private final Image fImage;

		public EELabelProvider() {
			fImage = PDEPluginImages.DESC_JAVA_LIB_OBJ.createImage();
		}

		@Override
		public Image getImage(Object element) {
			return fImage;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IExecutionEnvironment)
				return ((IExecutionEnvironment) element).getId();
			return super.getText(element);
		}

		@Override
		public void dispose() {
			if (fImage != null)
				fImage.dispose();
			super.dispose();
		}
	}

	public ExecutionEnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.RequiredExecutionEnvironmentSection_add, PDEUIMessages.RequiredExecutionEnvironmentSection_remove, PDEUIMessages.RequiredExecutionEnvironmentSection_up, PDEUIMessages.RequiredExecutionEnvironmentSection_down});
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.RequiredExecutionEnvironmentSection_title);
		if (isFragment())
			section.setDescription(PDEUIMessages.RequiredExecutionEnvironmentSection_fragmentDesc);
		else
			section.setDescription(PDEUIMessages.RequiredExecutionEnvironmentSection_pluginDesc);

		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));

		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION | SWT.MULTI, 2, toolkit);
		fEETable = tablePart.getTableViewer();
		fEETable.setContentProvider((IStructuredContentProvider) inputElement -> {
			if (inputElement instanceof IBundleModel model) {
				IBundle bundle = model.getBundle();
				@SuppressWarnings("deprecation")
				IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
				if (header instanceof RequiredExecutionEnvironmentHeader breeHeader) {
					return breeHeader.getElements();
				}
			}
			return new Object[0];
		});
		fEETable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		Hyperlink link = toolkit.createHyperlink(container, PDEUIMessages.BuildExecutionEnvironmentSection_configure, SWT.NONE);
		link.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> {
			Shell shell = PDEPlugin.getActiveWorkbenchShell();
			SWTFactory.showPreferencePage(shell, "org.eclipse.jdt.debug.ui.jreProfiles", null); //$NON-NLS-1$
		}));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		link.setLayoutData(gd);

		final IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				link = toolkit.createHyperlink(container, PDEUIMessages.ExecutionEnvironmentSection_updateClasspath, SWT.NONE);
				link.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> {
					try {
						getPage().getEditor().doSave(null);
						IPluginModelBase model = PluginRegistry.findModel(project);
						if (model != null) {
							ClasspathComputer.setClasspath(project, model);
							if (PDEPlugin.getWorkspace().isAutoBuilding()) {
								doFullBuild(project);
							}
						}
					} catch (CoreException e1) {
					}
				}));
				gd = new GridData();
				gd.horizontalSpan = 2;
				link.setLayoutData(gd);
			}
		} catch (CoreException e1) {
		}

		makeActions();

		IBundleModel model = getBundleModel();
		if (model != null) {
			fEETable.setInput(model);
			model.addModelChangedListener(this);
		}
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	@Override
	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
	}

	@Override
	public void refresh() {
		fEETable.refresh();
		updateButtons();
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 -> handleAdd();
			case 1 -> handleRemove();
			case 2 -> handleUp();
			case 3 -> handleDown();
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fAddAction);
		if (!fEETable.getStructuredSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fRemoveAction);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void makeActions() {
		fAddAction = new Action(PDEUIMessages.RequiredExecutionEnvironmentSection_add) {
			@Override
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());

		fRemoveAction = new Action(PDEUIMessages.NewManifestEditor_LibrarySection_remove) {
			@Override
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());
	}

	private void updateButtons() {
		Table table = fEETable.getTable();
		int count = table.getItemCount();
		boolean canMoveUp = count > 0 && table.getSelection().length == 1 && table.getSelectionIndex() > 0;
		boolean canMoveDown = count > 0 && table.getSelection().length == 1 && table.getSelectionIndex() < count - 1;

		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable() && table.getSelection().length > 0);
		tablePart.setButtonEnabled(2, isEditable() && canMoveUp);
		tablePart.setButtonEnabled(3, isEditable() && canMoveDown);
	}

	private void handleDown() {
		int selection = fEETable.getTable().getSelectionIndex();
		swap(selection, selection + 1);
	}

	private void handleUp() {
		int selection = fEETable.getTable().getSelectionIndex();
		swap(selection, selection - 1);
	}

	public void swap(int index1, int index2) {
		RequiredExecutionEnvironmentHeader header = getHeader();
		header.swap(index1, index2);
	}

	private void handleRemove() {
		IStructuredSelection ssel = fEETable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			for (Object object : ssel) {
				if (object instanceof ExecutionEnvironment ee) {
					getHeader().removeExecutionEnvironment(ee.getName());
				}
			}
		}
	}

	private void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new EELabelProvider());
		dialog.setElements(getEnvironments());
		dialog.setAllowDuplicates(false);
		dialog.setMultipleSelection(true);
		dialog.setTitle(PDEUIMessages.RequiredExecutionEnvironmentSection_dialog_title);
		dialog.setMessage(PDEUIMessages.RequiredExecutionEnvironmentSection_dialogMessage);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.EXECUTION_ENVIRONMENT_SELECTION);
		if (dialog.open() == Window.OK) {
			addExecutionEnvironments(dialog.getResult());
		}
	}

	@SuppressWarnings("deprecation")
	private void addExecutionEnvironments(Object[] result) {
		List<String> ees = Arrays.stream(result).map(resultObject -> {
			if (resultObject instanceof IExecutionEnvironment ee) {
				return ee.getId();
			} else if (resultObject instanceof ExecutionEnvironment ee) {
				return ee.getName();
			}
			return null;
		}).filter(Objects::nonNull).toList();

		IManifestHeader header = getHeader();
		if (header == null) {
			String eeList = String.join("," + getLineDelimiter() + " ", ees); //$NON-NLS-1$//$NON-NLS-2$
			getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, eeList);
		} else {
			RequiredExecutionEnvironmentHeader ee = (RequiredExecutionEnvironmentHeader) header;
			ee.addExecutionEnvironments(ees);
		}
	}

	private String getLineDelimiter() {
		BundleInputContext inputContext = getBundleContext();
		if (inputContext != null) {
			return inputContext.getLineDelimiter();
		}
		return TextUtil.getDefaultLineDelimiter();
	}

	private IExecutionEnvironment[] getEnvironments() {
		RequiredExecutionEnvironmentHeader header = getHeader();
		IExecutionEnvironmentsManager eeManager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = eeManager.getExecutionEnvironments();
		if (header == null) {
			return envs;
		}
		List<IExecutionEnvironment> ees = header.getElementNames().stream().map(eeManager::getEnvironment).toList();
		return Arrays.stream(envs).filter(ee -> !ees.contains(ee)).toArray(IExecutionEnvironment[]::new);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (Object object : objects) {
				Table table = fEETable.getTable();
				if (object instanceof ExecutionEnvironment) {
					int index = table.getSelectionIndex();
					fEETable.remove(object);
					if (canSelect()) {
						table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
					}
				}
			}
			updateButtons();
		} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
			Object[] objects = e.getChangedObjects();
			if (objects.length > 0) {
				fEETable.refresh();
				fEETable.setSelection(new StructuredSelection(objects[objects.length - 1]));
			}
			updateButtons();
		} else if (Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.equals(e.getChangedProperty())) {
			refresh();
			// Bug 171896
			// Since the model sends a CHANGE event instead of
			// an INSERT event on the very first addition to the empty table
			// Selection should fire here to take this first insertion into account
			Object lastElement = fEETable.getElementAt(fEETable.getTable().getItemCount() - 1);
			if (lastElement != null) {
				fEETable.setSelection(new StructuredSelection(lastElement));
			}
		}
	}

	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}

	private IBundle getBundle() {
		IBundleModel model = getBundleModel();
		return model == null ? null : model.getBundle();
	}

	private IBundleModel getBundleModel() {
		BundleInputContext context = getBundleContext();
		return context == null ? null : (IBundleModel) context.getModel();
	}

	protected RequiredExecutionEnvironmentHeader getHeader() {
		IBundle bundle = getBundle();
		if (bundle == null) {
			return null;
		}
		@SuppressWarnings("deprecation")
		IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		return header instanceof RequiredExecutionEnvironmentHeader breeHeader ? breeHeader : null;
	}

	protected boolean isFragment() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		if (manager.getAggregateModel() instanceof IPluginModelBase model) {
			return model.isFragmentModel();
		}
		return false;
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}

		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleRemove();
			return false;
		}

		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}

		return super.doGlobalAction(actionId);
	}

	@Override
	protected boolean canPaste(Object target, Object[] objects) {
		RequiredExecutionEnvironmentHeader header = getHeader();
		for (Object object : objects) {
			if (object instanceof ExecutionEnvironment executionEnvironment) {
				String env = executionEnvironment.getName();
				if (header == null || !header.hasElement(env)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		if (getPage().getModel().isEditable()) {
			updateButtons();
		}
	}

	@Override
	protected void doPaste(Object target, Object[] objects) {
		addExecutionEnvironments(objects);
	}

	private void doFullBuild(final IProject project) {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) {
			@Override
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.build(IncrementalProjectBuilder.FULL_BUILD, JavaCore.BUILDER_ID, null, monitor);
				} catch (CoreException e) {
				}
				return Status.OK_STATUS;
			}
		};
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.schedule();
	}
}
