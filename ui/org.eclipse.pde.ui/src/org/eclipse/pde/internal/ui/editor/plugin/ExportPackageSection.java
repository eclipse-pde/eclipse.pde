/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - bug 200756
 *     Joern Dinkla <devnull@dinkla.com> - bug 200757
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.FindReferencesAction;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.parts.ConditionalListSelectionDialog;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.CalculateUsesAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;

public class ExportPackageSection extends TableSection {

	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int PROPERTIES_INDEX = 2;
	private static final int CALCULATE_USE_INDEX = 3;

	class ExportPackageContentProvider implements IStructuredContentProvider {
		@Override
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
		if (getPage().getPDEEditor().getAggregateModel() instanceof IPluginModelBase model) {
			return model.isFragmentModel();
		}
		return false;
	}

	@Override
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
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				String s1 = e1.toString();
				String s2 = e2.toString();
				if (s1.contains(" ")) //$NON-NLS-1$
					s1 = s1.substring(0, s1.indexOf(' '));
				if (s2.contains(" ")) //$NON-NLS-1$
					s2 = s2.substring(0, s2.indexOf(' '));
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
	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		Map<String, IPackageFragment> currentPackageFragments = null;
		// Only export package objects that represent existing package
		// fragments within the Java project that this plugin.xml is stored
		// can be pasted
		for (Object sourceObject : sourceObjects) {
			// Only export package objects are allowed
			if ((sourceObject instanceof ExportPackageObject) == false) {
				return false;
			}
			// Get the package fragments that are allowed and store them to
			// assist in searching
			if (currentPackageFragments == null) {
				currentPackageFragments = createCurrentExportPackageMap();
			}
			// Only export packages that are in the list of allowed package
			// fragments are allowed
			ExportPackageObject exportPackageObject = (ExportPackageObject) sourceObject;
			if (currentPackageFragments.containsKey(exportPackageObject.getName()) == false) {
				return false;
			}
		}
		return true;
	}

	private boolean canAddExportedPackages() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		final IProject project = model.getUnderlyingResource().getProject();
		try {
			if (isEditable() && getProjectWithJavaNature().isPresent()) {
				return true;
			} else if (isEditable() && hasPluginNature(project)) {
				return true;
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean hasPluginNature(IProject project) throws CoreException {
		return project.isNatureEnabled("org.eclipse.pde.PluginNature"); //$NON-NLS-1$
	}

	private Optional<IProject> getProjectWithJavaNature() {
		Optional<IPluginModelBase> model = Optional.ofNullable(getModel());
		Optional<IProject> project = model.map(IPluginModelBase::getUnderlyingResource).map(IResource::getProject);
		return project.filter(p -> {
			try { // Ensure the project is a Java project
				return p.hasNature(JavaCore.NATURE_ID);
			} catch (CoreException e) {
				return false;
			}
		});
	}

	private Map<String, IPackageFragment> createCurrentExportPackageMap() {
		Optional<IProject> project = getProjectWithJavaNature();
		if (project.isEmpty()) {
			return Collections.emptyMap();
		}
		IJavaProject javaProject = JavaCore.create(project.get());
		// Get the current packages associated with the export package header
		Collection<String> currentExportPackages = fHeader == null ? Collections.emptySet() : fHeader.getPackageNames();
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
			if (sourceObject instanceof ExportPackageObject exportPackageObject) {
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

	@Override
	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getPDEEditor().setSelection(sel);
		updateButtons();
	}

	private void updateButtons() {
		Object[] selected = fPackageViewer.getStructuredSelection().toArray();

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

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleGoToPackage(selection);
	}

	private IPackageFragment getPackageFragment(ISelection sel) {
		if (sel instanceof IStructuredSelection selection) {
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

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case ADD_INDEX -> handleAdd();
			case REMOVE_INDEX -> handleRemove();
			case PROPERTIES_INDEX -> handleOpenProperties();
			case CALCULATE_USE_INDEX -> calculateUses();
		}
	}

	private void handleOpenProperties() {
		Object[] selected = fPackageViewer.getStructuredSelection().toArray();
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
			for (Object selectedObject : selected) {
				ExportPackageObject object = (ExportPackageObject) selectedObject;
				if (!newVersion.equals(object.getVersion()))
					object.setVersion(newVersion);
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
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		final IProject project = model.getUnderlyingResource().getProject();
		final Collection<String> pckgs = fHeader == null ? Collections.emptySet() : fHeader.getPackageNames();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				final ConditionalListSelectionDialog dialog = new ConditionalListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), labelProvider, PDEUIMessages.ExportPackageSection_dialogButtonLabel);
				final boolean allowJava = "true".equals(getBundle().getHeader(ICoreConstants.ECLIPSE_JREBUNDLE)); //$NON-NLS-1$
				Runnable runnable = () -> {
					ArrayList<IPackageFragment> elements = new ArrayList<>();
					ArrayList<IPackageFragment> conditional = new ArrayList<>();
					IPackageFragment[] fragments = PDEJavaHelper.getPackageFragments(JavaCore.create(project), pckgs, allowJava);
					for (IPackageFragment fragment : fragments) {
						try {
							if (fragment.containsJavaResources()) {
								elements.add(fragment);
							} else {
								conditional.add(fragment);
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
				};
				BusyIndicator.showWhile(Display.getCurrent(), runnable);
				if (dialog.open() == Window.OK) {
					Object[] selected = dialog.getResult();
					if (fHeader != null) {
						for (Object selectedObject : selected) {
							IPackageFragment candidate = (IPackageFragment) selectedObject;
							fHeader.addPackage(new ExportPackageObject(fHeader, candidate, getVersionAttribute()));
						}
					} else {
						addPackageIntoManifestFile(selected);
					}
				}
				labelProvider.dispose();
			} else if (hasPluginNature(project)) {
				ILabelProvider labelProvider = nonJavaLabelProvider();
				final ConditionalListSelectionDialog dialog = new ConditionalListSelectionDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), labelProvider,
						PDEUIMessages.ExportPackageSection_dialogButtonLabel);
				try {
					InputStream inputStream = project.getFile("META-INF/MANIFEST.MF").getContents(); //$NON-NLS-1$
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					List<String> jarPaths = extractJars(reader);
					Set<String> packageNames = new HashSet<>();
					packageNames.addAll(getPackageNamesFromJar(jarPaths, pckgs, project));
					if (!packageNames.isEmpty()) {
						dialogforNonJavaPrj(dialog, packageNames);
						if (dialog.open() == Window.OK) {
							Object[] selected = dialog.getResult();
							if (fHeader != null) {
								for (Object selectedObject : selected) {
									fHeader.addPackage(new ExportPackageObject(fHeader, selectedObject.toString(), null,
											getVersionAttribute()));
								}
							} else {
								addPackageIntoManifestFile(selected);
							}
						}
					} else {
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Information", //$NON-NLS-1$
								"No packages are available for export."); //$NON-NLS-1$
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					labelProvider.dispose();
				}
			}
		} catch (CoreException e) {
		}
	}

	private void dialogforNonJavaPrj(final ConditionalListSelectionDialog dialog, Set<String> packageNames) {
		dialog.setElements(packageNames.toArray());
		dialog.setMultipleSelection(true);
		dialog.setMessage(PDEUIMessages.PackageSelectionDialog_label);
		dialog.setTitle(PDEUIMessages.ExportPackageSection_title);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.EXPORT_PACKAGES);
	}

	private void addPackageIntoManifestFile(Object[] selected) {
		getBundle().setHeader(getExportedPackageHeader(), getValue(selected));
		// the way events get triggered, updateButtons isn't
		// called
		if (selected.length > 0)
			getTablePart().setButtonEnabled(CALCULATE_USE_INDEX, true);
	}

	private ILabelProvider nonJavaLabelProvider() {
		ILabelProvider labelProvider = new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
			}
		};
		return labelProvider;
	}

	private List<String> extractJars(BufferedReader reader) throws IOException {
		List<String> jarPaths = new ArrayList<>();
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("Bundle-ClassPath:")) { //$NON-NLS-1$
				String[] entries = line.substring("Bundle-ClassPath:".length()).trim().split(","); //$NON-NLS-1$ //$NON-NLS-2$
				for (String entry : entries) {
					entry = entry.trim();
					if (!entry.isEmpty() && !entry.equals(".") && !entry.equals("*")) { //$NON-NLS-1$ //$NON-NLS-2$
						jarPaths.add(entry);
					}
				}
				while ((line = reader.readLine()) != null && Character.isWhitespace(line.charAt(0))) {
					entries = line.trim().split(","); //$NON-NLS-1$
					for (String entry : entries) {
						entry = entry.trim();
						if (!entry.isEmpty() && !entry.equals(".") && !entry.equals("*")) { //$NON-NLS-1$ //$NON-NLS-2$
							jarPaths.add(entry);
						}
					}
				}
				break;
			}
		}
		return jarPaths;
	}

	public static IResource getPackageResource(IProject project, String packageName) {
		IFolder folder = project.getFolder(packageName.replace('.', '/'));
		if (folder.exists()) {
			return folder;
		}
		return null;
	}

	public static Set<String> getPackageNamesFromJar(List<String> jarPaths, Collection<String> existingPackages,
			IProject project) {
		Set<String> packName = new HashSet<>();
		String location = project.getLocation().toOSString();
		for (String jarFilePath : jarPaths) {
			try (JarFile jarFile = new JarFile(location + "\\" + jarFilePath)) { //$NON-NLS-1$
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.isDirectory() && !entry.getName().equals("META-INF")) { //$NON-NLS-1$
						String packageName = entry.getName().replace('/', '.');
						if (!existingPackages.contains(packageName)) {
							packName.add(packageName);
						}
					} else if (entry.getName().endsWith(".class")) { //$NON-NLS-1$
						String packageName = getPackageNameFromClassName(entry.getName());
						if (!existingPackages.contains(packageName)) {
							packName.add(packageName);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return packName;
	}

	private static String getPackageNameFromClassName(String className) {
		int lastSlashIndex = className.lastIndexOf('/');
		if (lastSlashIndex != -1) {
			return className.substring(0, lastSlashIndex).replace('/', '.');
		}
		return "";
	}

	private String getValue(Object[] objects) {
		StringBuilder buffer = new StringBuilder();
		for (Object object : objects) {
			if (object instanceof IPackageFragment) {
				IPackageFragment fragment = (IPackageFragment) object;
				if (buffer.length() > 0)
					buffer.append("," + getLineDelimiter() + " "); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(fragment.getElementName());
			} else if (object instanceof String) {
				String str = (String) object;
				if (buffer.length() > 0)
					buffer.append("," + getLineDelimiter() + " "); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(str);
			}
		}
		return buffer.toString();
	}

	@Override
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
		for (Object changedObject : objects) {
			if (changedObject instanceof ExportPackageObject object) {
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

		fPropertiesAction = new Action(PDEUIMessages.ExportPackageSection_propertyAction) {
			@Override
			public void run() {
				handleOpenProperties();
			}
		};
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = fPackageViewer.getStructuredSelection();
		manager.add(fAddAction);
		boolean singleSelection = selection.size() == 1;
		if (singleSelection)
			manager.add(fGoToAction);
		manager.add(new Separator());
		if (!selection.isEmpty())
			manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		if (singleSelection)
			manager.add(new Action(PDEUIMessages.ExportPackageSection_findReferences) {
				@Override
				public void run() {
					doSearch(fPackageViewer.getStructuredSelection());
				}
			});
		if (shouldEnableProperties(fPackageViewer.getStructuredSelection().toArray())) {
			manager.add(new Separator());
			manager.add(fPropertiesAction);
		}
	}

	private void doSearch(ISelection sel) {
		IPackageFragment frag = getPackageFragment(sel);
		if (frag != null) {
			FindReferencesAction action = new FindReferencesAction(getPage().getEditorSite());
			action.run(frag);
		} else if (sel instanceof IStructuredSelection selection) {
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
		return TextUtil.getDefaultLineDelimiter();
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

	@Override
	protected boolean createCount() {
		return true;
	}

	private void calculateUses() {
		final IProject proj = getPage().getPDEEditor().getCommonProject();
		Action action = new CalculateUsesAction(proj, (IBundlePluginModelBase) getPage().getModel());
		action.run();
	}
}
