/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public abstract class BuildContentsSection
	extends TableSection
	implements IModelChangedListener, IResourceChangeListener, IResourceDeltaVisitor {
	protected CheckboxTreeViewer treeViewer;
	private boolean doRefresh = false;

	protected Control sectionControl;
	protected IProject project;
	protected IBuildModel buildModel;
	public class TreeContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IProject) {
				try {
					return ((IProject) parent).members();
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			} 
			return new Object[0];
		}
		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
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
				ArrayList results = new ArrayList();

				for (int i = 0; i < members.length; i++) {
					if ((members[i].getType() == IResource.FOLDER)) {
						results.add(members[i]);
					}
				}
				return results.toArray();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			return new Object[0];
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element != null && element instanceof IResource) {
				return ((IResource) element).getParent();
			}
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof IFolder)
				return getChildren(element).length > 0;
			return false;
		}

	}

	public BuildContentsSection(BuildPage page) {
		super(page, new String[0]);

		buildModel = (IBuildModel) getFormPage().getModel();
		if (buildModel.getUnderlyingResource() != null)
			project = buildModel.getUnderlyingResource().getProject();

		PDEPlugin.getWorkspace().addResourceChangeListener(this);
	}

	public Composite createClient(
		final Composite parent,
		FormWidgetFactory factory) {

		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		container.setLayout(layout);

		treeViewer = new CheckboxTreeViewer(createTree(container, factory));
		treeViewer.setContentProvider(new TreeContentProvider());
		treeViewer.setLabelProvider(new WorkbenchLabelProvider());
		treeViewer.setAutoExpandLevel(0);
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final Object element = event.getElement();
				BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
					public void run() {
						if (element instanceof IFile) {
							IFile file = (IFile) event.getElement();
							handleCheckStateChanged(file, event.getChecked());
						} else if (element instanceof IFolder) {
							IFolder folder = (IFolder) event.getElement();
							handleCheckStateChanged(folder, event.getChecked());
						}
					}
				});
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		gd.widthHint = 100;
		treeViewer.getTree().setLayoutData(gd);
		initialize();
		initializeCheckState();
		factory.paintBordersFor(container);

		return container;
	}

	private Tree createTree(Composite parent, FormWidgetFactory factory) {
		Tree tree = new Tree(parent, SWT.CHECK);
		tree.setBackground(factory.getBackgroundColor());
		tree.setForeground(factory.getForegroundColor());
		factory.hookDeleteListener(tree);
		return tree;
	}

	public void disableSection() {
		treeViewer.getTree().setEnabled(false);
		if (getSectionControl() != null)
			getSectionControl().setEnabled(false);
	}

	public void enableSection() {
		treeViewer.getTree().setEnabled(true);
		if (getSectionControl() != null)
			getSectionControl().setEnabled(true);
	}

	protected void handleCheckStateChanged(
		IResource resource,
		boolean checked) {

		treeViewer.setChecked(resource, checked);
		treeViewer.setParentsGrayed(resource, true);
		treeViewer.setGrayed(resource, false);

		boolean wasTopParentChecked =
			treeViewer.getChecked(resource.getParent());

		if (checked) {
			setParentsChecked(resource);
		} else {
			resource = handleAllUnselected(resource);
		}

		if (resource instanceof IFolder) {
			IFolder folder = (IFolder) resource;
			treeViewer.setSubtreeChecked(folder, checked);
			setChildrenGrayed(folder, false);
			handleBuildCheckStateChange(folder, checked, wasTopParentChecked);
		} else {
			handleBuildCheckStateChange(resource, checked, wasTopParentChecked);
		}
	}

	protected IResource handleAllUnselected(IResource resource) {
		IResource parent = resource.getParent();
		if (parent == resource.getProject()) {
			return resource;
		}

		try {
			boolean uncheck = true;
			IResource[] members = ((IFolder) parent).members();
			for (int i = 0; i < members.length; i++) {
				if (treeViewer.getChecked(members[i]))
					uncheck = false;
			}
			if (uncheck) {
				treeViewer.setChecked(parent, false);
				return handleAllUnselected(parent);
			}
			return resource;

		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}

	}

	protected void setChildrenGrayed(IResource folder, boolean isGray) {
		treeViewer.setGrayed(folder, isGray);

		if (((TreeContentProvider) treeViewer.getContentProvider())
			.hasChildren(folder)) {
			Object[] members =
				((TreeContentProvider) treeViewer.getContentProvider())
						.getFolderChildren(folder);
			for (int i = 0; i < members.length; i++) {
				setChildrenGrayed((IFolder) members[i], isGray);
			}
		}
	}
	protected void setParentsChecked(IResource resource) {
		if (resource.getParent() != resource.getProject()) {
			treeViewer.setChecked(resource.getParent(), true);
			setParentsChecked(resource.getParent());
		}
	}

	/**
	 * removes all child resources of the specified folder from build entries
	 * @param folder - current folder being modified in tree
	 * 
	 * note:  does not remove folder itself
	 */
	protected abstract void deleteFolderChildrenFromEntries(IFolder folder);

	protected void initializeCheckState() {
		uncheckAll();
	}

	protected void initializeCheckState(
		IBuildEntry includes,
		IBuildEntry excludes) {
		Vector fileExt = new Vector();
		String[] inclTokens, exclTokens = new String[0];
		if (project == null || includes == null)
			return;

		inclTokens = includes.getTokens();

		if (excludes != null)
			exclTokens = excludes.getTokens();

		Set temp = new TreeSet();
		for (int i = 0; i < inclTokens.length; i++)
			temp.add(inclTokens[i]);

		for (int i = 0; i < exclTokens.length; i++)
			temp.add(exclTokens[i]);

		Iterator iter = temp.iterator();

		while (iter.hasNext()) {
			String resource = iter.next().toString();
			boolean isIncluded = includes.contains(resource);
			if (resource.lastIndexOf(Path.SEPARATOR)
				== resource.length() - 1) {
				IFolder folder = project.getFolder(resource);
				treeViewer.setSubtreeChecked(folder, isIncluded);
				treeViewer.setParentsGrayed(folder, true);
				if (isIncluded && folder.exists()) {
					setParentsChecked(folder);
					treeViewer.setGrayed(folder, false);
				}
			} else if (resource.startsWith("*.")) {
				if (isIncluded)
					fileExt.add(resource.substring(2));
			} else {
				IFile file = project.getFile(resource);
				treeViewer.setChecked(file, isIncluded);
				treeViewer.setParentsGrayed(file, true);
				if (isIncluded && file.exists()) {
					treeViewer.setGrayed(file, false);
					setParentsChecked(file);
				}
			}
		}

		if (fileExt.size() == 0)
			return;

		try {
			IResource[] members = project.members();
			for (int i = 0; i < members.length; i++) {
				if (!(members[i] instanceof IFolder)
					&& (fileExt.contains(members[i].getFileExtension()))) {
					treeViewer.setChecked(
						(IFile) members[i],
						includes.contains(
							"*." + members[i].getFileExtension()));
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	protected abstract void handleBuildCheckStateChange(
		IResource resource,
		boolean checked,
		boolean wasTopParentChecked);

	protected void handleCheck(
		IBuildEntry includes,
		IBuildEntry excludes,
		String resourceName,
		IResource resource,
		boolean wasTopParentChecked,
		String PROPERTY_INCLUDES) {
			
		try {
			if (includes == null) {
				includes =	buildModel.getFactory().createEntry(PROPERTY_INCLUDES);
				IBuild build = buildModel.getBuild();
				build.add(includes);
			}
			if ((!wasTopParentChecked
				&& !includes.contains(resourceName))
				|| isValidIncludeEntry(includes,excludes,resource,resourceName)){
				includes.addToken(resourceName);
			}
			if (excludes != null && excludes.contains(resourceName))
				excludes.removeToken(resourceName);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected boolean isValidIncludeEntry(IBuildEntry includes, IBuildEntry excludes, IResource resource, String resourceName){
		if (excludes == null)
			return true;

		IPath resPath = resource.getProjectRelativePath();
		  
		while (resPath.segmentCount()>1){
			resPath = resPath.removeLastSegments(1);
			if (includes.contains(resPath.toString() + Path.SEPARATOR))
				return false;
			else if (excludes!=null && excludes.contains(resPath.toString() + Path.SEPARATOR))
				return true;
		}
		return !excludes.contains(resourceName);
	}
	protected void handleUncheck(IBuildEntry includes, IBuildEntry excludes, String resourceName, IResource resource, String PROPERTY_EXCLUDES){
		try {
			if(treeViewer.getChecked(resource.getParent())){
				if (excludes == null){
					excludes = buildModel.getFactory().createEntry(PROPERTY_EXCLUDES);
					IBuild build = buildModel.getBuild();
					build.add(excludes);
				}
				if (!excludes.contains(resourceName) && (includes!=null ? !includes.contains(resourceName) : true))
					excludes.addToken(resourceName);
			}
			if (includes !=null){
				if (includes.contains(resourceName))
					includes.removeToken(resourceName);
				if (includes.contains("*." + resource.getFileExtension())) {
					IResource[] members = project.members();
					for (int i = 0; i<members.length; i++){
						if (!(members[i] instanceof IFolder) &&
							!members[i].getName().equals(resource.getName())
							&& (resource.getFileExtension().equals(members[i].getFileExtension()))) {
								includes.addToken(members[i].getName());
						}

						IBuildEntry[] libraries = BuildUtil.getBuildLibraries(buildModel.getBuild().getBuildEntries());
						if (resource.getFileExtension().equals("jar") && libraries.length!=0){
							for (int j=0; j<libraries.length; j++){
								String libName = libraries[j].getName().substring(7);
								IPath path = project.getFile(libName).getProjectRelativePath();
								if (path.segmentCount()==1 && !includes.contains(libName) && !libName.equals(resource.getName()))
									includes.addToken(libName);
							}
						}
					}
					includes.removeToken("*." + resource.getFileExtension());
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected String getResourceFolderName(String resourceName){
		return resourceName + Path.SEPARATOR;
	}
	
	/**
	 * @param resource - file/folder being modified in tree
	 * @param resourceName - name file/folder 
	 * @return relative path of folder if resource is folder, otherwise, return resourceName
	 */
	protected String handleResourceFolder(IResource resource, String resourceName){
		if (resource instanceof IFolder){
			deleteFolderChildrenFromEntries((IFolder)resource);
			return getResourceFolderName(resourceName);
		}
		return resourceName;
	}

	public void initialize() {
		if (treeViewer.getInput() == null) {
			treeViewer.setUseHashlookup(true);
			treeViewer.setInput(project);
		}
		buildModel.addModelChangedListener(this);
	}
	
	public void dispose() {
		buildModel.removeModelChangedListener(this);
		PDEPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	protected void deleteEmptyEntries() {
		IBuild build = buildModel.getBuild();
		IBuildEntry[] entries =
			{
				build.getEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES),
				build.getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES),
				build.getEntry(IXMLConstants.PROPERTY_SRC_EXCLUDES),
				build.getEntry(IXMLConstants.PROPERTY_SRC_INCLUDES)};

		try {
			for (int i = 0; i < entries.length; i++) {
				if (entries[i] != null && entries[i].getTokens().length == 0)
					build.remove(entries[i]);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	public CheckboxTreeViewer getTreeViewer() {
		return treeViewer;
	}

	public void setSectionControl(Control control) {
		sectionControl = control;
	}

	public Control getSectionControl() {
		return sectionControl;
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			initializeCheckState();
		}
	}

	public void uncheckAll() {
		treeViewer.setCheckedElements(new Object[0]);
	}

	protected void removeChildren(IBuildEntry entry, String parentFolder) {
		try {
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].indexOf(Path.SEPARATOR) != -1
						&& tokens[i].startsWith(parentFolder) &&
						!tokens[i].equals(parentFolder)) {
						entry.removeToken(tokens[i]);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
		doRefresh = false;
		IResourceDelta delta = event.getDelta();
		try {
			if (delta != null)
				delta.accept(this);
			if (doRefresh) {
				asyncRefresh();
				doRefresh = false;
			}
		} catch (CoreException e) {
		}
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		if ((resource instanceof IFile || resource instanceof IFolder)
			&& resource.getProject().equals(
				buildModel.getUnderlyingResource().getProject())) {
			if (delta.getKind() == IResourceDelta.ADDED
				|| delta.getKind() == IResourceDelta.REMOVED) {
				doRefresh = true;
				return false;
			}
		}
		return true;
	}

	private void asyncRefresh() {
		if (!getSectionControl().isDisposed()) {
			getSectionControl().getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!treeViewer.getControl().isDisposed()) {
						treeViewer.refresh(true);
						initializeCheckState();
					}
				}
			});
		}
	}
}
