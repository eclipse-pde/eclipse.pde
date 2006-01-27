/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.actions.FindReferencesInWorkingSetAction;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.search.AbstractSearchResultPage;
import org.eclipse.pde.internal.ui.search.ManifestEditorOpener;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;


public class DependencyExtentSearchResultPage extends
		AbstractSearchResultPage {
	

	class Sorter extends ViewerSorter {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
		 */
		public int category(Object element) {
			try {
				if (element instanceof IType) {
					if (((IType)element).isClass())
						return 1;
					return 0;
				}
			} catch (JavaModelException e) {
			}
			return 2;
		}
	}
	
	class LabelProvider extends JavaElementLabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.ui.JavaElementLabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element instanceof IPluginObject)
				return PDEPlugin.getDefault().getLabelProvider().getImage(element);
			return super.getImage(element);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.ui.JavaElementLabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof IPluginExtensionPoint) 
				return ((IPluginExtensionPoint) element).getFullId();
			
			if (element instanceof IPluginExtension)
				return ((IPluginExtension)element).getPoint();
			
			if (element instanceof IJavaElement) {
				IJavaElement javaElement = (IJavaElement) element;
				String text =
					super.getText(javaElement)
						+ " - " //$NON-NLS-1$
						+ javaElement
							.getAncestor(IJavaElement.PACKAGE_FRAGMENT)
							.getElementName();
				if (!(javaElement instanceof IType)) {
					IJavaElement ancestor = javaElement.getAncestor(IJavaElement.TYPE);
					if (ancestor == null)
						ancestor = javaElement.getAncestor(IJavaElement.CLASS_FILE);
					if (ancestor == null)
						ancestor = javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (ancestor != null)
						text += "." + ancestor.getElementName(); //$NON-NLS-1$
				}
				return text;
			}
			return super.getText(element);
		}
	}

	public DependencyExtentSearchResultPage() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager mgr) {
		super.fillContextMenu(mgr);
		mgr.add(new Separator());
		JavaSearchActionGroup group = new JavaSearchActionGroup(this);
		group.setContext(new ActionContext(getViewer().getSelection()));
		group.fillContextMenu(mgr);
		addJavaSearchGroup(mgr);
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(getViewer().getSelection()));
		actionGroup.fillContextMenu(mgr);
	}
	
	private void addJavaSearchGroup(IMenuManager mgr) {
		IStructuredSelection ssel = (IStructuredSelection)getViewer().getSelection();
		if (ssel.size() == 1) {
			final Object object = ssel.getFirstElement();
			if (object instanceof IType) {
				mgr.add(new Separator());
				mgr.add(new Action(PDEUIMessages.DependencyExtentSearchResultPage_referencesInPlugin) {
					public void run() {
						DependencyExtentQuery query = (DependencyExtentQuery)getInput().getQuery();
						IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
						IWorkingSet set = manager.createWorkingSet("temp", query.getDirectRoots()); //$NON-NLS-1$
						new FindReferencesInWorkingSetAction(getViewPart().getSite(), new IWorkingSet[] {set}).run((IType)object);
						manager.removeWorkingSet(set);
					}
				});
			}
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchViewPage#showMatch(org.eclipse.search.ui.text.Match, int, int, boolean)
	 */
	protected void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		if (match.getElement() instanceof IPluginObject) {
			ManifestEditorOpener.open(match, activate);
		} else {
			try {
				JavaEditorOpener.open(match, currentOffset, currentLength, activate);
			} catch (PartInitException e) {
			} catch (JavaModelException e) {
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.search.AbstractSearchResultPage#createLabelProvider()
	 */
	protected ILabelProvider createLabelProvider() {
		return new LabelProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.search.AbstractSearchResultPage#createViewerSorter()
	 */
	protected ViewerSorter createViewerSorter() {
		return new Sorter();
	}

}
