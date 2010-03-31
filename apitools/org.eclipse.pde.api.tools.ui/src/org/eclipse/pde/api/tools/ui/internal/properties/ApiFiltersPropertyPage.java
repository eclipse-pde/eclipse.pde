/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import com.ibm.icu.text.MessageFormat;

/**
 * Property page to allow UI edits to the current set of filters for a given project
 * 
 * @since 1.0.0
 */
public class ApiFiltersPropertyPage extends PropertyPage {
	
	/**
	 * Holds an edit change so it can be reverted if cancel is pressed
	 * 
	 * @since 1.1
	 */
	class CommentChange {
		IApiProblemFilter filter = null;
		String comment = null;
		public CommentChange(IApiProblemFilter filter, String orig) {
			this.filter = filter;
			this.comment = orig;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return this.filter.equals(obj);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.filter.hashCode();
		}
	}
	
	/**
	 * Comparator for the viewer to group filters by {@link IElementDescriptor} type
	 */
	static class ApiFilterComparator extends WorkbenchViewerComparator {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
		 */
		public int category(Object element) {
			if(element instanceof IApiProblemFilter) {
				return (int)((IApiProblemFilter) element).getUnderlyingProblem().getCategory();
			}
			return -1;
		}
	}
	
	/**
	 * Content provider for the tree
	 */
	class TreeContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof IResource) {
				try {
					return getFilterStore().getFilters((IResource) parentElement);
				} catch (CoreException e) {
					ApiUIPlugin.log(e);
				}
			}
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if(element instanceof IResource) {
				try {
					return getFilterStore().getFilters((IResource) element).length > 0;
				} catch (CoreException e) {
					ApiUIPlugin.log(e);
				}
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof ArrayList) {
				return ((ArrayList)inputElement).toArray();
			}
			return new Object[0];
		}
		public Object getParent(Object element) {return null;}
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}
	
	TreeViewer fViewer = null;
	Button fRemoveButton;
	Button fEditButton = null;
	Text fCommentText = null;
	private IProject fProject = null;
	ArrayList fDeleteSet = new ArrayList();
	ArrayList fEditSet = new ArrayList();
	private ArrayList fInputset = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(comp, PropertiesMessages.ApiFiltersPropertyPage_55, 2);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 200;
		tree.setLayoutData(gd);
		tree.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.character == SWT.DEL && e.stateMask == 0) {
					handleRemove((IStructuredSelection) fViewer.getSelection());
				}
			}
		});
		fViewer = new TreeViewer(tree);
		fViewer.setContentProvider(new TreeContentProvider());
		fViewer.setLabelProvider(new ApiToolsLabelProvider());
		fViewer.setComparator(new ApiFilterComparator());
		fViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !fDeleteSet.contains(element);
			}
		});
		try {
			IApiFilterStore store = getFilterStore();
			if(store != null) {
				fInputset = new ArrayList(Arrays.asList(store.getResources()));
				fViewer.setInput(fInputset);
			}
		}
		catch(CoreException e) {
			ApiUIPlugin.log(e);
		}
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				int size = ss.size();
				fRemoveButton.setEnabled(size > 0);
				if(size == 1) {
					Object element = ss.getFirstElement();
					if(element instanceof IApiProblemFilter) {
						IApiProblemFilter filter = (IApiProblemFilter) element;
						String comment = filter.getComment();
						fEditButton.setEnabled(true);
						if(comment != null) {
							fCommentText.setText(comment);
						}
						else {
							fCommentText.setText(IApiToolsConstants.EMPTY_STRING);
						}
					}
					else {
						fEditButton.setEnabled(false);
						fCommentText.setText(IApiToolsConstants.EMPTY_STRING);
					}
				}
				else {
					fEditButton.setEnabled(false);
					fCommentText.setText(IApiToolsConstants.EMPTY_STRING);
				}
			}
		});
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				Object o = ((IStructuredSelection)event.getSelection()).getFirstElement();
				if(fViewer.isExpandable(o)) {
					fViewer.setExpandedState(o, !fViewer.getExpandedState(o));
				}
				else {
					if(o instanceof IApiProblemFilter) {
						IApiProblemFilter filter = (IApiProblemFilter) o;
						handleEdit(filter);
					}
				}
			}
		});
		
		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL, 0, 0);
		fEditButton = SWTFactory.createPushButton(bcomp, PropertiesMessages.ApiFiltersPropertyPage_edit_button, null, SWT.LEFT);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ss = (IStructuredSelection) fViewer.getSelection();
				handleEdit((IApiProblemFilter) ss.getFirstElement());
			}
		});
		fEditButton.setEnabled(false);
		fRemoveButton = SWTFactory.createPushButton(bcomp, PropertiesMessages.ApiFiltersPropertyPage_57, null, SWT.LEFT);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ss = (IStructuredSelection) fViewer.getSelection();
				handleRemove(ss);
			}
		});
		fRemoveButton.setEnabled(false);
		
		SWTFactory.createLabel(comp, PropertiesMessages.ApiFiltersPropertyPage_comment, 2);
		fCommentText = SWTFactory.createText(comp, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL, 2, 200, 100, GridData.FILL_HORIZONTAL);
		fCommentText.setEditable(false);
		
		//do initial selections
		if(tree.getItemCount() > 0) {
			TreeItem item = tree.getItem(0);
			fViewer.setSelection(new StructuredSelection(item.getData()), true);
			fViewer.expandToLevel(item.getData(), 1);
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_FILTERS_PROPERTY_PAGE);
		return comp;
	}
	
	/**
	 * Handles the edit button being pressed
	 * @param selection
	 * @since 1.1
	 */
	void handleEdit(IApiProblemFilter filter) {
		String orignal = filter.getComment();
		String comment = orignal;
		InputDialog dialog = new InputDialog(getShell(), PropertiesMessages.ApiFiltersPropertyPage_edit_comment, PropertiesMessages.ApiFiltersPropertyPage_edit_filter_comment, comment, null);
		if(dialog.open() == IDialogConstants.OK_ID) {
			comment = dialog.getValue();
			if(comment != null && comment.length() < 1) {
				comment = null;
			}
			((ApiProblemFilter)filter).setComment(comment);
			CommentChange change = new CommentChange(filter, orignal);
			int idx = fEditSet.indexOf(change);
			if(idx < 0) {
				fEditSet.add(change);
			}
			fViewer.refresh(filter, true);
			fViewer.setSelection(fViewer.getSelection(), true);
		}
	}
	
	/**
	 * Performs the remove
	 * @param selection
	 */
	void handleRemove(IStructuredSelection selection) {
		ArrayList comments = new ArrayList();
		HashSet deletions = collectDeletions(selection, comments);
		boolean refresh = false;
		if(deletions.size() > 0) {
			fDeleteSet.addAll(deletions);
			int[] indexes = getIndexes(selection);
			fViewer.remove(deletions.toArray());
			updateParents();
			refresh = true;
			updateSelection(indexes);
		}
		if(comments.size() > 0) {
			for (int i = 0; i < comments.size(); i++) {
				ApiProblemFilter filter = (ApiProblemFilter) comments.get(i);
				CommentChange change = new CommentChange(filter, filter.getComment());
				int idx = fEditSet.indexOf(filter);
				if(idx < 0) {
					fEditSet.add(change);
				}
				filter.setComment(null);
				refresh = true;
			}
		}
		if(refresh) {
			fViewer.refresh(true);
			fViewer.setSelection(fViewer.getSelection(), true);
		}
	}
	
	/**
	 * Collects the indexes of the first item in the current selection
	 * @param selection
	 * @return an array of indexes (parent, child) of the first item in the current selection
	 */
	private int[] getIndexes(IStructuredSelection selection) {
		int[] indexes = new int[] {0,0};
		TreeSelection tsel = (TreeSelection) selection;
		TreePath path = tsel.getPaths()[0];
		TreeItem parent = (TreeItem) fViewer.testFindItem(path.getFirstSegment());
		if(parent != null) {
			Tree tree = fViewer.getTree();
			//found parent
			indexes[0] = tree.indexOf(parent);
			TreeItem item = (TreeItem) fViewer.testFindItem(path.getLastSegment());
			if(item != null) {
				indexes[1] = parent.indexOf(item);
			}
		}
		return indexes;
	}
	
	/**
	 * Updates the selection in the viewer based on the given indexes.
	 * If there is no item to update at the given indexes then the next logical child is taken, else the parent
	 * is selected, else no selection is made
	 * @param indexes
	 */
	private void updateSelection(int[] indexes) {
		Tree tree = fViewer.getTree();
		TreeItem parent = null;
		if(tree.getItemCount() == 0) {
			return;
		}
		if(indexes[0] < tree.getItemCount()) {
			TreeItem child = null;
			parent = tree.getItem(indexes[0]);
			int childcount = parent.getItemCount();
			if(childcount < 1 || indexes[1] < 0) {
				fViewer.setSelection(new StructuredSelection(parent.getData()));
				return;
			}
			else if (indexes[1] < childcount){
				child = parent.getItem(indexes[1]);
			}
			else {
				child = parent.getItem(childcount-1);
			}
			fViewer.setSelection(new StructuredSelection(child.getData()));
		}
		else {
			parent = tree.getItem(tree.getItemCount()-1);
			fViewer.setSelection(new StructuredSelection(parent.getData()));
		}
	}
	
	/**
	 * Cleans up empty parents once a deletion update has been done
	 * for the parents that have incrementally had all their children removed
	 */
	private void updateParents() {
		Tree tree = fViewer.getTree();
		TreeItem[] items = tree.getItems();
		for(int i = 0; i < items.length; i++) {
			if(items[i].getItems().length < 1) {
				fInputset.remove(items[i].getData());
			}
		}
	}
	
	/**
	 * Collects all of the elements to be deleted
	 * @param selection
	 * @param comments a collector for filters that will have their comments removed
	 * @return the set of elements to be added to the change set for deletion
	 */
	private HashSet collectDeletions(IStructuredSelection selection, ArrayList comments) {
		HashSet filters = new HashSet();
		Object node = null;
		Object[] children = null;
		for(Iterator iter = selection.iterator(); iter.hasNext();) {
			node = iter.next();
			if(node instanceof IResource) {
				children = ((TreeContentProvider)fViewer.getContentProvider()).getChildren(node);
				filters.addAll(Arrays.asList(children));
				fInputset.remove(node);
			}
			else if(node instanceof IApiProblemFilter){
				filters.add(node);
			}
			else if(node instanceof String) {
				TreeItem item = (TreeItem) fViewer.testFindItem(node);
				if(item != null) {
					comments.add(item.getParentItem().getData());
				}
			}
		}
		return filters;
	}
	
	/**
	 * @return the backing project for this page, or <code>null</code> if this page was 
	 * somehow opened without a project
	 */
	private IProject getProject() {
		if(fProject == null) {
			fProject = (IProject) getElement().getAdapter(IProject.class);
		}
		return fProject;
	}
	
	/**
	 * @return the {@link IApiFilterStore} from the backing project
	 * @throws CoreException
	 */
	IApiFilterStore getFilterStore() throws CoreException {
		IProject project  = getProject();
		IApiFilterStore store = null;
		if(project != null) {
			IApiComponent component = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline().getApiComponent(project);
			if(component != null) {
				return component.getFilterStore();
			}
		}
		return store;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	public boolean performCancel() {
		//revert changes
		for (int i = 0; i < fEditSet.size(); i++) {
			CommentChange change = (CommentChange) fEditSet.get(i);
			((ApiProblemFilter)change.filter).setComment(change.comment);
		}
		fEditSet.clear();
		return super.performCancel();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		try {
			boolean needsbuild = false;
			if(fDeleteSet.size() > 0) {
				IApiProblemFilter[] apiProblemFilters = (IApiProblemFilter[]) fDeleteSet.toArray(new IApiProblemFilter[fDeleteSet.size()]);
				getFilterStore().removeFilters(apiProblemFilters);
				// we want to make sure that we rebuild only applicable types
				for (int i = 0, max = apiProblemFilters.length; i < max; i++) {
					IApiProblemFilter filter = apiProblemFilters[i];
					IApiProblem apiProblem = filter.getUnderlyingProblem();
					if (apiProblem != null) {
						String resourcePath = apiProblem.getResourcePath();
						if (resourcePath != null) {
							IResource resource = fProject.findMember(resourcePath);
							if (resource != null) {
								Util.touchCorrespondingResource(fProject, resource, apiProblem.getTypeName());
							}
						}
					}
				}
				needsbuild = true;
			} else if(fEditSet.size() > 0) {
				ApiFilterStore store = (ApiFilterStore) getFilterStore();
				store.needsSaving();
				store.persistApiFilters();
			}
			if(needsbuild) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				if (!workspace.isAutoBuilding()) {
					if(MessageDialog.openQuestion(getShell(), PropertiesMessages.ApiFiltersPropertyPage_58, 
							MessageFormat.format(PropertiesMessages.ApiFiltersPropertyPage_59, new String[] {fProject.getName()}))) {
						Util.getBuildJob(new IProject[] {fProject}, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
					}
				}
			}
			fEditSet.clear();
			fDeleteSet.clear();
		}
		catch(CoreException e) {
			ApiUIPlugin.log(e);
		} catch(OperationCanceledException e) {
			// ignore
		}
		return super.performOk();
	}
}
