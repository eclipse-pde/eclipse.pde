/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Anyware Technologies - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.ByteArrayInputStream;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.AddNewDependenciesAction;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.FileEditorInput;
import org.osgi.service.prefs.BackingStoreException;

public class DependencyManagementSection extends TableSection implements IModelChangedListener, IPluginModelListener, IPropertyChangeListener {

	private TableViewer fAdditionalTable;
	private Vector fAdditionalBundles;
	private Action fNewAction;
	private Action fRemoveAction;
	private Action fOpenAction;
	private Action fSortAction;
	private Button fRequireBundleButton;
	private Button fImportPackageButton;
	private IProject fProject;

	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int UP_INDEX = 2;
	private static final int DOWN_INDEX = 3;

	private static String ADD = PDEUIMessages.RequiresSection_add;
	private static String REMOVE = PDEUIMessages.RequiresSection_delete;
	private static String OPEN = PDEUIMessages.RequiresSection_open;
	private static String UP = PDEUIMessages.RequiresSection_up;
	private static String DOWN = PDEUIMessages.RequiresSection_down;

	class ContentProvider extends DefaultTableProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (fAdditionalBundles == null)
				return createAdditionalBundles();
			return fAdditionalBundles.toArray();
		}

		private IBuildEntry getBuildInfo() {
			IBuildEntry entry = null;
			IBuildModel model = getBuildModel(false);
			if (model == null)
				return null;
			IBuild buildObject = model.getBuild();
			entry = buildObject.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			return entry;
		}

		private Object[] createAdditionalBundles() {
			IBuildEntry entry = getBuildInfo();
			try {
				if (entry != null) {
					String[] tokens = entry.getTokens();
					fAdditionalBundles = new Vector(tokens.length);
					for (int i = 0; i < tokens.length; i++) {
						fAdditionalBundles.add(tokens[i].trim());
					}
					return fAdditionalBundles.toArray();
				}
				return new Object[0];
			} catch (Exception e) {
				PDEPlugin.logException(e);
				return new Object[0]; //If exception happen while getting bundles, return an empty table
			}
		}
	}

	class SecondaryTableLabelProvider extends SharedLabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			String pluginID = obj.toString();
			IPluginModelBase model = PluginRegistry.findModel(pluginID);
			if (model == null) {
				return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, F_ERROR);
			} else if (model instanceof IBundlePluginModel || model instanceof WorkspacePluginModel) {
				return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
			} else if (model instanceof ExternalPluginModel) {
				return get(PDEPluginImages.DESC_PLUGIN_OBJ, F_EXTERNAL);
			}
			return null;
		}
	}

	public DependencyManagementSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, ExpandableComposite.TWISTIE | ExpandableComposite.COMPACT, new String[] {ADD, REMOVE, UP, DOWN});
		IBuildModel model = getBuildModel(false);
		if (model != null) {
			IBuildEntry entry = model.getBuild().getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			if (entry != null && entry.getTokens().length > 0)
				getSection().setExpanded(true);
		}
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		FormText text = toolkit.createFormText(section, true);
		text.setText(PDEUIMessages.SecondaryBundlesSection_desc, false, false);
		section.setDescriptionControl(text);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fAdditionalTable = tablePart.getTableViewer();

		fAdditionalTable.setContentProvider(new ContentProvider());
		fAdditionalTable.setLabelProvider(new SecondaryTableLabelProvider());
		GridData gd = (GridData) fAdditionalTable.getTable().getLayoutData();
		gd.heightHint = 150;
		fAdditionalTable.getTable().setLayoutData(gd);

		gd = new GridData();
		gd.horizontalSpan = 2;
		FormText resolveText = toolkit.createFormText(container, true);
		resolveText.setText(PDEUIMessages.SecondaryBundlesSection_resolve, true, true);
		resolveText.setLayoutData(gd);
		resolveText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				doAddDependencies();
			}
		});

		Composite comp = toolkit.createComposite(container);
		comp.setLayout(new GridLayout(2, false));
		gd = new GridData();
		gd.horizontalSpan = 2;
		comp.setLayoutData(gd);

		fRequireBundleButton = toolkit.createButton(comp, "Require-Bundle", SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fRequireBundleButton.setLayoutData(gd);
		fRequireBundleButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				savePreferences();
			}
		});

		fImportPackageButton = toolkit.createButton(comp, "Import-Package", SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fImportPackageButton.setLayoutData(gd);

		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		section.setText(PDEUIMessages.SecondaryBundlesSection_title);
		createSectionToolbar(section, toolkit);
		initialize();
	}

	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});

		// Add sort action to the tool bar
		fSortAction = new SortAction(getTablePart().getTableViewer(), PDEUIMessages.RequiresSection_sortAlpha, null, null, this);
		toolBarManager.add(fSortAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	private void savePreferences() {
		if (fProject == null) {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IResource resource = model.getUnderlyingResource();
			if (resource == null)
				return;
			fProject = resource.getProject();
		}
		IEclipsePreferences pref = new ProjectScope(fProject).getNode(PDECore.PLUGIN_ID);

		if (fImportPackageButton.getSelection())
			pref.putBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, false);
		else
			pref.remove(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void initialize() {
		try {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			fAdditionalTable.setInput(model.getPluginBase());
			TablePart part = getTablePart();
			part.setButtonEnabled(0, model.isEditable());
			part.setButtonEnabled(1, false);
			part.setButtonEnabled(2, false);
			part.setButtonEnabled(3, false);

			IBuildModel build = getBuildModel(false);
			if (build != null)
				build.addModelChangedListener(this);

			IResource resource = model.getUnderlyingResource();
			if (resource == null)
				return;
			fProject = resource.getProject();
			IEclipsePreferences pref = new ProjectScope(fProject).getNode(PDECore.PLUGIN_ID);
			if (pref != null) {
				boolean useRequireBundle = pref.getBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, true);
				fRequireBundleButton.setSelection(useRequireBundle);
				fImportPackageButton.setSelection(!useRequireBundle);
			}
			PDECore.getDefault().getModelManager().addPluginModelListener(this);
		} catch (Exception e) {
			PDEPlugin.logException(e);
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fAdditionalTable.getSelection();
		manager.add(fNewAction);
		manager.add(fOpenAction);
		manager.add(new Separator());

		if (!selection.isEmpty())
			manager.add(fRemoveAction);

		// Add clipboard operations
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	public void refresh() {
		fAdditionalBundles = null;
		if (!fAdditionalTable.getControl().isDisposed())
			fAdditionalTable.refresh();
		super.refresh();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case ADD_INDEX :
				handleNew();
				break;
			case REMOVE_INDEX :
				handleRemove();
				break;
			case UP_INDEX :
				handleUp();
				break;
			case DOWN_INDEX :
				handleDown();
				break;
		}
	}

	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	private void handleOpen(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			if (ssel.size() == 1) {
				Object obj = ssel.getFirstElement();
				IPluginModelBase base = PluginRegistry.findModel((String) obj);
				if (base != null)
					ManifestEditor.open(base.getPluginBase(), false);
			}
		}
	}

	private IBuildModel getBuildModel(boolean createIfMissing) {
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		if (context == null) {
			if (createIfMissing) {
				IFile buildFile = PDEProject.getBuildProperties(getPage().getPDEEditor().getCommonProject());
				try {
					buildFile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
				} catch (CoreException e) {
					return null;
				}
				FileEditorInput in = new FileEditorInput(buildFile);
				PDEFormEditor editor = getPage().getPDEEditor();
				context = new BuildInputContext(getPage().getPDEEditor(), in, false);
				editor.getContextManager().putContext(in, context);
			} else
				return null;
		}
		return (IBuildModel) context.getModel();
	}

	private void makeActions() {
		fNewAction = new Action(ADD) {
			public void run() {
				handleNew();
			}
		};

		fOpenAction = new Action(OPEN) {
			public void run() {
				handleOpen(fAdditionalTable.getSelection());
			}
		};

		fRemoveAction = new Action(REMOVE) {
			public void run() {
				handleRemove();
			}
		};
	}

	private void handleNew() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getAvailablePlugins(), true);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IBuildModel model = getBuildModel(true);
			IBuild build = model.getBuild();
			IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			try {
				if (entry == null) {
					entry = model.getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
					build.add(entry);
				}
				Object[] models = dialog.getResult();

				for (int i = 0; i < models.length; i++) {
					IPluginModel pmodel = (IPluginModel) models[i];
					entry.addToken(pmodel.getPlugin().getId());
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
		HashSet currentPlugins = new HashSet((fAdditionalBundles == null) ? new Vector(1) : fAdditionalBundles);
		IProject currentProj = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = PluginRegistry.findModel(currentProj);
		if (model != null) {
			currentPlugins.add(model.getPluginBase().getId());
			if (model.isFragmentModel()) {
				currentPlugins.add(((IFragmentModel) model).getFragment().getPluginId());
			}
		}

		ArrayList result = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			if (!currentPlugins.contains(plugins[i].getPluginBase().getId()))
				result.add(plugins[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fAdditionalTable.getSelection();

		IBuild build = getBuildModel(false).getBuild();
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		Iterator it = ssel.iterator();
		try {
			while (it.hasNext()) {
				String pluginName = (String) it.next();
				entry.removeToken(pluginName);
			}
			if (entry.getTokens().length == 0)
				build.remove(entry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		refresh();
		markDirty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TableSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection sel) {
		// Update global selection
		getPage().getPDEEditor().setSelection(sel);
		updateButtons();
	}

	private void updateButtons() {
		TablePart part = getTablePart();
		Table table = fAdditionalTable.getTable();
		int index = table.getSelectionIndex();
		part.setButtonEnabled(1, index != -1);
		updateUpDownButtons();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changedObject = event.getChangedObjects()[0];
		if ((changedObject instanceof IBuildEntry && ((IBuildEntry) changedObject).getName().equals(IBuildEntry.SECONDARY_DEPENDENCIES))) {
			refresh();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		HashSet secondaryDepSet = null;
		// Only String objects representing non-duplicate secondary 
		// dependencies can be pasted
		for (int i = 0; i < sourceObjects.length; i++) {
			// Only String objects are allowed
			if ((sourceObjects[i] instanceof String) == false) {
				return false;
			}
			// Get the current secondary dependencies and store them to 
			// assist in searching
			if (secondaryDepSet == null) {
				secondaryDepSet = createSecondaryDepSet();
			}
			// No duplicate secondary dependencies allowed
			String secondaryDep = (String) sourceObjects[i];
			if (secondaryDepSet.contains(secondaryDep)) {
				return false;
			}
		}
		return true;
	}

	private HashSet createSecondaryDepSet() {
		HashSet secondaryDepSet = new HashSet();
		// Get the build model
		IBuildModel buildModel = getBuildModel(true);
		// Ensure the build model is defined
		if (buildModel == null) {
			return secondaryDepSet;
		}
		// Get the root build object
		IBuild build = buildModel.getBuild();
		// Get the secondary dependencies build entry
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		// Ensure the build entry is defined
		if (entry == null) {
			return secondaryDepSet;
		}
		// Get the token values for the build entry
		String[] tokens = entry.getTokens();
		// Ensure we have token values
		if (tokens.length == 0) {
			return secondaryDepSet;
		}
		// Add all token values to the dependencies set
		for (int i = 0; i < tokens.length; i++) {
			secondaryDepSet.add(tokens[i]);
		}
		return secondaryDepSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Get the build model
		IBuildModel buildModel = getBuildModel(true);
		// Ensure the build model is defined
		if (buildModel == null) {
			return;
		}
		// Get the root build object
		IBuild build = buildModel.getBuild();
		// Get the secondary dependencies build entry
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		try {
			// Paste all source objects
			for (int i = 0; i < sourceObjects.length; i++) {
				Object sourceObject = sourceObjects[i];
				if (sourceObject instanceof String) {
					// If the build entry is not defined, create one
					if (entry == null) {
						entry = buildModel.getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
						build.add(entry);
					}
					// Add the source object token value to the build entry
					entry.addToken((String) sourceObject);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void doAddDependencies() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IBundlePluginModelBase) {
			IProject proj = getPage().getPDEEditor().getCommonProject();
			IBundlePluginModelBase bmodel = ((IBundlePluginModelBase) model);
			AddNewDependenciesAction action = new AddNewDependenciesAction(proj, bmodel);
			action.run();
		}
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		super.dispose();
	}

	public void modelsChanged(PluginModelDelta delta) {
		fAdditionalBundles = null;
		final Control control = fAdditionalTable.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						fAdditionalTable.refresh();
				}
			});
		}
	}

	private void handleUp() {
		movePlugins(-1);
	}

	private void handleDown() {
		movePlugins(1);
	}

	private void updateUpDownButtons() {
		TablePart tablePart = getTablePart();
		if (fSortAction.isChecked()) {
			tablePart.setButtonEnabled(UP_INDEX, false);
			tablePart.setButtonEnabled(DOWN_INDEX, false);
			return;
		}
		Table table = fAdditionalTable.getTable();
		int index = table.getSelectionIndex();
		int totalElems = table.getItemCount();
		boolean canMove = totalElems > 1 && table.getSelectionCount() == 1;
		tablePart.setButtonEnabled(2, canMove && index > 0);
		tablePart.setButtonEnabled(3, canMove && index >= 0 && index < totalElems - 1);
	}

	private void movePlugins(int newOffset) {
		int index = fAdditionalTable.getTable().getSelectionIndex();
		if (index == -1)
			return; // safety check
		IBuildModel model = getBuildModel(false);
		if (model != null) {
			IBuild build = model.getBuild();
			IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			if (entry instanceof org.eclipse.pde.internal.core.text.build.BuildEntry)
				((org.eclipse.pde.internal.core.text.build.BuildEntry) entry).swap(index, index + newOffset);
		}
		updateButtons();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			updateUpDownButtons();
		}
	}

	protected boolean createCount() {
		return true;
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
		} else if (isTreeViewerSorted()) {
			return false;
		}
		return true;
	}

	private boolean validateDragMoveSanity(Object[] sourceObjects) {
		// Validate source
		if (sourceObjects == null) {
			// No objects
			return false;
		} else if (sourceObjects.length != 1) {
			// Multiple selection not supported
			return false;
		} else if ((sourceObjects[0] instanceof String) == false) {
			// Must be the right type
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
		String sourcePlugin = (String) sourceObjects[0];
		String targetPlugin = (String) targetObject;
		// Get the secondary dependencies build entry
		BuildEntry entry = getSecondaryDepBuildEntry();
		// Validate entry
		if (entry == null) {
			return false;
		}
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the previous plugin of the target 
			String previousPlugin = entry.getPreviousToken(targetPlugin);
			// Ensure the previous token is not the source
			if (sourcePlugin.equals(previousPlugin)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the next plugin of the target 
			String nextPlugin = entry.getNextToken(targetPlugin);
			// Ensure the next plugin is not the source
			if (sourcePlugin.equals(nextPlugin)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
			return false;
		}

		return false;
	}

	private BuildEntry getSecondaryDepBuildEntry() {
		// Get the build model
		IBuildModel buildModel = getBuildModel(true);
		// Ensure the build model is defined
		if (buildModel == null) {
			return null;
		}
		// Get the root build object
		IBuild build = buildModel.getBuild();
		// Ensure we have a root
		if (build == null) {
			return null;
		}
		// Get the secondary dependencies build entry
		IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		// Ensure we have the concrete text entry
		if ((entry instanceof BuildEntry) == false) {
			return null;
		}
		return (BuildEntry) entry;
	}

	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof String) == false) {
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
		String sourcePlugin = (String) sourceObjects[0];
		String targetPlugin = (String) targetObject;
		// Validate move
		if ((targetLocation == ViewerDropAdapter.LOCATION_BEFORE) || (targetLocation == ViewerDropAdapter.LOCATION_AFTER)) {
			// Do move
			doDropMove(sourcePlugin, targetPlugin, targetLocation);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
		}
	}

	/**
	 * @param sourcePlugin
	 * @param targetPlugin
	 * @param targetLocation
	 */
	private void doDropMove(String sourcePlugin, String targetPlugin, int targetLocation) {
		// Remove the original source object
		// Normally we remove the original source object after inserting the
		// serialized source object; however, the plug-ins are removed via ID
		// and having both objects with the same ID co-existing will confound
		// the remove operation
		doDragRemove();
		// Get the secondary dependencies build entry
		BuildEntry entry = getSecondaryDepBuildEntry();
		// Validate entry
		if (entry == null) {
			return;
		}
		// Get the index of the target
		int index = entry.getIndexOf(targetPlugin);
		// Ensure the target index was found
		if (index == -1) {
			return;
		}
		// Determine the location index
		int targetIndex = index;
		if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			targetIndex++;
		}
		// Add source as sibling of target		
		entry.addToken(sourcePlugin, targetIndex);
	}

	/**
	 * 
	 */
	private void doDragRemove() {
		// Get the secondary dependencies build entry
		BuildEntry entry = getSecondaryDepBuildEntry();
		// Validate entry
		if (entry == null) {
			return;
		}
		// Retrieve the original non-serialized source objects dragged initially
		Object[] sourceObjects = getDragSourceObjects();
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		// Remove the library
		String sourcePlugin = (String) sourceObjects[0];
		try {
			entry.removeToken(sourcePlugin);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private boolean isTreeViewerSorted() {
		if (fSortAction == null) {
			return false;
		}
		return fSortAction.isChecked();
	}
}
