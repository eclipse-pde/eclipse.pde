package org.eclipse.pde.internal.runtime.logview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.util.Vector;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.ViewPart;

public class LogView
	extends ViewPart
	implements ILogListener {
	private TableTreeViewer tableTreeViewer;
	private Vector logs = new Vector();
	private static final String C_SEVERITY = "LogView.column.severity";
	private static final String KEY_PROPERTIES_TOOLTIP =
		"LogView.properties.tooltip";
	private static final String KEY_CLEAR_LABEL = "LogView.clear.label";
	private static final String KEY_CLEAR_TOOLTIP = "LogView.clear.tooltip";
	private static final String KEY_READ_LOG_LABEL = "LogView.readLog.label";
	private static final String KEY_READ_LOG_TOOLTIP = "LogView.readLog.tooltip";
	private static final String C_MESSAGE = "LogView.column.message";
	private static final String C_PLUGIN = "LogView.column.plugin";
	private Action propertiesAction;
	private Action clearAction;
	private Action readLogAction;

	public LogView() {
		logs = new Vector();
	}
	public void createPartControl(Composite parent) {
		TableTree tableTree = new TableTree(parent, SWT.FULL_SELECTION);
		TableLayout tlayout = new TableLayout();

		Table table = tableTree.getTable();
		TableColumn tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText("");
		tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(PDERuntimePlugin.getResourceString(C_SEVERITY));
		tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(PDERuntimePlugin.getResourceString(C_MESSAGE));
		tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(PDERuntimePlugin.getResourceString(C_PLUGIN));
		ColumnLayoutData cLayout = new ColumnPixelData(21);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnPixelData(20);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnWeightData(100, true);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnPixelData(100);
		tlayout.addColumnData(cLayout);
		table.setLayout(tlayout);
		table.setHeaderVisible(true);

		tableTreeViewer = new TableTreeViewer(tableTree);
		tableTreeViewer.setContentProvider(new LogViewContentProvider(this));
		tableTreeViewer.setLabelProvider(new LogViewLabelProvider());
		tableTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				propertiesAction.setEnabled(!e.getSelection().isEmpty());
			}
		});

		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Menu menu = popupMenuManager.createContextMenu(tableTree);
		tableTree.setMenu(menu);
		//readLogFile();
		Platform.addLogListener(this);

		tableTreeViewer.setInput(Platform.class);
		getSite().setSelectionProvider(tableTreeViewer);
		propertiesAction = new PropertyDialogAction(table.getShell(), tableTreeViewer);
		propertiesAction.setImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES);
		propertiesAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_DISABLED);
		propertiesAction.setHoverImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_HOVER);
		propertiesAction.setToolTipText(
			PDERuntimePlugin.getResourceString(KEY_PROPERTIES_TOOLTIP));
		propertiesAction.setEnabled(false);

		clearAction = new Action(PDERuntimePlugin.getResourceString(KEY_CLEAR_LABEL)) {
			public void run() {
				handleClear();
			}
		};
		clearAction.setImageDescriptor(PDERuntimePluginImages.DESC_CLEAR);
		clearAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_CLEAR_DISABLED);
		clearAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_CLEAR_HOVER);
		clearAction.setToolTipText(
			PDERuntimePlugin.getResourceString(KEY_CLEAR_TOOLTIP));
		clearAction.setText(PDERuntimePlugin.getResourceString(KEY_CLEAR_LABEL));

		readLogAction =
			new Action(PDERuntimePlugin.getResourceString(KEY_READ_LOG_LABEL)) {
			public void run() {
				restoreFromFile();
			}
		};
		readLogAction.setToolTipText(
			PDERuntimePlugin.getResourceString(KEY_READ_LOG_TOOLTIP));
		readLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_READ_LOG);
		readLogAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_READ_LOG_DISABLED);
		readLogAction.setHoverImageDescriptor(
			PDERuntimePluginImages.DESC_READ_LOG_HOVER);

		
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(clearAction);
		//toolBarManager.add(readLogAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(propertiesAction);
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				if (tableTreeViewer.getSelection().isEmpty() == false) {
					propertiesAction.run();
				}
			}
		});
	}
	public void dispose() {
		Platform.removeLogListener(this);
		super.dispose();
	}
	public void fillContextMenu(IMenuManager manager) {
		//manager.add(readLogAction);
		manager.add(clearAction);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}
	public StatusAdapter[] getLogs() {
		StatusAdapter[] array = new StatusAdapter[logs.size()];
		logs.copyInto(array);
		return array;
	}
	public TableTreeViewer getTableTreeViewer() {
		return tableTreeViewer;
	}
	protected void handleClear() {
		BusyIndicator
			.showWhile(tableTreeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				logs.clear();
				tableTreeViewer.refresh();
			}
		});
	}
	protected void restoreFromFile() {
		BusyIndicator
			.showWhile(tableTreeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				readLogFile();
				tableTreeViewer.refresh();
			}
		});
	}
	private void readLogFile() {
		logs.clear();
		File logFile = Platform.getLogFileLocation().toFile();
		if (!logFile.exists()) return;
	}
	public void logging(IStatus status) {
		logs.insertElementAt(new StatusAdapter(status), 0);
		tableTreeViewer.refresh();
	}
	public void logging(IStatus status, String plugin) {
		logs.insertElementAt(new StatusAdapter(status), 0);
		tableTreeViewer.refresh();
	}
	public void setFocus() {
		tableTreeViewer.getTableTree().getTable().setFocus();
	}
}