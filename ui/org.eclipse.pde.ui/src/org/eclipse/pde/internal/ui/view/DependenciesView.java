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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.builders.DependencyLoop;
import org.eclipse.pde.internal.builders.DependencyLoopFinder;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.plugin.LoopDialog;
import org.eclipse.swt.custom.BusyIndicator;
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

	class ShowLoopsAction extends Action {

		public ShowLoopsAction() {
			super("", AS_PUSH_BUTTON); //$NON-NLS-1$
			setText(PDEPlugin
					.getResourceString("DependenciesView.ShowLoopsAction.label")); //$NON-NLS-1$
			setDescription(PDEPlugin
					.getResourceString("DependenciesView.ShowLoopsAction.description")); //$NON-NLS-1$
			setToolTipText(PDEPlugin
					.getResourceString("DependenciesView.ShowLoopsAction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(PDEPluginImages.DESC_DEP_LOOP);
			setDisabledImageDescriptor(PDEPluginImages.DESC_DEP_LOOP_DISABLED);
			setEnabled(false);
		}

		/*
		 * @see Action#actionPerformed
		 */
		public void run() {
			LoopDialog dialog = new LoopDialog(PDEPlugin
					.getActiveWorkbenchShell(), fLoops);
			dialog.open();
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

	private static final DependencyLoop[] NO_LOOPS = new DependencyLoop[0];
	
	private Map fPagesToParts;

	private Map fPartsToPages;

	private Object fInput;

	private Preferences fPreferences = PDEPlugin.getDefault()
			.getPluginPreferences();

	private ShowCalleesAction fShowCallees;

	private ShowCallersAction fShowCallers;

	private ShowListAction fShowList;

	private ShowTreeAction fShowTree;
	
	private ShowLoopsAction fShowLoops;

	// history of input elements (as Strings). No duplicates
	private ArrayList fInputHistory;
	
	private DependencyLoop[] fLoops;
	
	private HistoryDropDownAction fHistoryDropDownAction;
	/**
	 * 
	 */
	public DependenciesView() {
		super();
		fPartsToPages = new HashMap(4);
		fPagesToParts = new HashMap(4);
		fInputHistory= new ArrayList();
		fLoops = NO_LOOPS;
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
		manager.add(new Separator("history")); //$NON-NLS-1$
		manager.add(fShowLoops);
		manager.add(fHistoryDropDownAction);
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
		
		fShowLoops = new ShowLoopsAction();
		fShowLoops.setEnabled(fLoops != NO_LOOPS);
		
		fHistoryDropDownAction= new HistoryDropDownAction(this);
		fHistoryDropDownAction.setEnabled(!fInputHistory.isEmpty());
		
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
		if(memento == null)
			return;
		String id = memento.getString(MEMENTO_KEY_INPUT);
		if (id != null) {
			IPluginModelBase plugin = PDECore.getDefault().getModelManager().findModel(id);
			if (plugin != null) {
				fInput = plugin;
				addHistoryEntry(id);
				findLoops();
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
		if (object != null && !object.equals(fInput)) {
			if(object instanceof IPluginModelBase){
				String id =((IPluginModelBase)object).getPluginBase().getId();
				addHistoryEntry(id);
			}
		}
		updateInput(object);
	}

	private void updateInput(Object object) {
		fInput = object;
		findLoops();
		((DependenciesViewPage) getCurrentPage()).setInput(object);
	}

	/**
	 * 
	 */
	private void findLoops() {
		fLoops = NO_LOOPS;
		if (fInput != null && fInput instanceof IPluginModel) {
			BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell()
					.getDisplay(), new Runnable() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					IPlugin plugin = ((IPluginModel) fInput).getPlugin();
					DependencyLoop[] loops = DependencyLoopFinder
							.findLoops(plugin);
					if (loops.length > 0) {
						fLoops = loops;
					}
				}
			});
		}
		if(fShowLoops != null)
			fShowLoops.setEnabled(fLoops != NO_LOOPS);
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
			if(fLoops != NO_LOOPS){
				title = title + " " + PDEPlugin.getResourceString( //$NON-NLS-1$
						"DependenciesView.cycles.title"); //$NON-NLS-1$
			}
			setContentDescription(title); //$NON-NLS-1$
		}
		setTitleToolTip(getTitle());
	}
	
	/**
	 * Adds the entry if new. Inserted at the beginning of the history entries list.
	 * @param entry The new entry
	 */		
	private void addHistoryEntry(String entry) {
		if (fInputHistory.contains(entry)) {
			fInputHistory.remove(entry);
		}
		fInputHistory.add(0, entry);
		if (fHistoryDropDownAction != null)
			fHistoryDropDownAction.setEnabled(true);
	}
	
	private void updateHistoryEntries() {
		for (int i= fInputHistory.size() - 1; i >= 0; i--) {
			String type= (String) fInputHistory.get(i);
			if (PDECore.getDefault().getModelManager().findModel(type)==null) {
				fInputHistory.remove(i);
			}
		}
		if (fHistoryDropDownAction != null)
			fHistoryDropDownAction.setEnabled(!fInputHistory.isEmpty());
	}
	
	/**
	 * Goes to the selected entry, without updating the order of history entries.
	 * @param entry The entry to open
	 */	
	public void gotoHistoryEntry(String entry) {
		if (fInputHistory.contains(entry)) {	
			updateInput(PDECore.getDefault().getModelManager().findModel(entry));
		}
	}	
	
	/**
	 * Gets all history entries.
	 * @return All history entries
	 */
	public String[] getHistoryEntries() {
		if (fInputHistory.size() > 0) {
			updateHistoryEntries();
		}
		return (String[]) fInputHistory.toArray(new String[fInputHistory.size()]);
	}
	
	/**
	 * Sets the history entries
	 * @param elems The history elements to set
	 */
	public void setHistoryEntries(String[] elems) {
		fInputHistory.clear();
		for (int i= 0; i < elems.length; i++) {
			fInputHistory.add(elems[i]);
		}
		updateHistoryEntries();
	}
	/**
	 * @return Returns the fInput.
	 */
	public String getInput() {
		if(fInput!=null){
			return ((IPluginModelBase)fInput).getPluginBase().getId();
		}
		return null;
	}
}
