/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;
import org.osgi.framework.Constants;

public class ExecutionEnvironmentSection extends TableSection {

	private TableViewer fEETable;
	private Action fRemoveAction;
	private Action fAddAction;

	class EELabelProvider extends LabelProvider {

		private Image fImage;

		public EELabelProvider() {
			fImage = PDEPluginImages.DESC_JAVA_LIB_OBJ.createImage();
		}

		public Image getImage(Object element) {
			return fImage;
		}

		public String getText(Object element) {
			if (element instanceof IExecutionEnvironment)
				return ((IExecutionEnvironment) element).getId();
			return super.getText(element);
		}

		public void dispose() {
			if (fImage != null)
				fImage.dispose();
			super.dispose();
		}
	}

	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IBundleModel) {
				IBundleModel model = (IBundleModel) inputElement;
				IBundle bundle = model.getBundle();
				IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
				if (header instanceof RequiredExecutionEnvironmentHeader) {
					return ((RequiredExecutionEnvironmentHeader) header).getEnvironments();
				}
			}
			return new Object[0];
		}
	}

	public ExecutionEnvironmentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.RequiredExecutionEnvironmentSection_add, PDEUIMessages.RequiredExecutionEnvironmentSection_remove, PDEUIMessages.RequiredExecutionEnvironmentSection_up, PDEUIMessages.RequiredExecutionEnvironmentSection_down});
		createClient(getSection(), page.getEditor().getToolkit());
	}

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
		fEETable.setContentProvider(new ContentProvider());
		fEETable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		Hyperlink link = toolkit.createHyperlink(container, PDEUIMessages.BuildExecutionEnvironmentSection_configure, SWT.NONE);
		link.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		link.addHyperlinkListener(new IHyperlinkListener() {
			public void linkEntered(HyperlinkEvent e) {
			}

			public void linkExited(HyperlinkEvent e) {
			}

			public void linkActivated(HyperlinkEvent e) {
				SWTFactory.showPreferencePage(PDEPlugin.getActiveWorkbenchShell(), "org.eclipse.jdt.debug.ui.jreProfiles", null); //$NON-NLS-1$
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		link.setLayoutData(gd);

		final IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				link = toolkit.createHyperlink(container, PDEUIMessages.ExecutionEnvironmentSection_updateClasspath, SWT.NONE);
				link.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
				link.addHyperlinkListener(new IHyperlinkListener() {
					public void linkEntered(HyperlinkEvent e) {
					}

					public void linkExited(HyperlinkEvent e) {
					}

					public void linkActivated(HyperlinkEvent e) {
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
				});
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

	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
	}

	public void refresh() {
		fEETable.refresh();
		updateButtons();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleAdd();
				break;
			case 1 :
				handleRemove();
				break;
			case 2 :
				handleUp();
				break;
			case 3 :
				handleDown();
				break;
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fAddAction);
		if (!fEETable.getSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fRemoveAction);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void makeActions() {
		fAddAction = new Action(PDEUIMessages.RequiredExecutionEnvironmentSection_add) {
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());

		fRemoveAction = new Action(PDEUIMessages.NewManifestEditor_LibrarySection_remove) {
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
		IStructuredSelection ssel = (IStructuredSelection) fEETable.getSelection();
		if (ssel.size() > 0) {
			Iterator iter = ssel.iterator();
			while (iter.hasNext()) {
				Object object = iter.next();
				if (object instanceof ExecutionEnvironment) {
					getHeader().removeExecutionEnvironment((ExecutionEnvironment) object);
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

	private void addExecutionEnvironments(Object[] result) {
		IManifestHeader header = getHeader();
		if (header == null) {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < result.length; i++) {
				String id = null;
				if (result[i] instanceof IExecutionEnvironment)
					id = ((IExecutionEnvironment) result[i]).getId();
				else if (result[i] instanceof ExecutionEnvironment)
					id = ((ExecutionEnvironment) result[i]).getName();
				else
					continue;
				if (buffer.length() > 0) {
					buffer.append(","); //$NON-NLS-1$
					buffer.append(getLineDelimiter());
					buffer.append(" "); //$NON-NLS-1$
				}
				buffer.append(id);
			}
			getBundle().setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, buffer.toString());
		} else {
			RequiredExecutionEnvironmentHeader ee = (RequiredExecutionEnvironmentHeader) header;
			ee.addExecutionEnvironments(result);
		}
	}

	private String getLineDelimiter() {
		BundleInputContext inputContext = getBundleContext();
		if (inputContext != null) {
			return inputContext.getLineDelimiter();
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	private Object[] getEnvironments() {
		RequiredExecutionEnvironmentHeader header = getHeader();
		IExecutionEnvironment[] envs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		if (header == null)
			return envs;
		ArrayList list = new ArrayList();
		for (int i = 0; i < envs.length; i++) {
			if (!header.hasExecutionEnvironment(envs[i]))
				list.add(envs[i]);
		}
		return list.toArray();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				Table table = fEETable.getTable();
				if (objects[i] instanceof ExecutionEnvironment) {
					int index = table.getSelectionIndex();
					fEETable.remove(objects[i]);
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
		if (bundle == null)
			return null;
		IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header instanceof RequiredExecutionEnvironmentHeader)
			return (RequiredExecutionEnvironmentHeader) header;
		return null;
	}

	protected boolean isFragment() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		IPluginModelBase model = (IPluginModelBase) manager.getAggregateModel();
		return model.isFragmentModel();
	}

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

		return false;
	}

	protected boolean canPaste(Object target, Object[] objects) {
		RequiredExecutionEnvironmentHeader header = getHeader();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ExecutionEnvironment) {
				String env = ((ExecutionEnvironment) objects[i]).getName();
				if (header == null || !header.hasElement(env))
					return true;
			}
		}
		return false;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		if (getPage().getModel().isEditable())
			updateButtons();
	}

	protected void doPaste(Object target, Object[] objects) {
		addExecutionEnvironments(objects);
	}

	private void doFullBuild(final IProject project) {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) {
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}

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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#isDragAndDropEnabled()
	 */
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDragMove(java.lang.Object[])
	 */
	public boolean canDragMove(Object[] sourceObjects) {
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multiple selection not supported
		ExecutionEnvironment sourceEEObject = (ExecutionEnvironment) sourceObjects[0];
		ExecutionEnvironment targetEEObject = (ExecutionEnvironment) targetObject;
		// Validate model
		if (validateDropMoveModel(sourceEEObject, targetEEObject) == false) {
			return false;
		}
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the previous element of the target 
			RequiredExecutionEnvironmentHeader header = getHeader();
			// Ensure we have a header
			if (header == null) {
				return false;
			}
			// Get the previous element of the target
			PDEManifestElement previousElement = header.getPreviousElement(targetEEObject);
			// Ensure the previous element is not the source
			if (sourceEEObject.equals(previousElement)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the next element of the target 
			RequiredExecutionEnvironmentHeader header = getHeader();
			// Ensure we have a header
			if (header == null) {
				return false;
			}
			// Get the next element of the target
			PDEManifestElement nextElement = header.getNextElement(targetEEObject);
			// Ensure the next element is not the source
			if (sourceEEObject.equals(nextElement)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
			return false;
		}
		return false;
	}

	private boolean validateDropMoveModel(ExecutionEnvironment sourceEEObject, ExecutionEnvironment targetEEObject) {
		// Objects have to be from the same model
		IBundleModel sourceModel = sourceEEObject.getModel();
		IBundleModel targetModel = targetEEObject.getModel();
		if (sourceModel.equals(targetModel)) {
			return true;
		}
		return false;
	}

	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof ExecutionEnvironment) == false) {
			return false;
		}
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multiple selection not supported
		ExecutionEnvironment sourceEEObject = (ExecutionEnvironment) sourceObjects[0];
		ExecutionEnvironment targetEEObject = (ExecutionEnvironment) targetObject;
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the header
			RequiredExecutionEnvironmentHeader header = getHeader();
			// Ensure we have a header
			if (header == null) {
				return;
			}
			// Get the index of the target
			int index = header.indexOf(targetEEObject);
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Add source as sibling of target (before)			
			header.addExecutionEnvironment(sourceEEObject, index);
			return;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the header
			RequiredExecutionEnvironmentHeader header = getHeader();
			// Ensure we have a header
			if (header == null) {
				return;
			}
			// Get the index of the target
			int index = header.indexOf(targetEEObject);
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Add source as sibling of target (before)			
			header.addExecutionEnvironment(sourceEEObject, index + 1);
			return;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// NO-OP.  Not supported
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDragRemove(java.lang.Object[])
	 */
	public void doDragRemove(Object[] sourceObjects) {
		// Validate source
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		// Get the source
		ExecutionEnvironment environment = (ExecutionEnvironment) sourceObjects[0];
		// Get the header
		RequiredExecutionEnvironmentHeader header = getHeader();
		// Ensure we have a header
		if (header == null) {
			return;
		}
		// Remove the source
		doSelect(false);
		header.removeExecutionEnvironmentUnique(environment);
		doSelect(true);
	}

	private boolean validateDragMoveSanity(Object[] sourceObjects) {
		// Validate source
		if (sourceObjects == null) {
			// No objects
			return false;
		} else if (sourceObjects.length != 1) {
			// Multiple selection not supported
			return false;
		} else if ((sourceObjects[0] instanceof ExecutionEnvironment) == false) {
			// Must be the right type
			return false;
		}
		return true;
	}

}
