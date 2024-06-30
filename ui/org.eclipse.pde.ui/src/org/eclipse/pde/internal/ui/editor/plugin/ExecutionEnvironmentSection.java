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
 *     Latha Patil (ETAS GmbH) - Issue 1178 Add 'Change' button to change a Plugin's required EE and classpath
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
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
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.text.bundle.ExecutionEnvironment;
import org.eclipse.pde.internal.core.text.bundle.RequiredExecutionEnvironmentHeader;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.eclipse.pde.internal.core.util.VMUtil;
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
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Namespace;

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
		super(page, parent, Section.DESCRIPTION, new String[] { //
				PDEUIMessages.RequiredExecutionEnvironmentSection_modify,
				PDEUIMessages.RequiredExecutionEnvironmentSection_add,
				PDEUIMessages.RequiredExecutionEnvironmentSection_remove });
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
				return getRequiredEEs(model.getBundle()).toArray();
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

		IProject project = getPage().getPDEEditor().getCommonProject();
		if (project != null && PluginProject.isJavaProject(project)) {
			link = toolkit.createHyperlink(container, PDEUIMessages.ExecutionEnvironmentSection_updateClasspath, SWT.NONE);
			link.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> {
				updateClasspathSettings(project);
			}));
			gd = new GridData();
			gd.horizontalSpan = 2;
			link.setLayoutData(gd);
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

	private void updateClasspathSettings(IProject project) {
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
			case 0 -> handleModify();
			case 1 -> handleAdd();
			case 2 -> handleRemove();
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
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(0, isEditable() && table.getItemCount() == 1);
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(2, isEditable() && table.getSelection().length > 0);
	}

	private void handleRemove() {
		IStructuredSelection ssel = fEETable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			for (Object object : ssel) {
				if (object instanceof IExecutionEnvironment ee) {
					getHeader().removeExecutionEnvironment(ee.getId());
				}
			}
		}
	}

	private void handleAdd() {
		Object[] selection = showEESelectionDialog(true);
		if (selection != null) {
			addExecutionEnvironments(selection);
		}
	}

	private void handleModify() {
		RequiredExecutionEnvironmentHeader header = getHeader();
		Object[] selection = showEESelectionDialog(false);
		if (selection != null) {

			List<String> existingEEs = header.getEnvironments();
			// Remove existing EE (Upgrade is allowed only if a single EE is
			// present in the header)
			getHeader().removeExecutionEnvironment(existingEEs.get(0));

			// add selected Java EE to the header
			addExecutionEnvironments(selection);

			IProject project = getPage().getPDEEditor().getCommonProject();
			updateClasspathSettings(project);
		}

	}

	private Object[] showEESelectionDialog(boolean allowMultiSelection) {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				new EELabelProvider()) {
			@Override
			protected FilteredList createFilteredList(Composite parent) {
				FilteredList filteredList = super.createFilteredList(parent);
				filteredList.setComparator(VMUtil.ASCENDING_EE_JAVA_VERSION.reversed());
				return filteredList;
			}
		};
		dialog.setElements(getEnvironments());
		dialog.setAllowDuplicates(false);
		dialog.setMultipleSelection(allowMultiSelection);
		dialog.setTitle(PDEUIMessages.RequiredExecutionEnvironmentSection_dialog_title);
		dialog.setMessage(PDEUIMessages.RequiredExecutionEnvironmentSection_dialogMessage);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(),
				IHelpContextIds.EXECUTION_ENVIRONMENT_SELECTION);
		if (dialog.open() == Window.OK) {
			return dialog.getResult();
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private void addExecutionEnvironments(Object[] result) {
		List<String> ees = Arrays.stream(result).filter(IExecutionEnvironment.class::isInstance)
				.map(IExecutionEnvironment.class::cast).map(IExecutionEnvironment::getId).toList();

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
		IExecutionEnvironmentsManager eeManager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = eeManager.getExecutionEnvironments();
		IBundle bundle = getBundle();
		if (bundle != null) {
			List<IExecutionEnvironment> requiredEEs = getRequiredEEs(bundle).toList();
			if (!requiredEEs.isEmpty()) {
				return Arrays.stream(envs).filter(ee -> !requiredEEs.contains(ee))
						.toArray(IExecutionEnvironment[]::new);
			}
		}
		return envs;
	}

	private Stream<IExecutionEnvironment> getRequiredEEs(IBundle bundle) {
		List<String> requiredEEs = new ArrayList<>(1);
		RequiredExecutionEnvironmentHeader breeHeader = getHeader();
		if (breeHeader != null) {
			requiredEEs.addAll(breeHeader.getEnvironments());
		}
		IManifestHeader requiredCapabilitiesHeader = bundle.getManifestHeader(Constants.REQUIRE_CAPABILITY);
		if (requiredCapabilitiesHeader != null) {
			addRequiredEEs(requiredCapabilitiesHeader, requiredEEs::add);
		}
		return requiredEEs.stream().sorted(VMUtil.ASCENDING_EE_JAVA_VERSION)
				.map(JavaRuntime.getExecutionEnvironmentsManager()::getEnvironment);
	}

	static void addRequiredEEs(IManifestHeader requiredCapabilitiesHeader, Consumer<String> eeCollector) {
		String eeRequirement = requiredCapabilitiesHeader.getValue();
		try {
			ManifestElement[] required = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, eeRequirement);
			for (ManifestElement requiredCapability : required) {
				if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(requiredCapability.getValue())) {
					String filter = requiredCapability.getDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					ManifestUtils.parseRequiredEEsFromFilter(filter, eeCollector);
				}
			}
		} catch (BundleException e) {
			ILog.get().error("Failed to parse " + Constants.REQUIRE_CAPABILITY + " header: " + eeRequirement, e); //$NON-NLS-1$//$NON-NLS-2$
		}
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