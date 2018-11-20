/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
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
import org.eclipse.ui.forms.widgets.*;
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
	class ElementSelectionValidator implements ISelectionStatusValidator {

		private Class<?>[] fAcceptedTypes;
		private boolean fAllowMultipleSelection;

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
				return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", //$NON-NLS-1$
						null);
			}
			return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, "", //$NON-NLS-1$
					null);
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

	class TableContentProvider implements IStructuredContentProvider {
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

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
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
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.TWISTIE, new String[] {PDEUIMessages.BuildEditor_ClasspathSection_add, PDEUIMessages.BuildEditor_ClasspathSection_remove, null, null});
		getSection().setText(PDEUIMessages.BuildEditor_ClasspathSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_ClasspathSection_desc);
		initialize();

	}

	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		if (context == null)
			return null;
		return (IBuildModel) context.getModel();
	}

	public void initialize() {
		getBuildModel().addModelChangedListener(this);
		IBuildEntry entry = getBuildModel().getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
		getSection().setExpanded(entry != null && entry.getTokens().length > 0);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI | SWT.FULL_SELECTION, 2, toolkit);

		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(true);
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

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
	}

	@Override
	public void dispose() {
		IBuildModel model = getBuildModel();
		if (model != null)
			model.removeModelChangedListener(this);
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
						if (tokens.length == 0)
							build.remove(entry);

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
				if (element instanceof IProject) {
					try {
						return ((IProject) element).hasNature(PDE.PLUGIN_NATURE);
					} catch (CoreException e) {
					}
					return false;
				} else if (element instanceof IResource) {
					IBuildModel model = getBuildModel();
					IBuildEntry entry = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
					if (entry != null)
						return !entry.contains(getRelativePathTokenName((IResource) element));
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
				if (tokenName == null)
					continue;
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
			if (!entry.contains(tokenName))
				entry.addToken(tokenName);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private String getRelativePathTokenName(IResource elem) {
		IProject thisProject = getBuildModel().getUnderlyingResource().getProject();
		IProject elemProject = elem.getProject();
		String projectRelative = elem.getProjectRelativePath().toString();
		if (thisProject == elemProject)
			return projectRelative;

		IPluginModelBase model = PluginRegistry.findModel(elemProject);
		if (model != null)
			return "platform:/plugin/" + model.getPluginBase().getId() + '/' + projectRelative; //$NON-NLS-1$
		return null;
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 1 :
				handleDelete();
				break;
			default :
				break;
		}
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED)
			markStale();
		else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object changeObject = event.getChangedObjects()[0];

			if (changeObject instanceof IBuildEntry && ((IBuildEntry) changeObject).getName().equals(IBuildEntry.JARS_EXTRA_CLASSPATH)) {
				Table table = fTableViewer.getTable();
				int index = table.getSelectionIndex();
				fTableViewer.refresh();
				int count = table.getItemCount();
				if (index == -1 || index >= count || event.getOldValue() == null)
					index = count - 1;
				if (count == 0)
					fTableViewer.setSelection(null);
				else
					fTableViewer.setSelection(new StructuredSelection(table.getItem(index).getData()));
				table.setFocus();
			}
		}
	}
}
