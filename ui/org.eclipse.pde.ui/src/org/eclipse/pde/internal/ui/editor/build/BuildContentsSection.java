/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public abstract class BuildContentsSection extends TableSection implements IResourceChangeListener, IResourceDeltaVisitor {

	protected CheckboxTreeViewer fTreeViewer;
	private boolean fDoRefresh = false;
	protected IContainer fBundleRoot;
	protected IBuildModel fBuildModel;
	protected IResource fOriginalResource, fParentResource;
	protected boolean isChecked;

	public static class TreeContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IContainer) {
				try {
					return ((IContainer) parent).members();
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parent) {
			try {
				if (parent instanceof IFolder)
					return ((IFolder) parent).members();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			return new Object[0];
		}

		public Object[] getFolderChildren(Object parent) {
			IResource[] members = null;
			try {
				if (!(parent instanceof IFolder))
					return new Object[0];
				members = ((IFolder) parent).members();
				ArrayList<IResource> results = new ArrayList<>();
				for (IResource member : members) {
					if ((member.getType() == IResource.FOLDER)) {
						results.add(member);
					}
				}
				return results.toArray();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element != null && element instanceof IResource) {
				return ((IResource) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof IFolder)
				return getChildren(element).length > 0;
			return false;
		}
	}

	@Override
	protected void createViewerPartControl(Composite parent, int style, int span, FormToolkit toolkit) {
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = this::fillContextMenu;
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = fTreeViewer.getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(getPage().getPDEEditor().getContributor().getRevertAction());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
	}

	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		return (IBuildModel) context.getModel();
	}

	public BuildContentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[0]);
		PDEPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public void createClient(final Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		fBuildModel = getBuildModel();
		if (fBuildModel.getUnderlyingResource() != null)
			fBundleRoot = PDEProject.getBundleRoot(fBuildModel.getUnderlyingResource().getProject());

		fTreeViewer = new CheckboxTreeViewer(toolkit.createTree(container, SWT.CHECK));
		fTreeViewer.setContentProvider(new TreeContentProvider());
		fTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
		fTreeViewer.setAutoExpandLevel(0);
		fTreeViewer.addCheckStateListener(event -> {
			final Object element = event.getElement();
			BusyIndicator.showWhile(section.getDisplay(), () -> {
				if (element instanceof IFile) {
					IFile file = (IFile) event.getElement();
					handleCheckStateChanged(file, event.getChecked());
				} else if (element instanceof IFolder) {
					IFolder folder = (IFolder) event.getElement();
					handleCheckStateChanged(folder, event.getChecked());
				}
			});
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 100;
		fTreeViewer.getTree().setLayoutData(gd);
		initialize();
		toolkit.paintBordersFor(container);
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		section.setClient(container);
	}

	public void enableSection(boolean enable) {
		fTreeViewer.getTree().setEnabled(enable);
	}

	protected void handleCheckStateChanged(IResource resource, boolean checked) {
		fOriginalResource = resource;
		isChecked = checked;
		boolean wasTopParentChecked = fTreeViewer.getChecked(fOriginalResource.getParent());
		if (!isChecked) {
			resource = handleAllUnselected(resource, resource.getName());
		}
		fParentResource = resource;
		handleBuildCheckStateChange(wasTopParentChecked);
	}

	protected IResource handleAllUnselected(IResource resource, String name) {
		IResource parent = resource.getParent();
		if (parent.equals(fBundleRoot)) {
			return resource;
		}
		try {
			boolean uncheck = true;
			IResource[] members = ((IFolder) parent).members();
			for (int i = 0; i < members.length; i++) {
				if (fTreeViewer.getChecked(members[i]) && !members[i].getName().equals(name))
					uncheck = false;
			}
			if (uncheck) {
				return handleAllUnselected(parent, parent.getName());
			}
			return resource;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
	}

	protected void setChildrenGrayed(IResource folder, boolean isGray) {
		fTreeViewer.setGrayed(folder, isGray);
		if (((TreeContentProvider) fTreeViewer.getContentProvider()).hasChildren(folder)) {
			Object[] members = ((TreeContentProvider) fTreeViewer.getContentProvider()).getFolderChildren(folder);
			for (Object member : members) {
				setChildrenGrayed((IFolder) member, isGray);
			}
		}
	}

	protected void setParentsChecked(IResource resource) {
		if (resource.getParent() != resource.getProject()) {
			fTreeViewer.setChecked(resource.getParent(), true);
			setParentsChecked(resource.getParent());
		}
	}

	/**
	 * removes all child resources of the specified folder from build entries
	 *
	 * @param folder -
	 *            current folder being modified in tree
	 *
	 * note: does not remove folder itself
	 */
	protected abstract void deleteFolderChildrenFromEntries(IFolder folder);

	protected void initializeCheckState() {
		uncheckAll();
	}

	protected void initializeCheckState(final IBuildEntry includes, final IBuildEntry excludes) {
		fTreeViewer.getTree().getDisplay().asyncExec(() -> BusyIndicator.showWhile(Display.getCurrent(), () -> {
			if (fTreeViewer.getTree().isDisposed())
				return;
			ArrayList<String> fileExt = new ArrayList<>();
			String[] inclTokens, exclTokens = new String[0];
			if (fBundleRoot == null || includes == null)
				return;
			inclTokens = includes.getTokens();
			if (excludes != null)
				exclTokens = excludes.getTokens();
			Set<String> temp = new TreeSet<>();
			Collections.addAll(temp, inclTokens);
			Collections.addAll(temp, exclTokens);
			Iterator<String> iter = temp.iterator();
			while (iter.hasNext()) {
				String resource = iter.next().toString();
				boolean isIncluded = includes.contains(resource);
				if (resource.isEmpty()) {
					// ignore - empty line
				} else if (resource.equals(".") || resource.equals("./") || resource.equals(".\\")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					// ignore - should be root directory
				} else if (resource.lastIndexOf(IPath.SEPARATOR) == resource.length() - 1) {
					IFolder folder = fBundleRoot.getFolder(IPath.fromOSString(resource));
					if (!folder.exists())
						continue;
					fTreeViewer.setSubtreeChecked(folder, isIncluded);
					fTreeViewer.setParentsGrayed(folder, true);
					if (isIncluded) {
						setParentsChecked(folder);
						fTreeViewer.setGrayed(folder, false);
					}
				} else if (resource.startsWith("*.")) { //$NON-NLS-1$
					if (isIncluded)
						fileExt.add(resource.substring(2));
				} else {
					IFile file = fBundleRoot.getFile(IPath.fromOSString(resource));
					if (!file.exists())
						continue;
					fTreeViewer.setChecked(file, isIncluded);
					fTreeViewer.setParentsGrayed(file, true);
					if (isIncluded) {
						fTreeViewer.setGrayed(file, false);
						setParentsChecked(file);
					}
				}
			}
			if (fileExt.isEmpty())
				return;
			try {
				IResource[] members = fBundleRoot.members();
				for (int i = 0; i < members.length; i++) {
					if (!(members[i] instanceof IFolder) && (fileExt.contains(members[i].getFileExtension()))) {
						fTreeViewer.setChecked(members[i], includes.contains("*." //$NON-NLS-1$
								+ members[i].getFileExtension()));
					}
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}));
	}

	protected abstract void handleBuildCheckStateChange(boolean wasTopParentChecked);

	protected void handleCheck(IBuildEntry includes, IBuildEntry excludes, String resourceName, IResource resource, boolean wasTopParentChecked, String PROPERTY_INCLUDES) {

		try {
			if (includes == null) {
				includes = fBuildModel.getFactory().createEntry(PROPERTY_INCLUDES);
				IBuild build = fBuildModel.getBuild();
				build.add(includes);
			}
			if ((!wasTopParentChecked && !includes.contains(resourceName)) || isValidIncludeEntry(includes, excludes, resource, resourceName)) {
				includes.addToken(resourceName);
			}
			if (excludes != null && excludes.contains(resourceName))
				excludes.removeToken(resourceName);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected boolean isValidIncludeEntry(IBuildEntry includes, IBuildEntry excludes, IResource resource, String resourceName) {
		if (excludes == null)
			return true;
		IPath resPath = resource.getProjectRelativePath();
		while (resPath.segmentCount() > 1) {
			resPath = resPath.removeLastSegments(1);
			if (includes.contains(resPath.toString() + IPath.SEPARATOR))
				return false;
			else if (excludes.contains(resPath.toString() + IPath.SEPARATOR))
				return true;
		}
		return !excludes.contains(resourceName);
	}

	protected void handleUncheck(IBuildEntry includes, IBuildEntry excludes, String resourceName, IResource resource, String PROPERTY_EXCLUDES) {

		try {
			if (fTreeViewer.getChecked(resource.getParent())) {
				if (excludes == null) {
					excludes = fBuildModel.getFactory().createEntry(PROPERTY_EXCLUDES);
					IBuild build = fBuildModel.getBuild();
					build.add(excludes);
				}
				if (!excludes.contains(resourceName) && (includes != null ? !includes.contains(resourceName) : true))
					excludes.addToken(resourceName);
			}
			if (includes != null) {
				if (includes.contains(resourceName))
					includes.removeToken(resourceName);
				if (includes.contains("*." + resource.getFileExtension())) { //$NON-NLS-1$
					IResource[] members = fBundleRoot.members();
					for (int i = 0; i < members.length; i++) {
						if (!(members[i] instanceof IFolder) && !members[i].getName().equals(resource.getName()) && (resource.getFileExtension().equals(members[i].getFileExtension()))) {
							includes.addToken(members[i].getName());
						}
						IBuildEntry[] libraries = BuildUtil.getBuildLibraries(fBuildModel.getBuild().getBuildEntries());
						if (resource.getFileExtension().equals("jar") //$NON-NLS-1$
								&& libraries.length != 0) {
							for (IBuildEntry library : libraries) {
								String libName = library.getName().substring(7);
								IPath path = fBundleRoot.getFile(IPath.fromOSString(libName)).getProjectRelativePath().makeRelativeTo(fBundleRoot.getProjectRelativePath());
								if (path.segmentCount() == 1 && !includes.contains(libName) && !libName.equals(resource.getName()))
									includes.addToken(libName);
							}
						}
					}
					includes.removeToken("*." + resource.getFileExtension()); //$NON-NLS-1$
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected String getResourceFolderName(String resourceName) {
		return resourceName + IPath.SEPARATOR;
	}

	/**
	 * @param resource -
	 *            file/folder being modified in tree
	 * @param resourceName -
	 *            name file/folder
	 * @return relative path of folder if resource is folder, otherwise, return
	 *         resourceName
	 */
	protected String handleResourceFolder(IResource resource, String resourceName) {
		if (resource instanceof IFolder) {
			deleteFolderChildrenFromEntries((IFolder) resource);
			return getResourceFolderName(resourceName);
		}
		return resourceName;
	}

	public void initialize() {
		if (fTreeViewer.getInput() == null) {
			fTreeViewer.setUseHashlookup(true);
			fTreeViewer.setInput(fBundleRoot);
		}
		fBuildModel.addModelChangedListener(this);
	}

	@Override
	public void dispose() {
		fBuildModel.removeModelChangedListener(this);
		PDEPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	protected void deleteEmptyEntries() {
		IBuild build = fBuildModel.getBuild();
		IBuildEntry[] entries = {build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES), build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES), build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES), build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES)};
		try {
			for (IBuildEntry entry : entries) {
				if (entry != null && entry.getTokens().length == 0)
					build.remove(entry);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public CheckboxTreeViewer getTreeViewer() {
		return fTreeViewer;
	}

	@Override
	protected IStructuredSelection getViewerSelection() {
		return getTreeViewer().getStructuredSelection();
	}

	@Override
	public void refresh() {
		initializeCheckState();
		super.refresh();
	}

	public void uncheckAll() {
		fTreeViewer.setCheckedElements(new Object[0]);
	}

	protected void removeChildren(IBuildEntry entry, String parentFolder) {
		try {
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].indexOf(IPath.SEPARATOR) != -1 && tokens[i].startsWith(parentFolder) && !tokens[i].equals(parentFolder)) {
						entry.removeToken(tokens[i]);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (fTreeViewer.getControl().isDisposed())
			return;
		fDoRefresh = false;
		IResourceDelta delta = event.getDelta();
		try {
			if (delta != null)
				delta.accept(this);
			if (fDoRefresh) {
				asyncRefresh();
				fDoRefresh = false;
			}
		} catch (CoreException e) {
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		IProject project = fBuildModel.getUnderlyingResource().getProject();

		if ((resource instanceof IFile || resource instanceof IFolder) && resource.getProject().equals(project)) {
			if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
				fDoRefresh = true;
				return false;
			}
		} else if (resource instanceof IProject && ((IProject) resource).equals(project)) {
			return delta.getKind() != IResourceDelta.REMOVED;
		}
		return true;
	}

	private void asyncRefresh() {
		Control control = fTreeViewer.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(() -> {
				if (!fTreeViewer.getControl().isDisposed()) {
					fTreeViewer.refresh(true);
					initializeCheckState();
				}
			});
		}
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);

	}

	@Override
	public void modelChanged(IModelChangedEvent event) {

		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
		Object changeObject = event.getChangedObjects()[0];

		if (!(changeObject instanceof IBuildEntry && (((IBuildEntry) changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES) || ((IBuildEntry) changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES) || ((IBuildEntry) changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES) || ((IBuildEntry) changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES))))
			return;

		if ((fParentResource == null && fOriginalResource != null) || (fOriginalResource == null && fParentResource != null)) {
			initializeCheckState();
			return;
		}
		if ((fParentResource == null && fOriginalResource == null) || (event.getChangedProperty() != null && event.getChangedProperty().equals(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES))) {

			return;
		}

		fTreeViewer.setChecked(fParentResource, isChecked);
		fTreeViewer.setGrayed(fOriginalResource, false);
		fTreeViewer.setParentsGrayed(fParentResource, true);
		setParentsChecked(fParentResource);
		fTreeViewer.setGrayed(fParentResource, false);
		if (fParentResource instanceof IFolder) {
			fTreeViewer.setSubtreeChecked(fParentResource, isChecked);
			setChildrenGrayed(fParentResource, false);
		}
		while (!fOriginalResource.equals(fParentResource)) {
			fTreeViewer.setChecked(fOriginalResource, isChecked);
			fOriginalResource = fOriginalResource.getParent();
		}
		fParentResource = null;
		fOriginalResource = null;
	}
}
