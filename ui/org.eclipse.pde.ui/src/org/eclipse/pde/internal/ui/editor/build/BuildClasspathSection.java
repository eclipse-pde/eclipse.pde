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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class BuildClasspathSection extends TableSection {

	private TableViewer fTableViewer;
	private boolean fEnabled = true;

	/**
	 * Implementation of a <code>ISelectionValidator</code> to validate the
	 * type of an element.
	 * Empty selections are not accepted.
	 */
	static class ElementSelectionValidator implements ISelectionStatusValidator {

		private final Class<?>[] fAcceptedTypes;
		private final boolean fAllowMultipleSelection;

		/**
		 * @param acceptedTypes The types accepted by the validator
		 * @param allowMultipleSelection If set to <code>true</code>, the validator
		 * allows multiple selection.
		 */
		public ElementSelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection) {
			Assert.isNotNull(acceptedTypes);
			fAcceptedTypes = acceptedTypes;
			fAllowMultipleSelection = allowMultipleSelection;
		}

		@Override
		public IStatus validate(Object[] elements) {
			if (isValid(elements)) {
				return Status.OK_STATUS;
			}
			return Status.error(""); //$NON-NLS-1$
		}

		private boolean isOfAcceptedType(Object o) {
			for (Class<?> type : fAcceptedTypes) {
				if (type.isInstance(o)) {
					return true;
				}
			}
			return false;
		}

		private boolean isValid(Object[] selection) {
			if (selection.length == 0) {
				return false;
			}

			if (!fAllowMultipleSelection && selection.length != 1) {
				return false;
			}

			for (Object o : selection) {
				if (!isOfAcceptedType(o)) {
					return false;
				}
			}
			return true;
		}
	}

	static class TableContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel) parent).getBuild();
				IBuildEntry entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
				if (entry != null) {
					return entry.getTokens();
				}
			}
			return new Object[0];
		}
	}

	static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
		}
	}

	public BuildClasspathSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.TWISTIE,
				new String[] {
					PDEUIMessages.BuildEditor_ClasspathSection_add,
					PDEUIMessages.BuildEditor_ClasspathSection_remove,
					PDEUIMessages.BuildEditor_ClasspathSection_add_bundle,
					null
				});
		getSection().setText(PDEUIMessages.BuildEditor_ClasspathSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_ClasspathSection_desc);
		initialize();

	}

	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		if (context == null) {
			return null;
		}
		return (IBuildModel) context.getModel();
	}

	public void initialize() {
		getBuildModel().addModelChangedListener(this);
		getSection().setExpanded(true);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI | SWT.FULL_SELECTION, 2, toolkit);

		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(true);

		// -- New Button for Target Platform Browsing DRAFT ---
//		Button browseTargetButton = toolkit.createButton(container, "Browse Target Platform Bundles", SWT.PUSH);
//		browseTargetButton.setLayoutData(new GridData(SWT.RIGHT, SWT.RIGHT, false, false));
//		browseTargetButton.addListener(SWT.Selection, e -> {
//			openTargetPlatformDialog();
//		});

		fTableViewer = tablePart.getTableViewer();

		fTableViewer.setContentProvider(new TableContentProvider());
		fTableViewer.setLabelProvider(new TableLabelProvider());
		fTableViewer.setInput(getBuildModel());

		toolkit.paintBordersFor(container);
		enableSection(true);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);
		data.horizontalSpan = 2;
		section.setClient(container);

	}




	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = fTableViewer.getStructuredSelection();

		// add NEW action
		Action action = new Action(PDEUIMessages.BuildEditor_ClasspathSection_add) {
			@Override
			public void run() {
				handleNew();
			}
		};
		action.setEnabled(fEnabled);
		manager.add(action);

		manager.add(new Separator());

		// add DELETE action
		action = new Action(PDEUIMessages.BuildEditor_ClasspathSection_remove) {
			@Override
			public void run() {
				handleDelete();
			}
		};
		action.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(action);

		manager.add(new Separator());

		// add new bundle action
		action = new Action(PDEUIMessages.BuildEditor_ClasspathSection_add_bundle) {
			@Override
			public void run() {
				handleDelete(); // TODO: change to handleNewBundle
			}
		};
		action.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(action);

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
	}

	@Override
	public void dispose() {
		IBuildModel model = getBuildModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	@Override
	public void refresh() {
		fTableViewer.refresh();
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			if (fEnabled) {
				handleDelete();
			}
			return true;
		}
		return super.doGlobalAction(actionId);
	}

	public void enableSection(boolean enable) {
		fEnabled = enable;
		EditableTablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(1, enable && !fTableViewer.getStructuredSelection().isEmpty());
		tablePart.setButtonEnabled(0, enable);
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		getTablePart().setButtonEnabled(1, selection != null && !selection.isEmpty() && fEnabled);
	}

	private void handleDelete() {
		for (Object selection : fTableViewer.getStructuredSelection().toList()) {
			if (selection != null && selection instanceof String) {
				IBuild build = getBuildModel().getBuild();
				IBuildEntry entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
				if (entry != null) {
					try {
						entry.removeToken(selection.toString());

						String[] tokens = entry.getTokens();
						if (tokens.length == 0) {
							build.remove(entry);
						}

					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}
		}
	}

	private void initializeDialogSettings(ElementTreeSelectionDialog dialog) {
		Class<?>[] acceptedClasses = new Class[] {IFile.class};
		dialog.setValidator(new ElementSelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEUIMessages.BuildEditor_ClasspathSection_jarsTitle);
		dialog.setMessage(PDEUIMessages.BuildEditor_ClasspathSection_jarsDesc);
		dialog.addFilter(new JARFileFilter());
		dialog.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject project) {
					return PluginProject.isPluginProject(project);
				} else if (element instanceof IResource resource) {
					IBuildModel model = getBuildModel();
					IBuildEntry entry = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
					if (entry != null) {
						return !entry.contains(getRelativePathTokenName(resource));
					}
				}
				return true;
			}
		});
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setInitialSelection(getBuildModel().getUnderlyingResource().getProject());

	}

	private void handleNew() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		initializeDialogSettings(dialog);
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			for (Object element : elements) {
				IResource elem = (IResource) element;
				String tokenName = getRelativePathTokenName(elem);
				if (tokenName == null) {
					continue;
				}
				addClasspathToken(tokenName);
			}
		}
	}

	private void addClasspathToken(String tokenName) {
		IBuildModel model = getBuildModel();
		IBuildEntry entry = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
		try {
			if (entry == null) {
				entry = model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
				model.getBuild().add(entry);
			}
			if (!entry.contains(tokenName)) {
				entry.addToken(tokenName);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private String getRelativePathTokenName(IResource elem) {
		IProject thisProject = getBuildModel().getUnderlyingResource().getProject();
		IProject elemProject = elem.getProject();
		String projectRelative = elem.getProjectRelativePath().toString();
		if (thisProject == elemProject) {
			return projectRelative;
		}

		IPluginModelBase model = PluginRegistry.findModel(elemProject);
		if (model != null) {
			return "platform:/plugin/" + model.getPluginBase().getId() + '/' + projectRelative; //$NON-NLS-1$
		}
		return null;
	}

	private void handleNewBundle() {

		// TODO: implement a custom dialog to browse external plug-ins

		PluginSelectionDialog dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				getAvailablePlugins(), true);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IBuildModel model = getBuildModel();
			IBuild build = model.getBuild();
			IBuildEntry entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
			try {
				if (entry == null) {

					entry = model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
					build.add(entry);
				}
				Object[] models = dialog.getResult();
				for (Object m : models) {
					IPluginModel pmodel = (IPluginModel) m;
					String tokenName = "platform:/plugin/" + pmodel.getPlugin().getId() + "/"; // 7
					entry.addToken(tokenName);
				}
				markDirty();
				PDEPreferencesManager store = PDELaunchingPlugin.getDefault().getPreferenceManager();
				store.setDefault(ILaunchingPreferenceConstants.PROP_AUTO_MANAGE, true);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private IPluginModelBase[] getAvailablePlugins() {
		IPluginModelBase[] plugins = PluginRegistry.getActiveModels(false);
		HashSet<String> currentPlugins = new HashSet<>();
		IProject currentProj = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = PluginRegistry.findModel(currentProj);
		if (model != null) {
			currentPlugins.add(model.getPluginBase().getId());
			if (model.isFragmentModel()) {
				currentPlugins.add(((IFragmentModel) model).getFragment().getPluginId());
			}
		}

		ArrayList<IPluginModelBase> result = new ArrayList<>();
		for (int i = 0; i < plugins.length; i++) {
			if (!currentPlugins.contains(plugins[i].getPluginBase().getId())) {
				result.add(plugins[i]);
			}
		}
		return result.toArray(new IPluginModelBase[result.size()]);
	}


	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 -> handleNew();
			case 1 -> handleDelete();
			case 2 -> handleNewBundle();
		}
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object changeObject = event.getChangedObjects()[0];

			if (changeObject instanceof IBuildEntry && ((IBuildEntry) changeObject).getName().equals(IBuildEntry.JARS_EXTRA_CLASSPATH)) {
				Table table = fTableViewer.getTable();
				int index = table.getSelectionIndex();
				fTableViewer.refresh();
				int count = table.getItemCount();
				if (index == -1 || index >= count || event.getOldValue() == null) {
					index = count - 1;
				}
				if (count == 0) {
					fTableViewer.setSelection(null);
				} else {
					fTableViewer.setSelection(new StructuredSelection(table.getItem(index).getData()));
				}
				table.setFocus();
			}
		}
	}
}
