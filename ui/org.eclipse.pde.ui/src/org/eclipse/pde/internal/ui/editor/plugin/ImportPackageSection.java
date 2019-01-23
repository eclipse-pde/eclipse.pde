/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.FindReferencesInWorkingSetAction;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.parts.ConditionalListSelectionDialog;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ImportPackageSection extends TableSection {

	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int PROPERTIES_INDEX = 2;

	private ImportPackageHeader fHeader;

	class ImportItemWrapper {
		Object fUnderlying;

		public ImportItemWrapper(Object underlying) {
			fUnderlying = underlying;
		}

		@Override
		public String toString() {
			return getName();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ImportItemWrapper) {
				ImportItemWrapper item = (ImportItemWrapper) obj;
				return getName().equals(item.getName());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		public String getName() {
			if (fUnderlying instanceof ExportPackageDescription)
				return ((ExportPackageDescription) fUnderlying).getName();
			if (fUnderlying instanceof IPackageFragment)
				return ((IPackageFragment) fUnderlying).getElementName();
			if (fUnderlying instanceof ExportPackageObject)
				return ((ExportPackageObject) fUnderlying).getName();
			return null;
		}

		public Version getVersion() {
			if (fUnderlying instanceof ExportPackageDescription)
				return ((ExportPackageDescription) fUnderlying).getVersion();
			if (fUnderlying instanceof ExportPackageObject) {
				String version = ((ExportPackageObject) fUnderlying).getVersion();
				if (version != null)
					return new Version(version);
			}
			return null;
		}

		boolean hasVersion() {
			return hasEPD() && ((ExportPackageDescription) fUnderlying).getVersion() != null;
		}

		boolean hasEPD() {
			return fUnderlying instanceof ExportPackageDescription;
		}
	}

	class ImportPackageContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (fHeader == null) {
				Bundle bundle = (Bundle) getBundle();
				fHeader = (ImportPackageHeader) bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
			}
			return fHeader == null ? new Object[0] : fHeader.getPackages();
		}
	}

	class ImportPackageDialogLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}

		@Override
		public String getText(Object element) {
			StringBuilder buffer = new StringBuilder();
			ImportItemWrapper p = (ImportItemWrapper) element;
			buffer.append(p.getName());
			Version version = p.getVersion();
			if (version != null && !Version.emptyVersion.equals(version)) {
				// Bug 183417 - Bidi3.3: Elements' labels in the extensions page in the fragment manifest characters order is incorrect
				// add RTL zero length character just before the ( and the LTR character just after to ensure:
				// 1. The leading parenthesis takes proper orientation when running in bidi configuration
				// 2. The bundle's version is always displayed as LTR.  Otherwise if qualifier contains an alpha,
				// 		it would be displayed incorrectly when running RTL.
				buffer.append(' ');
				buffer.append(PDELabelProvider.formatVersion(version.toString()));
			}
			return buffer.toString();
		}
	}

	private TableViewer fPackageViewer;

	private Action fAddAction;
	private Action fGoToAction;
	private Action fRemoveAction;
	private Action fPropertiesAction;

	public ImportPackageSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.ImportPackageSection_add, PDEUIMessages.ImportPackageSection_remove, PDEUIMessages.ImportPackageSection_properties});
	}

	private boolean isFragment() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor().getAggregateModel();
		return model != null && model.isFragmentModel();
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ImportPackageSection_required);
		if (isFragment())
			section.setDescription(PDEUIMessages.ImportPackageSection_descFragment);
		else
			section.setDescription(PDEUIMessages.ImportPackageSection_desc);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPackageViewer = tablePart.getTableViewer();
		fPackageViewer.setContentProvider(new ImportPackageContentProvider());
		fPackageViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPackageViewer.setComparator(new ViewerComparator() {
			@Override
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
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		makeActions();

		IBundleModel model = getBundleModel();
		fPackageViewer.setInput(model);
		model.addModelChangedListener(this);
		updateButtons();
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
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		// Only non-duplicate import packages can be pasted
		for (Object sourceObject : sourceObjects) {
			// Only import package objects are allowed
			if ((sourceObject instanceof ImportPackageObject) == false) {
				return false;
			}
			// Note:  Should check if the package fragment represented by the
			// import package object exists
			// (like in org.eclipse.pde.internal.ui.editor.plugin.ImportPackageSection.setElements(ConditionalListSelectionDialog))
			// However, the operation is too performance intensive as it
			// requires searching all workspace and target plug-in

			// If the import package header is not defined, no import packages
			// have been defined yet
			if (fHeader == null) {
				continue;
			}
			// Only import package objects that have not already been
			// specified are allowed (no duplicates)
			ImportPackageObject importPackageObject = (ImportPackageObject) sourceObject;
			if (fHeader.hasPackage(importPackageObject.getName())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
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
		for (Object sourceObject : sourceObjects) {
			if (sourceObject instanceof ImportPackageObject) {
				ImportPackageObject importPackageObject = (ImportPackageObject) sourceObject;
				// Import package object
				// Adjust all the source object transient field values to
				// acceptable values
				importPackageObject.reconnect(model, fHeader, getVersionAttribute());
				// Add the object to the header
				if (fHeader == null) {
					// Import package header not defined yet
					// Define one
					// Value will get inserted into a new import package object
					// created by a factory
					// Value needs to be empty string so no import package
					// object is created as the initial value
					bundle.setHeader(getImportedPackageHeader(), ""); //$NON-NLS-1$
				}
				// Add the import package to the header
				fHeader.addPackage(importPackageObject);
			}
		}
	}

	private String getImportedPackageHeader() {
		return Constants.IMPORT_PACKAGE;
	}

	@Override
	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getPDEEditor().setSelection(sel);
		updateButtons();
	}

	private void updateButtons() {
		Object[] selected = fPackageViewer.getStructuredSelection().toArray();
		int size = selected.length;
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(ADD_INDEX, isEditable());
		tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && size > 0);
		tablePart.setButtonEnabled(PROPERTIES_INDEX, shouldEnableProperties(selected));
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleGoToPackage(selection);
	}

	@Override
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
		}
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

	private void handleOpenProperties() {
		Object[] selected = fPackageViewer.getStructuredSelection().toArray();
		ImportPackageObject first = (ImportPackageObject) selected[0];
		DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(), first);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.IMPORTED_PACKAGE_PROPERTIES);
		SWTUtil.setDialogSize(dialog, 400, -1);
		if (selected.length == 1)
			dialog.setTitle(((ImportPackageObject) selected[0]).getName());
		else
			dialog.setTitle(PDEUIMessages.ExportPackageSection_props);
		if (dialog.open() == Window.OK && isEditable()) {
			String newVersion = dialog.getVersion();
			boolean newOptional = dialog.isOptional();
			for (Object selectedObject : selected) {
				ImportPackageObject object = (ImportPackageObject) selectedObject;
				if (!newVersion.equals(object.getVersion()))
					object.setVersion(newVersion);
				if (!newOptional == object.isOptional())
					object.setOptional(newOptional);
			}
		}
	}

	private void handleRemove() {
		Object[] removed = fPackageViewer.getStructuredSelection().toArray();
		for (Object removedObject : removed) {
			fHeader.removePackage((PackageObject) removedObject);
		}
	}

	private void handleAdd() {
		final ConditionalListSelectionDialog dialog = new ConditionalListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new ImportPackageDialogLabelProvider(), PDEUIMessages.ImportPackageSection_dialogButtonLabel);
		Runnable runnable = () -> {
			setElements(dialog);
			dialog.setMultipleSelection(true);
			dialog.setMessage(PDEUIMessages.ImportPackageSection_exported);
			dialog.setTitle(PDEUIMessages.ImportPackageSection_selection);
			dialog.create();
			PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.IMPORT_PACKAGES);
			SWTUtil.setDialogSize(dialog, 400, 500);
		};

		BusyIndicator.showWhile(Display.getCurrent(), runnable);
		if (dialog.open() == Window.OK) {
			Object[] selected = dialog.getResult();
			if (fHeader != null) {
				Set<String> names = new HashSet<>(); // set of String names, do not allow the same package to be added twice
				for (int i = 0; i < selected.length; i++) {
					ImportPackageObject impObject = null;
					if (selected[i] instanceof ImportItemWrapper)
						selected[i] = ((ImportItemWrapper) selected[i]).fUnderlying;

					if (selected[i] instanceof ExportPackageDescription)
						impObject = new ImportPackageObject(fHeader, (ExportPackageDescription) selected[i], getVersionAttribute());
					else if (selected[i] instanceof IPackageFragment) {
						// non exported package
						IPackageFragment fragment = ((IPackageFragment) selected[i]);
						impObject = new ImportPackageObject(fHeader, fragment.getElementName(), null, getVersionAttribute());
					} else if (selected[i] instanceof ExportPackageObject) {
						ExportPackageObject epo = (ExportPackageObject) selected[i];
						impObject = new ImportPackageObject(fHeader, epo.getName(), epo.getVersion(), getVersionAttribute());
					}
					if (impObject != null && names.add(impObject.getName()))
						fHeader.addPackage(impObject);
				}
			} else {
				getBundle().setHeader(Constants.IMPORT_PACKAGE, getValue(selected));
			}
		}
	}

	private String getValue(Object[] objects) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof ImportItemWrapper))
				continue;
			Version version = ((ImportItemWrapper) objects[i]).getVersion();
			if (buffer.length() > 0)
				buffer.append("," + getLineDelimiter() + " "); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append(((ImportItemWrapper) objects[i]).getName());
			if (version != null && !version.equals(Version.emptyVersion)) {
				buffer.append(";"); //$NON-NLS-1$
				buffer.append(getVersionAttribute());
				buffer.append("=\""); //$NON-NLS-1$
				buffer.append(version.toString());
				buffer.append("\""); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

	private void setElements(ConditionalListSelectionDialog dialog) {
		Set<String> forbidden = getForbiddenIds();
		boolean allowJava = "true".equals(getBundle().getHeader(ICoreConstants.ECLIPSE_JREBUNDLE)); //$NON-NLS-1$

		ArrayList<ImportItemWrapper> elements = new ArrayList<>();
		ArrayList<ImportItemWrapper> conditional = new ArrayList<>();
		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		Set<NameVersionDescriptor> nameVersions = new HashSet<>(); // Set of NameVersionDescriptors, used to remove duplicate entries

		for (IPluginModelBase pluginModel : models) {
			BundleDescription desc = pluginModel.getBundleDescription();

			// If the current model is a fragment, it can export packages only if its parent has hasExtensibleAPI set
			if (isFragmentThatCannotExportPackages(pluginModel))
				continue;

			String id = desc == null ? null : desc.getSymbolicName();
			if (id == null || forbidden.contains(id))
				continue;

			ExportPackageDescription[] exported = desc.getExportPackages();
			for (ExportPackageDescription exportedPackage : exported) {
				String name = exportedPackage.getName();
				NameVersionDescriptor nameVersion = new NameVersionDescriptor(exportedPackage.getName(), exportedPackage.getVersion().toString(), NameVersionDescriptor.TYPE_PACKAGE);
				if (("java".equals(name) || name.startsWith("java.")) && !allowJava) //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				if (nameVersions.add(nameVersion) && (fHeader == null || !fHeader.hasPackage(name)))
					elements.add(new ImportItemWrapper(exportedPackage));
			}
			IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor().getAggregateModel();
			if (model instanceof IBundlePluginModelBase) {
				IBundleModel bmodel = ((IBundlePluginModelBase) model).getBundleModel();
				if (bmodel != null) {
					ExportPackageHeader header = (ExportPackageHeader) bmodel.getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
					if (header != null) {
						ExportPackageObject[] pkgs = header.getPackages();
						for (ExportPackageObject pkg : pkgs) {
							String name = pkg.getName();
							String version = pkg.getVersion();
							NameVersionDescriptor nameVersion = new NameVersionDescriptor(name, version, NameVersionDescriptor.TYPE_PACKAGE);
							if (nameVersions.add(nameVersion) && (fHeader == null || !fHeader.hasPackage(name)))
								elements.add(new ImportItemWrapper(pkg));
						}
					}
				}

			}
		}
		for (IPluginModelBase model : models) {
			try {
				// add un-exported packages in workspace non-binary plug-ins
				IResource resource = model.getUnderlyingResource();
				IProject project = resource != null ? resource.getProject() : null;
				if (project == null || !project.hasNature(JavaCore.NATURE_ID) || WorkspaceModelManager.isBinaryProject(project) || !PDEProject.getManifest(project).exists())
					continue;

				// If the current model is a fragment, it can export packages only if its parent has hasExtensibleAPI set
				if (isFragmentThatCannotExportPackages(model))
					continue;

				IJavaProject jp = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
				for (int j = 0; j < roots.length; j++) {
					if (roots[j].getKind() == IPackageFragmentRoot.K_SOURCE || (roots[j].getKind() == IPackageFragmentRoot.K_BINARY && !roots[j].isExternal())) {
						IJavaElement[] children = roots[j].getChildren();
						for (IJavaElement child : children) {
							IPackageFragment f = (IPackageFragment) child;
							String name = f.getElementName();
							NameVersionDescriptor nameVersion = new NameVersionDescriptor(name, null, NameVersionDescriptor.TYPE_PACKAGE);
							if (name.equals("")) //$NON-NLS-1$
								name = "."; //$NON-NLS-1$
							if (nameVersions.add(nameVersion) && (f.hasChildren() || f.getNonJavaResources().length > 0))
								conditional.add(new ImportItemWrapper(f));
						}
					}
				}
			} catch (CoreException e) {
			}
		}
		dialog.setElements(elements.toArray());
		dialog.setConditionalElements(conditional.toArray());
	}

	/**
	 * Returns whether the provided plug-in model is a fragment that cannot export
	 * its packages to other bundles (<code>hasExtensibleAPI</code> is not set).  Will
	 * return false if the model does not represent a fragment.
	 *
	 * @param fragment the model to test
	 * @return <code>true</code> if the model is a fragment that cannot export packages
	 */
	private boolean isFragmentThatCannotExportPackages(IPluginModelBase fragment) {
		if (!fragment.isFragmentModel()) {
			// Not a fragment
			return false;
		}
		BundleDescription bundleDescription = fragment.getBundleDescription();
		if (bundleDescription == null) {
			// Classic plugin, do not change the behavior
			return false;
		}
		HostSpecification hostSpec = bundleDescription.getHost();
		if (hostSpec == null) {
			// Not a fragment
			return false;
		}
		BundleDescription[] hosts = hostSpec.getHosts();
		// At least one of fragment hosts has to have extensible API
		for (BundleDescription host : hosts) {
			if (ClasspathUtilCore.hasExtensibleAPI(PluginRegistry.findModel(host)))
				return false;
		}
		// Fragment that cannot export
		return true;
	}

	@Override
	public void modelChanged(final IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			fHeader = null;
			markStale();
			return;
		}

		// Model change may have come from a non UI thread such as the auto add dependencies operation. See bug 333533
		UIJob job = new UIJob("Update package imports") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (Constants.IMPORT_PACKAGE.equals(event.getChangedProperty())) {
					refresh();
					// Bug 171896
					// Since the model sends a CHANGE event instead of
					// an INSERT event on the very first addition to the empty table
					// Selection should fire here to take this first insertion into account
					Object lastElement = fPackageViewer.getElementAt(fPackageViewer.getTable().getItemCount() - 1);
					if (lastElement != null) {
						fPackageViewer.setSelection(new StructuredSelection(lastElement));
					}
					return Status.OK_STATUS;
				}

				Object[] objects = event.getChangedObjects();
				for (Object changedObject : objects) {
					if (changedObject instanceof ImportPackageObject) {
						ImportPackageObject object = (ImportPackageObject) changedObject;
						switch (event.getChangeType()) {
							case IModelChangedEvent.INSERT :
								fPackageViewer.remove(object); // If another thread has modified the header, avoid creating a duplicate
								fPackageViewer.add(object);
								fPackageViewer.setSelection(new StructuredSelection(object));
								fPackageViewer.getTable().setFocus();
								break;
							case IModelChangedEvent.REMOVE :
								Table table = fPackageViewer.getTable();
								int index = table.getSelectionIndex();
								fPackageViewer.remove(object);
								table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
								updateButtons();
								break;
							default :
								fPackageViewer.refresh(object);
						}
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public void refresh() {
		fPackageViewer.refresh();
		super.refresh();
	}

	private void makeActions() {
		fAddAction = new Action(PDEUIMessages.RequiresSection_add) {
			@Override
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());
		fGoToAction = new Action(PDEUIMessages.ImportPackageSection_goToPackage) {
			@Override
			public void run() {
				handleGoToPackage(fPackageViewer.getStructuredSelection());
			}
		};
		fRemoveAction = new Action(PDEUIMessages.RequiresSection_delete) {
			@Override
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());

		fPropertiesAction = new Action(PDEUIMessages.ImportPackageSection_propertyAction) {
			@Override
			public void run() {
				handleOpenProperties();
			}
		};
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		final IStructuredSelection selection = fPackageViewer.getStructuredSelection();
		manager.add(fAddAction);
		boolean singleSelection = selection.size() == 1;
		if (singleSelection)
			manager.add(fGoToAction);
		manager.add(new Separator());
		if (!selection.isEmpty())
			manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);

		if (((IModel) getPage().getModel()).getUnderlyingResource() != null) {
			manager.add(new Separator());
			if (singleSelection) {
				manager.add(new Action(PDEUIMessages.DependencyExtentSearchResultPage_referencesInPlugin) {
					@Override
					public void run() {
						doReferenceSearch(selection);
					}
				});
			}
			manager.add(new UnusedDependenciesAction((IPluginModelBase) getPage().getModel(), false));
		}

		if (shouldEnableProperties(fPackageViewer.getStructuredSelection().toArray())) {
			manager.add(new Separator());
			manager.add(fPropertiesAction);
		}
	}

	private void doReferenceSearch(final ISelection sel) {
		IPackageFragmentRoot[] roots = null;
		try {
			roots = getSourceRoots();
		} catch (JavaModelException e) {
		}
		final IPackageFragment fragment = getPackageFragment(sel);
		if (fragment != null && roots != null) {
			IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
			IWorkingSet set = manager.createWorkingSet("temp", roots); //$NON-NLS-1$
			new FindReferencesInWorkingSetAction(getPage().getEditorSite(), new IWorkingSet[] {set}).run(fragment);
			manager.removeWorkingSet(set);
		} else if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			PackageObject importObject = (PackageObject) selection.getFirstElement();
			NewSearchUI.runQueryInBackground(new BlankQuery(importObject));
		}
	}

	private IPackageFragmentRoot[] getSourceRoots() throws JavaModelException {
		ArrayList<IPackageFragmentRoot> result = new ArrayList<>();
		IProject project = getPage().getPDEEditor().getCommonProject();
		// would normally return array of size 0, but by returning null can optimize the search to run faster.
		if (project == null) {
			return null;
		}
		IPackageFragmentRoot[] roots = JavaCore.create(project).getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE || (roots[i].isArchive() && !roots[i].isExternal()))
				result.add(roots[i]);
		}
		return result.toArray(new IPackageFragmentRoot[result.size()]);
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
		return TextUtil.getDefaultLineDelimiter();
	}

	private IBundle getBundle() {
		IBundleModel model = getBundleModel();
		return (model != null) ? model.getBundle() : null;
	}

	private String getVersionAttribute() {
		return getVersionAttribute(getBundle());
	}

	private String getVersionAttribute(IBundle bundle) {
		int manifestVersion = BundlePluginBase.getBundleManifestVersion(bundle);
		return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
	}

	private Set<String> getForbiddenIds() {
		HashSet<String> set = new HashSet<>();
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor().getAggregateModel();
		if (model == null) {
			return set;
		}
		String id = model.getPluginBase().getId();
		if (id != null)
			set.add(id);
		IPluginImport[] imports = model.getPluginBase().getImports();
		State state = TargetPlatformHelper.getState();
		for (IPluginImport pluginImport : imports) {
			addDependency(state, pluginImport.getId(), set);
		}
		return set;
	}

	private void addDependency(State state, String bundleID, Set<String> set) {
		if (bundleID == null || !set.add(bundleID))
			return;

		BundleDescription desc = state.getBundle(bundleID, null);
		if (desc == null)
			return;

		BundleDescription[] fragments = desc.getFragments();
		for (BundleDescription fragment : fragments) {
			addDependency(state, fragment.getSymbolicName(), set);
		}

		BundleSpecification[] specs = desc.getRequiredBundles();
		for (BundleSpecification spec : specs) {
			if (spec.isResolved() && spec.isExported()) {
				addDependency(state, spec.getName(), set);
			}
		}
	}

	@Override
	protected boolean createCount() {
		return true;
	}

	private boolean shouldEnableProperties(Object[] selected) {
		if (selected.length == 0)
			return false;
		if (selected.length == 1)
			return true;

		String version = ((ImportPackageObject) selected[0]).getVersion();
		boolean optional = ((ImportPackageObject) selected[0]).isOptional();
		for (int i = 1; i < selected.length; i++) {
			ImportPackageObject object = (ImportPackageObject) selected[i];
			if (version == null) {
				if (object.getVersion() != null || !(optional == object.isOptional())) {
					return false;
				}
			} else if (!version.equals(object.getVersion()) || !(optional == object.isOptional())) {
				return false;
			}
		}
		return true;
	}

}
