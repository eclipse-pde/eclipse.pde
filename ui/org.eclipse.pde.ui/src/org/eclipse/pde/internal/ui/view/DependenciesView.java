/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

public class DependenciesView extends PageBookView implements
		IPreferenceConstants, IHelpContextIds {
	
	static class DummyPart implements IWorkbenchPart {
		public void addPropertyListener(IPropertyListener listener) {/* dummy */
		}

		public void createPartControl(Composite parent) {/* dummy */
		}

		public void dispose() {/* dummy */
		}

		public Object getAdapter(Class adapter) {
			return null;
		}

		public IWorkbenchPartSite getSite() {
			return null;
		}

		public String getTitle() {
			return null;
		}

		public Image getTitleImage() {
			return null;
		}

		public String getTitleToolTip() {
			return null;
		}

		public void removePropertyListener(IPropertyListener listener) {/* dummy */
		}

		public void setFocus() {/* dummy */
		}
	}

	class ShowCalleesAction extends Action {

		public ShowCalleesAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEPlugin
					.getResourceString("DependenciesView.ShowCalleesAction.label")); //$NON-NLS-1$
			setDescription(PDEPlugin
					.getResourceString("DependenciesView.ShowCalleesAction.description")); //$NON-NLS-1$
			setToolTipText(PDEPlugin
					.getResourceString("DependenciesView.ShowCalleesAction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(PDEPluginImages.DESC_CALLEES);
			setDisabledImageDescriptor(PDEPluginImages.DESC_CALLEES_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_CALLERS, false);
				setViewType(false);
			}
		}
	}

	class ShowCallersAction extends Action {
		public ShowCallersAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEPlugin
					.getResourceString("DependenciesView.ShowCallersAction.label")); //$NON-NLS-1$
			setDescription(PDEPlugin
					.getResourceString("DependenciesView.ShowCallersAction.description")); //$NON-NLS-1$
			setToolTipText(PDEPlugin
					.getResourceString("DependenciesView.ShowCallersAction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(PDEPluginImages.DESC_CALLERS);
			setDisabledImageDescriptor(PDEPluginImages.DESC_CALLERS_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_CALLERS, true);
				setViewType(true);
			}
		}
	}

	class ShowListAction extends Action {
		public ShowListAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEPlugin
					.getResourceString("DependenciesView.ShowListAction.label")); //$NON-NLS-1$
			setDescription(PDEPlugin
					.getResourceString("DependenciesView.ShowListAction.description")); //$NON-NLS-1$
			setToolTipText(PDEPlugin
					.getResourceString("DependenciesView.ShowListAction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(PDEPluginImages.DESC_FLAT_LAYOUT);
			setDisabledImageDescriptor(PDEPluginImages.DESC_FLAT_LAYOUT_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_LIST, true);
				setPresentation(true);
			}
		}
	}

	class ShowTreeAction extends Action {

		public ShowTreeAction() {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			setText(PDEPlugin
					.getResourceString("DependenciesView.ShowTreeAction.label")); //$NON-NLS-1$
			setDescription(PDEPlugin
					.getResourceString("DependenciesView.ShowTreeAction.description")); //$NON-NLS-1$
			setToolTipText(PDEPlugin
					.getResourceString("DependenciesView.ShowTreeAction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(PDEPluginImages.DESC_HIERARCHICAL_LAYOUT);
			setDisabledImageDescriptor(PDEPluginImages.DESC_HIERARCHICAL_LAYOUT_DISABLED);
		}

		/*
		 * @see Action#actionPerformed
		 */
		public void run() {
			if (isChecked()) {
				fPreferences.setValue(DEPS_VIEW_SHOW_LIST, false);
				setPresentation(false);
			}
		}
	}

	protected static final IWorkbenchPart PART_CALLEES_LIST = new DummyPart();

	protected static final IWorkbenchPart PART_CALLEES_TREE = new DummyPart();

	protected static final IWorkbenchPart PART_CALLERS_LIST = new DummyPart();

	protected static final IWorkbenchPart PART_CALLERS_TREE = new DummyPart();

	public static final String TREE_ACTION_GROUP = "tree"; //$NON-NLS-1$
	
	protected static final String MEMENTO_KEY_INPUT = "inputPluginId"; //$NON-NLS-1$

	private Map fPagesToParts;

	private Map fPartsToPages;

	private Object fInput;

	private Preferences fPreferences = PDEPlugin.getDefault()
			.getPluginPreferences();

	private ShowCalleesAction fShowCallees;

	private ShowCallersAction fShowCallers;

	private ShowListAction fShowList;

	private ShowTreeAction fShowTree;

	/**
	 * 
	 */
	public DependenciesView() {
		super();
		fPartsToPages = new HashMap(4);
		fPagesToParts = new HashMap(4);
	}

	private void contributeToActionBars(IActionBars actionBars) {
		contributeToLocalToolBar(actionBars.getToolBarManager());
		actionBars.updateActionBars();
	}

	private void contributeToLocalToolBar(IToolBarManager manager) {
		manager.add(new Separator(TREE_ACTION_GROUP));
		manager.add(new Separator("type")); //$NON-NLS-1$
		manager.appendToGroup("type", fShowCallees); //$NON-NLS-1$
		manager.appendToGroup("type", fShowCallers); //$NON-NLS-1$
		manager.add(new Separator("presentation")); //$NON-NLS-1$
		manager.appendToGroup("presentation", fShowTree); //$NON-NLS-1$
		manager.appendToGroup("presentation", fShowList); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#createDefaultPage(org.eclipse.ui.part.PageBook)
	 */
	protected IPage createDefaultPage(PageBook book) {
		if (fPreferences.getBoolean(DEPS_VIEW_SHOW_CALLERS)) {
			if (fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST)) {
				return createPage(PART_CALLERS_LIST);

			}
			return createPage(PART_CALLERS_TREE);
		}
		if (fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST)) {
			return createPage(PART_CALLEES_LIST);

		}
		return createPage(PART_CALLEES_TREE);
	}

	/**
	 * part of the part constants
	 */
	private DependenciesViewPage createPage(IWorkbenchPart part) {
		DependenciesViewPage page;
		if (part == PART_CALLEES_TREE) {
			page = new DependenciesViewTreePage(this,
					new CalleesTreeContentProvider(this));
		} else if (part == PART_CALLEES_LIST) {
			page = new DependenciesViewListPage(this,
					new CalleesListContentProvider(this));
		} else if (part == PART_CALLERS_TREE) {
			page = new DependenciesViewTreePage(this,
					new CallersTreeContentProvider(this));
		} else {
			page = new DependenciesViewListPage(this,
					new CallersListContentProvider(this));
		}

		initPage(page);
		page.createControl(getPageBook());
		fPartsToPages.put(part, page);
		fPagesToParts.put(page, part);
		return page;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		fShowCallees = new ShowCalleesAction();
		fShowCallees.setChecked(!fPreferences
				.getBoolean(DEPS_VIEW_SHOW_CALLERS));
		fShowCallers = new ShowCallersAction();
		fShowCallers
				.setChecked(fPreferences.getBoolean(DEPS_VIEW_SHOW_CALLERS));

		fShowTree = new ShowTreeAction();
		fShowTree.setChecked(!fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST));
		fShowList = new ShowListAction();
		fShowList.setChecked(fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST));
		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);

		WorkbenchHelp.setHelp(parent, IHelpContextIds.DEPENDENCIES_VIEW);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#doCreatePage(org.eclipse.ui.IWorkbenchPart)
	 */
	protected PageRec doCreatePage(IWorkbenchPart part) {
		IPageBookViewPage page = (IPageBookViewPage) fPartsToPages.get(part);
		if (page == null && !fPartsToPages.containsKey(part)) {
			page = createPage(part);
		}
		if (page != null) {
			return new PageRec(part, page);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#doDestroyPage(org.eclipse.ui.IWorkbenchPart,
	 *      org.eclipse.ui.part.PageBookView.PageRec)
	 */
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IPage page = pageRecord.page;
		page.dispose();
		pageRecord.dispose();

		// empty cross-reference cache
		fPartsToPages.remove(part);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#getBootstrapPart()
	 */
	protected IWorkbenchPart getBootstrapPart() {
		if (fPreferences.getBoolean(DEPS_VIEW_SHOW_CALLERS)) {
			if (fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST)) {
				return PART_CALLERS_LIST;

			}
			return PART_CALLERS_TREE;
		}
		if (fPreferences.getBoolean(DEPS_VIEW_SHOW_LIST)) {
			return PART_CALLEES_LIST;

		}
		return PART_CALLEES_TREE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite,
	 *      org.eclipse.ui.IMemento)
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		String id = memento.getString(MEMENTO_KEY_INPUT);
		if (id != null) {
			IPlugin plugin = PDECore.getDefault().findPlugin(id);
			if (plugin != null) {
				fInput = plugin.getModel();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#isImportant(org.eclipse.ui.IWorkbenchPart)
	 */
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof DummyPart;
	}

	public void openTo(Object object) {
		fInput = object;
		((DependenciesViewPage) getCurrentPage()).setInput(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		if (fInput != null && fInput instanceof IPluginModelBase) {
			String inputPluginId = ((IPluginModelBase) fInput).getPluginBase()
					.getId();
			memento.putString(MEMENTO_KEY_INPUT, inputPluginId);
		}
	}

	void setPresentation(boolean listNotTree) {
		IWorkbenchPart currentPart = getCurrentContributingPart();
		if (listNotTree) {
			if (currentPart == PART_CALLEES_TREE) {
				partActivated(PART_CALLEES_LIST);
			} else if (currentPart == PART_CALLERS_TREE) {
				partActivated(PART_CALLERS_LIST);
			}

		} else {
			if (currentPart == PART_CALLEES_LIST) {
				partActivated(PART_CALLEES_TREE);
			} else if (currentPart == PART_CALLERS_LIST) {
				partActivated(PART_CALLERS_TREE);
			}

		}
	}

	void setViewType(boolean callers) {
		IWorkbenchPart currentPart = getCurrentContributingPart();
		if (callers) {
			if (currentPart == PART_CALLEES_TREE) {
				partActivated(PART_CALLERS_TREE);
			} else if (currentPart == PART_CALLEES_LIST) {
				partActivated(PART_CALLERS_LIST);
			}

		} else {
			if (currentPart == PART_CALLERS_TREE) {
				partActivated(PART_CALLEES_TREE);
			} else if (currentPart == PART_CALLERS_LIST) {
				partActivated(PART_CALLEES_LIST);
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.PageBookView#showPageRec(org.eclipse.ui.part.PageBookView.PageRec)
	 */
	protected void showPageRec(PageRec pageRec) {
		IPage p = pageRec.page;
		((DependenciesViewPage) p).setInput(fInput);
		super.showPageRec(pageRec);
		updateTitle(fInput);
	}

	void updateTitle(Object newInput) {
		if (newInput == null
				|| newInput.equals(PDECore.getDefault().getModelManager())) {
			IConfigurationElement config = getConfigurationElement();
			if (config == null)
				return;
			String viewName = config.getAttribute("name"); //$NON-NLS-1$
			setContentDescription(viewName);
		} else {
			String name = PDEPlugin.getDefault().getLabelProvider().getText(
					newInput);
			String title;
			if (getCurrentContributingPart() == PART_CALLEES_TREE) {
				title = PDEPlugin.getFormattedMessage(
						"DependenciesView.callees.tree.title", name); //$NON-NLS-1$
			} else if (getCurrentContributingPart() == PART_CALLEES_LIST) {
				title = PDEPlugin.getFormattedMessage(
						"DependenciesView.callees.list.title", name); //$NON-NLS-1$
			} else if (getCurrentContributingPart() == PART_CALLERS_TREE) {
				title = PDEPlugin.getFormattedMessage(
						"DependenciesView.callers.tree.title", name); //$NON-NLS-1$
			} else {
				title = PDEPlugin.getFormattedMessage(
						"DependenciesView.callers.list.title", name); //$NON-NLS-1$
			}
			setContentDescription(title); //$NON-NLS-1$
		}
		setTitleToolTip(getTitle());
	}
	
}
