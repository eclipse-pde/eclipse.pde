package org.eclipse.pde.internal.pluginsview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.wizards.ListUtil;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import java.util.Iterator;
import org.eclipse.pde.internal.base.model.plugin.IPluginModelBase;

public class PluginsView extends ViewPart implements ISelectionListener, IModelProviderListener {
	private IDialogSettings settingsSection;
	private TreeViewer treeViewer;
	private Action propertiesAction;
	private Action showVersionsAction;
	private Action showFullNamesAction;
	private Action enableAction;
	private Action sortAction;
	private ViewerSorter sorter;

	private static final String SETTINGS_SECTION = "org.eclipse.pde.pluginsView";
	private static final String SETTING_SHOW_VERSION = "showVersion";
	private static final String SETTING_SORT = "sort";
	private static final String SETTING_SHOW_FULL_NAMES = "showFullNames";

	private static final String KEY_SHOW_VERSION = "PluginsView.showVersion.label";
	private static final String KEY_SHOW_FULL_NAMES =
		"PluginsView.showFullNames.label";
	private static final String KEY_SORT =
		"PluginsView.sort.label";
	private static final String KEY_ENABLE =
		"PluginsView.enable.label";

	public PluginsView() {
		super();
		settingsSection = getSettingsSection();
		sorter = new PluginsViewSorter();
	}

	public void createPartControl(Composite parent) {
		Tree tree = new Tree(parent, SWT.MULTI);

		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new PluginsViewContentProvider(this));
		treeViewer.setLabelProvider(new PluginsViewLabelProvider(this));
		treeViewer.setUseHashlookup(true);

		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.setRemoveAllWhenShown(true);
		popupMenuManager.addMenuListener(listener);
		Menu menu = popupMenuManager.createContextMenu(tree);
		tree.setMenu(menu);

		makeActions();
		IViewSite site = getViewSite();
		treeViewer.setInput(PDEPlugin.getDefault());
		site.setSelectionProvider(treeViewer);
		PDEPlugin.getDefault().getWorkspaceModelManager().addModelProviderListener(this);
		PDEPlugin.getDefault().getExternalModelManager().addModelProviderListener(this);
	}
	
	public void dispose() {
		PDEPlugin.getDefault().getWorkspaceModelManager().removeModelProviderListener(this);
		PDEPlugin.getDefault().getExternalModelManager().removeModelProviderListener(this);
		super.dispose();
	}
	
	public IDialogSettings getSettings() {
		return settingsSection;
	}

	private IDialogSettings getSettingsSection() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = masterSettings.getSection(SETTINGS_SECTION);
		if (section == null) {
			section = masterSettings.addNewSection(SETTINGS_SECTION);
		}
		return section;
	}

	private void makeActions() {
		IViewSite site = getViewSite();
		propertiesAction =
			new PropertyDialogAction(treeViewer.getTree().getShell(), treeViewer);
		propertiesAction.setImageDescriptor(PDEPluginImages.DESC_PROPERTIES);

		showVersionsAction = new Action() {
			public void run() {
				settingsSection.put(SETTING_SHOW_VERSION, showVersionsAction.isChecked());
				treeViewer.refresh();
			}
		};
		showVersionsAction.setText(PDEPlugin.getResourceString(KEY_SHOW_VERSION));
		showVersionsAction.setChecked(settingsSection.getBoolean(SETTING_SHOW_VERSION));

		showFullNamesAction = new Action() {
			public void run() {
				settingsSection.put(SETTING_SHOW_FULL_NAMES, !showFullNamesAction.isChecked());
				treeViewer.refresh();
			}
		};
		showFullNamesAction.setText(PDEPlugin.getResourceString(KEY_SHOW_FULL_NAMES));
		showFullNamesAction.setChecked(
			!settingsSection.getBoolean(SETTING_SHOW_FULL_NAMES));

		sortAction = new Action() {
			public void run() {
				boolean doSort = sortAction.isChecked();
				if (doSort) treeViewer.setSorter(sorter);
				else treeViewer.setSorter(null);
				settingsSection.put(SETTING_SORT, !sortAction.isChecked());
				treeViewer.refresh();
			}
		};
		sortAction.setText(PDEPlugin.getResourceString(KEY_SORT));
		sortAction.setChecked(!settingsSection.getBoolean(SETTING_SORT));
		
		enableAction = new EnableAction(treeViewer);
		enableAction.setText(PDEPlugin.getResourceString(KEY_ENABLE));

		IActionBars actionBars = site.getActionBars();
		IMenuManager manager = actionBars.getMenuManager();
		manager.add(showFullNamesAction);
		manager.add(showVersionsAction);
		manager.add(new Separator());
		manager.add(sortAction);
	}

	public boolean getShowFullName() {
		return showFullNamesAction.isChecked();
	}

	public boolean getShowVersion() {
		return showVersionsAction.isChecked();
	}

	public void fillContextMenu(IMenuManager manager) {
		if (isPluginSelection()) {
			manager.add(enableAction);
			manager.add(new Separator());
		}
		manager.add(propertiesAction);
	}

	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	private boolean isPluginSelection() {
		IStructuredSelection sel = (IStructuredSelection)treeViewer.getSelection();
		if (sel.isEmpty()) return false;
		for (Iterator iter=sel.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (!(obj instanceof IPluginModelBase)) return false;
		}
		return true;
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	}
	
	public void modelsChanged(IModelProviderEvent e) {
		((PluginsViewContentProvider)treeViewer.getContentProvider()).modelsChanged(e);
	}

	public void setFocus() {
		treeViewer.getTree().setFocus();
	}
}