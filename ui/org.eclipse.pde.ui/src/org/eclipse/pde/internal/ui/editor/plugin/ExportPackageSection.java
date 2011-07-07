/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - bug 200756
 *     Joern Dinkla <devnull@dinkla.com> - bug 200757
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.FindReferencesAction;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ConditionalListSelectionDialog;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.CalculateUsesAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;

public class ExportPackageSection extends TableSection implements IModelChangedListener {

	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int PROPERTIES_INDEX = 2;
	private static final int CALCULATE_USE_INDEX = 3;

	class ExportPackageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fHeader == null) {
				Bundle bundle = (Bundle) getBundle();
				fHeader = (ExportPackageHeader) bundle.getManifestHeader(getExportedPackageHeader());
			}
			return fHeader == null ? new Object[0] : fHeader.getPackages();
		}
	}

	private TableViewer fPackageViewer;

	private Action fAddAction;
	private Action fGoToAction;
	private Action fRemoveAction;
	private Action fPropertiesAction;
	private ExportPackageHeader fHeader;

	public ExportPackageSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.ExportPackageSection_add, PDEUIMessages.ExportPackageSection_remove, PDEUIMessages.ExportPackageSection_properties, PDEUIMessages.ExportPackageSection_uses});
	}

	private boolean isFragment() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor().getAggregateModel();
		return model.isFragmentModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ExportPackageSection_title);
		if (isFragment())
			section.setDescription(PDEUIMessages.ExportPackageSection_descFragment);
		else
			section.setDescription(PDEUIMessages.ExportPackageSection_desc);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPackageViewer = tablePart.getTableViewer();
		fPackageViewer.setContentProvider(new ExportPackageContentProvider());
		fPackageViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPackageViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				String s1 = e1.toString();
				String s2 = e2.toString();
				if (s1.indexOf(" ") != -1) //$NON-NLS-1$
					s1 = s1.substring(0, s1.indexOf(" ")); //$NON-NLS-1$
				if (s2.indexOf(" ") != -1) //$NON-NLS-1$
					s2 = s2.substring(0, s2.indexOf(" ")); //$NON-NLS-1$
				return super.compare(viewer, s1, s2);
			}
		});
		toolkit.paintBordersFor(container);
		section.setClient(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		if (((ManifestEditor) getPage().getEditor()).isEquinox()) {
			gd.verticalSpan = 2;
			gd.minimumWidth = 300;
		}
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(gd);
		makeActions();

		IBundleModel model = getBundleModel();
		fPackageViewer.setInput(model);
		model.addModelChangedListener(this);
		updateButtons();
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

	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		HashMap currentPackageFragments = null;
		// Only export package objects that represent existing package 
		// fragments within the Java project that this plugin.xml is stored
		// can be pasted
		for (int i = 0; i < sourceObjects.length; i++) {
			// Only export package objects are allowed
			if ((sourceObjects[i] instanceof ExportPackageObject) == false) {
				return false;
			}
			// Get the package fragments that are allowed and store them to 
			// assist in searching
			if (currentPackageFragments == null) {
				currentPackageFragments = createCurrentExportPackageMap();
			}
			// Only export packages that are in the list of allowed package
			// fragments are allowed
			ExportPackageObject exportPackageObject = (ExportPackageObject) sourceObjects[i];
			if (currentPackageFragments.containsKey(exportPackageObject.getName()) == false) {
				return false;
			}
		}
		return true;
	}

	private boolean canAddExportedPackages() {
		// Ensure model is editable
		if (isEditable() == false) {
			return false;
		}
		// Get the model
		IPluginModelBase model = getModel();
		// Ensure model is defined
		if (model == null) {
			return false;
		}
		// Get the underlying resource
		IResource resource = model.getUnderlyingResource();
		// Ensure resource is defined
		if (resource == null) {
			return false;
		}
		// Get the project
		IProject project = resource.getProject();
		// Ensure the project is defined
		if (project == null) {
			return false;
		}
		// Ensure the project is a Java project
		try {
			if (project.hasNature(JavaCore.NATURE_ID) == false) {
				return false;
			}
		} catch (CoreException e) {
			return false;
		}
		return true;
	}

	private HashMap createCurrentExportPackageMap() {
		// Dummy hash map created in order to return a defined but empty map
		HashMap packageFragments = new HashMap(0);
		// Get the model
		IPluginModelBase model = getModel();
		// Ensure model is defined
		if (model == null) {
			return packageFragments;
		}
		// Get the underlying resource
		IResource resource = model.getUnderlyingResource();
		// Ensure resource is defined
		if (resource == null) {
			return packageFragments;
		}
		// Get the project
		IProject project = resource.getProject();
		// Ensure the project is defined
		if (project == null) {
			return packageFragments;
		}
		// Ensure the project is a Java project
		try {
			if (project.hasNature(JavaCore.NATURE_ID) == false) {
				return packageFragments;
			}
		} catch (CoreException e) {
			return packageFragments;
		}
		// Get the Java project
		IJavaProject javaProject = JavaCore.create(project);
		// Ensure the Java project is defined
		if (javaProject == null) {
			return packageFragments;
		}
		// Get the current packages associated with the export package header
		Vector currentExportPackages = null;
		if (fHeader == null) {
			currentExportPackages = new Vector();
		} else {
			currentExportPackages = fHeader.getPackageNames();
		}
		// Get a hashtable of all the package fragments that are allowed to
		// be added to the current export package header
		// Generally, all package fragments contained in the same Java project
		// as the plugin manifest file
		// No duplicates are allowed and all current packages are excluded
		return PDEJavaHelper.getPackageFragmentsHash(javaProject, currentExportPackages, allowJavaPackages());
	}

	private IPluginModelBase getModel() {
		return (IPluginModelBase) getPage().getModel();
	}

	private boolean allowJavaPackages() {
		return "true".equals(getBundle().getHeader(ICoreConstants.ECLIPSE_JREBUNDLE)); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Get the model
		IBundleModel model = getBundleModel();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Get the bundle
		IBundle bundle = model.getBundle();
		// Paste all source objects
		for (int i = 0; i < sourceObjects.length; i++) {
			Object sourceObject = sourceObjects[i];
			if (sourceObject instanceof ExportPackageObject) {
				ExportPackageObject exportPackageObject = (ExportPackageObject) sourceObject;
				// Export package object
				// Adjust all the source object transient field values to
				// acceptable values
				exportPackageObject.reconnect(model, fHeader, getVersionAttribute());
				// Add the object to the header
				if (fHeader == null) {
					// Export package header not defined yet
					// Define one
					// Value will get inserted into a new export package object
					// created by a factory
					// Value needs to be empty string so no export package
					// object is created as the initial value
					bundle.setHeader(getExportedPackageHeader(), ""); //$NON-NLS-1$
				}
				// Add the export package to the header
				fHeader.addPackage(exportPackageObject);
			}
		}
	}

	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getPDEEditor().setSelection(sel);
		updateButtons();
	}

	private void updateButtons() {
		Object[] selected = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();

		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(ADD_INDEX, canAddExportedPackages());
		tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && selected.length > 0);
		tablePart.setButtonEnabled(PROPERTIES_INDEX, shouldEnableProperties(selected));
		tablePart.setButtonEnabled(CALCULATE_USE_INDEX, isEditable() && fPackageViewer.getTable().getItemCount() > 0);

	}

	private boolean shouldEnableProperties(Object[] selected) {
		if (selected.length == 0)
			return false;
		if (selected.length == 1)
			return true;

		String version = ((ExportPackageObject) selected[0]).getVersion();
		for (int i = 1; i < selected.length; i++) {
			ExportPackageObject object = (ExportPackageObject) selected[i];
			if (version == null) {
				if (object.getVersion() != null) {
					return false;
				}
			} else if (!version.equals(object.getVersion())) {
				return false;
			}
		}
		return true;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		handleGoToPackage(selection);
	}

	private IPackageFragment getPackageFragment(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			if (selection.size() != 1)
				return null;

			IBaseModel model = getPage().getModel();
			if (!(model instanceof IPluginModelBase))
				return null;

			return PDEJavaHelper.getPackageFragment(((PackageObject) selection.getFirstElement()).getName(), ((IPluginModelBase) model).getPluginBase().getId(), getPage().getPDEEditor().getCommonProject());
		}
		return null;
	}

	private void handleGoToPackage(ISelection selection) {
		IPackageFragment frag = getPackageFragment(selection);
		if (frag != null)
			try {
				IViewPart part = PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
				ShowInPackageViewAction action = new ShowInPackageViewAction(part.getSite());
				action.run(frag);
			} catch (PartInitException e) {
			}
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case ADD_INDEX :
				handleAdd();
				break;
			case REMOVE_INDEX :
				handleRemove();
				break;
			case PROPERTIES_INDEX :
				handleOpenProperties();
				break;
			case CALCULATE_USE_INDEX :
				calculateUses();
		}
	}

	private void handleOpenProperties() {
		Object[] selected = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();
		ExportPackageObject first = (ExportPackageObject) selected[0];
		DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(), first);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.EXPORTED_PACKAGE_PROPERTIES);
		SWTUtil.setDialogSize(dialog, 400, -1);
		if (selected.length == 1)
			dialog.setTitle(((ExportPackageObject) selected[0]).getName());
		else
			dialog.setTitle(PDEUIMessages.ExportPackageSection_props);
		if (dialog.open() == Window.OK && isEditable()) {
			String newVersion = dialog.getVersion();
			for (int i = 0; i < selected.length; i++) {
				ExportPackageObject object = (ExportPackageObject) selected[i];
				if (!newVersion.equals(object.getVersion()))
					object.setVersion(newVersion);
			}
		}
	}

	private void handleRemove() {
		Object[] removed = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();
		for (int i = 0; i < removed.length; i++) {
			fHeader.removePackage((PackageObject) removed[i]);
		}
	}

	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		final IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				final ConditionalListSelectionDialog dialog = new ConditionalListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), labelProvider, PDEUIMessages.ExportPackageSection_dialogButtonLabel);
				final Collection pckgs = fHeader == null ? new Vector() : fHeader.getPackageNames();
				final boolean allowJava = "true".equals(getBundle().getHeader(ICoreConstants.ECLIPSE_JREBUNDLE)); //$NON-NLS-1$
				Runnable runnable = new Runnable() {
					public void run() {
						ArrayList elements = new ArrayList();
						ArrayList conditional = new ArrayList();
						IPackageFragment[] fragments = PDEJavaHelper.getPackageFragments(JavaCore.create(project), pckgs, allowJava);
						for (int i = 0; i < fragments.length; i++) {
							try {
								if (fragments[i].containsJavaResources()) {
									elements.add(fragments[i]);
								} else {
									conditional.add(fragments[i]);
								}
							} catch (JavaModelException e) {
							}
						}
						dialog.setElements(elements.toArray());
						dialog.setConditionalElements(conditional.toArray());
						dialog.setMultipleSelection(true);
						dialog.setMessage(PDEUIMessages.PackageSelectionDialog_label);
						dialog.setTitle(PDEUIMessages.ExportPackageSection_title);
						dialog.create();
						PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.EXPORT_PACKAGES);
						SWTUtil.setDialogSize(dialog, 400, 500);
					}
				};
				BusyIndicator.showWhile(Display.getCurrent(), runnable);
				if (dialog.open() == Window.OK) {
					Object[] selected = dialog.getResult();
					if (fHeader != null) {
						for (int i = 0; i < selected.length; i++) {
							IPackageFragment candidate = (IPackageFragment) selected[i];
							fHeader.addPackage(new ExportPackageObject(fHeader, candidate, getVersionAttribute()));
						}
					} else {
						getBundle().setHeader(getExportedPackageHeader(), getValue(selected));
						// the way events get triggered, updateButtons isn't called
						if (selected.length > 0)
							getTablePart().setButtonEnabled(CALCULATE_USE_INDEX, true);
					}
				}
				labelProvider.dispose();
			}
		} catch (CoreException e) {
		}
	}

	private String getValue(Object[] objects) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < objects.length; i++) {
			IPackageFragment fragment = (IPackageFragment) objects[i];
			if (buffer.length() > 0)
				buffer.append("," + getLineDelimiter() + " "); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append(fragment.getElementName());
		}
		return buffer.toString();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			fHeader = null;
			markStale();
			return;
		}

		if (getExportedPackageHeader().equals(event.getChangedProperty())) {
			refresh();
			// Bug 171896
			// Since the model sends a CHANGE event instead of
			// an INSERT event on the very first addition to the empty table
			// Selection should fire here to take this first insertion into account
			Object lastElement = fPackageViewer.getElementAt(fPackageViewer.getTable().getItemCount() - 1);
			if (lastElement != null) {
				fPackageViewer.setSelection(new StructuredSelection(lastElement));
			}

			return;
		}

		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ExportPackageObject) {
				ExportPackageObject object = (ExportPackageObject) objects[i];
				switch (event.getChangeType()) {
					case IModelChangedEvent.INSERT :
						fPackageViewer.add(object);
						fPackageViewer.setSelection(new StructuredSelection(object), false);
						fPackageViewer.getTable().setFocus();
						break;
					case IModelChangedEvent.REMOVE :
						Table table = fPackageViewer.getTable();
						int index = table.getSelectionIndex();
						fPackageViewer.remove(object);
						table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
						break;
					default :
						fPackageViewer.refresh(object);
				}
			}
		}
	}

	public void refresh() {
		fPackageViewer.refresh();
		super.refresh();
	}

	private void makeActions() {
		fAddAction = new Action(PDEUIMessages.RequiresSection_add) {
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());
		fGoToAction = new Action(PDEUIMessages.ImportPackageSection_goToPackage) {
			public void run() {
				handleGoToPackage(fPackageViewer.getSelection());
			}
		};
		fRemoveAction = new Action(PDEUIMessages.RequiresSection_delete) {
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());

		fPropertiesAction = new Action(PDEUIMessages.ExportPackageSection_propertyAction) {
			public void run() {
				handleOpenProperties();
			}
		};
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fPackageViewer.getSelection();
		manager.add(fAddAction);
		boolean singleSelection = selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1;
		if (singleSelection)
			manager.add(fGoToAction);
		manager.add(new Separator());
		if (!selection.isEmpty())
			manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		if (singleSelection)
			manager.add(new Action(PDEUIMessages.ExportPackageSection_findReferences) {
				public void run() {
					doSearch(fPackageViewer.getSelection());
				}
			});
		if (shouldEnableProperties(((IStructuredSelection) fPackageViewer.getSelection()).toArray())) {
			manager.add(new Separator());
			manager.add(fPropertiesAction);
		}
	}

	private void doSearch(ISelection sel) {
		IPackageFragment frag = getPackageFragment(sel);
		if (frag != null) {
			FindReferencesAction action = new FindReferencesAction(getPage().getEditorSite());
			action.run(frag);
		} else if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			PackageObject exportObject = (PackageObject) selection.getFirstElement();
			NewSearchUI.runQueryInBackground(new BlankQuery(exportObject));
		}
	}

	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}

	private IBundleModel getBundleModel() {
		BundleInputContext context = getBundleContext();
		return (context != null) ? (IBundleModel) context.getModel() : null;

	}

	private String getLineDelimiter() {
		BundleInputContext inputContext = getBundleContext();
		if (inputContext != null) {
			return inputContext.getLineDelimiter();
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	private IBundle getBundle() {
		IBundleModel model = getBundleModel();
		return (model != null) ? model.getBundle() : null;
	}

	private String getVersionAttribute() {
		int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
		return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
	}

	public String getExportedPackageHeader() {
		int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
		return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
	}

	protected boolean createCount() {
		return true;
	}

	private void calculateUses() {
		final IProject proj = getPage().getPDEEditor().getCommonProject();
		Action action = new CalculateUsesAction(proj, (IBundlePluginModelBase) getPage().getModel());
		action.run();
	}
}
