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
package org.eclipse.pde.internal.runtime.logview;

import java.io.*;
import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ViewPart;

public class LogView extends ViewPart implements ILogListener {
	private TableTreeViewer tableTreeViewer;
	private ArrayList logs = new ArrayList();
	
	private int MESSAGE_ORDER = -1;
	private int PLUGIN_ORDER = -1;
	private int DATE_ORDER = -1;
	
	private static final String C_SEVERITY = "LogView.column.severity";
	private static final String KEY_PROPERTIES_TOOLTIP =
		"LogView.properties.tooltip";
	private static final String KEY_CLEAR_LABEL = "LogView.clear.label";
	private static final String KEY_CLEAR_TOOLTIP = "LogView.clear.tooltip";
	private static final String KEY_READ_LOG_LABEL = "LogView.readLog.label";
	private static final String KEY_READ_LOG_TOOLTIP =
		"LogView.readLog.tooltip";
	private static final String C_MESSAGE = "LogView.column.message";
	private static final String C_PLUGIN = "LogView.column.plugin";
	private static final String C_DATE = "LogView.column.date";
	private Action propertiesAction;
	private Action clearAction;
	private Action copyAction;
	private Action readLogAction; 
	private Action deleteLogAction;
	private Action filterAction;
	private LogSession thisSession;
	private Clipboard clipboard;

	public LogView() {
		logs = new ArrayList();
		thisSession = new LogSession();
		thisSession.createSessionData();
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
		tableColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MESSAGE_ORDER *= -1;
				tableTreeViewer.setSorter(new ViewerSorter() {
					public int compare(Viewer viewer, Object e1, Object e2) {
						LogEntry entry1 = (LogEntry)e1;
						LogEntry entry2 = (LogEntry)e2;
						return super.compare(viewer, entry1.getMessage(), entry2.getMessage()) * MESSAGE_ORDER;
					}
				});
				tableTreeViewer.refresh();
			}
		});
		tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(PDERuntimePlugin.getResourceString(C_PLUGIN));
		tableColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PLUGIN_ORDER *= -1;
				tableTreeViewer.setSorter(new ViewerSorter() {
					public int compare(Viewer viewer, Object e1, Object e2) {
						LogEntry entry1 = (LogEntry)e1;
						LogEntry entry2 = (LogEntry)e2;
						return super.compare(viewer, entry1.getPluginId(), entry2.getPluginId()) * PLUGIN_ORDER;
					}
				});
				tableTreeViewer.refresh();
			}
		});
		tableColumn = new TableColumn(table, SWT.NULL);
		tableColumn.setText(PDERuntimePlugin.getResourceString(C_DATE));
		tableColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DATE_ORDER *= -1;
				tableTreeViewer.setSorter(new ViewerSorter() {
					public int compare(Viewer viewer, Object e1, Object e2) {
						LogEntry entry1 = (LogEntry)e1;
						LogEntry entry2 = (LogEntry)e2;
						return super.compare(viewer, entry1.getDate(), entry2.getDate()) * DATE_ORDER;
					}
				});
				tableTreeViewer.refresh();
			}
		});
		ColumnLayoutData cLayout = new ColumnPixelData(21);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnPixelData(20);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnWeightData(100, 300, true);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnWeightData(50, 150);
		tlayout.addColumnData(cLayout);
		cLayout = new ColumnWeightData(50, 150);
		tlayout.addColumnData(cLayout);
		table.setLayout(tlayout);
		table.setHeaderVisible(true);

		tableTreeViewer = new TableTreeViewer(tableTree);
		tableTreeViewer.setContentProvider(new LogViewContentProvider(this));
		tableTreeViewer.setLabelProvider(new LogViewLabelProvider());
		tableTreeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e.getSelection());
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
		readLogFile();
		Platform.addLogListener(this);

		tableTreeViewer.setInput(Platform.class);
		getSite().setSelectionProvider(tableTreeViewer);
		propertiesAction =
			new PropertyDialogAction(table.getShell(), tableTreeViewer);
		propertiesAction.setImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES);
		propertiesAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_DISABLED);
		propertiesAction.setHoverImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_HOVER);
		propertiesAction.setToolTipText(
			PDERuntimePlugin.getResourceString(KEY_PROPERTIES_TOOLTIP));
		propertiesAction.setEnabled(false);

		clearAction =
			new Action(PDERuntimePlugin.getResourceString(KEY_CLEAR_LABEL)) {
			public void run() {
				handleClear();
			}
		};
		clearAction.setImageDescriptor(PDERuntimePluginImages.DESC_CLEAR);
		clearAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_CLEAR_DISABLED);
		clearAction.setHoverImageDescriptor(
			PDERuntimePluginImages.DESC_CLEAR_HOVER);
		clearAction.setToolTipText(
			PDERuntimePlugin.getResourceString(KEY_CLEAR_TOOLTIP));
		clearAction.setText(
			PDERuntimePlugin.getResourceString(KEY_CLEAR_LABEL));

		readLogAction =
			new Action(
				PDERuntimePlugin.getResourceString(KEY_READ_LOG_LABEL)) {
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

		table.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				if (tableTreeViewer.getSelection().isEmpty() == false) {
					propertiesAction.run();
				}
			}
		});
		
		deleteLogAction =
			new Action(PDERuntimePlugin.getResourceString("LogView.delete")) {
			public void run() {
				doDeleteLog();
			}
		};
		deleteLogAction.setToolTipText(PDERuntimePlugin.getResourceString("LogView.delete.tooltip"));
		copyAction = 
			new Action(PDERuntimePlugin.getResourceString("LogView.copy")) {
				public void run() {
					copyToClipboard(tableTreeViewer.getSelection());
				}
			};
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		copyAction.setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		copyAction.setHoverImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY_HOVER));
		
		deleteLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG);
		deleteLogAction.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG_DISABLED);
		deleteLogAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG_HOVER);
		
		filterAction = new Action("Filter") {
			public void run() {
			}
		};
		filterAction.setToolTipText("Filter");
		filterAction.setImageDescriptor(PDERuntimePluginImages.DESC_FILTER);
		filterAction.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_FILTER_DISABLED);
		filterAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_FILTER_HOVER);
		
		
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(IWorkbenchActionConstants.PROPERTIES, propertiesAction);
		bars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, copyAction);

		IToolBarManager toolBarManager = bars.getToolBarManager();
		toolBarManager.add(filterAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(deleteLogAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(clearAction);
		toolBarManager.add(readLogAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(propertiesAction);		
		
		WorkbenchHelp.setHelp(tableTree,IHelpContextIds.LOG_VIEW);
		clipboard = new Clipboard(tableTree.getDisplay());
	}
	public void dispose() {
		Platform.removeLogListener(this);
		clipboard.dispose();
		super.dispose();
	}
	
	private void doDeleteLog() {
		File logFile = Platform.getLogFileLocation().toFile();
		if (logFile.exists()) {
			String title = PDERuntimePlugin.getResourceString("LogView.confirmDelete.title");
			String message = PDERuntimePlugin.getResourceString("LogView.confirmDelete.message");
			if (!MessageDialog.openConfirm(tableTreeViewer.getControl().getShell(), title, message))
				return;
			logFile.delete();
			logs.clear();
			tableTreeViewer.refresh();
		}
	}
	
	public void fillContextMenu(IMenuManager manager) {
		manager.add(readLogAction);
		manager.add(clearAction);
		manager.add(new Separator());
		manager.add(copyAction);
		manager.add(deleteLogAction);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}
	public LogEntry[] getLogs() {
		return (LogEntry[]) logs.toArray(new LogEntry[logs.size()]);
	}
	public TableTreeViewer getTableTreeViewer() {
		return tableTreeViewer;
	}
	protected void handleClear() {
		BusyIndicator
			.showWhile(
				tableTreeViewer.getControl().getDisplay(),
				new Runnable() {
			public void run() {
				logs.clear();
				tableTreeViewer.refresh();
			}
		});
	}
	protected void restoreFromFile() {
		BusyIndicator
			.showWhile(
				tableTreeViewer.getControl().getDisplay(),
				new Runnable() {
			public void run() {
				readLogFile();
				tableTreeViewer.refresh();
			}
		});
	}
	private void readLogFile() {
		logs.clear();
		File logFile = Platform.getLogFileLocation().toFile();
		if (!logFile.exists())
			return;
		LogReader.parseLogFile(logFile, logs);
	}
	public void logging(IStatus status) {
		pushStatus(status);
	}
	public void logging(IStatus status, String plugin) {
		pushStatus(status);
	}
	private void pushStatus(IStatus status) {
		LogEntry entry = new LogEntry(status);
		entry.setSession(thisSession);
		logs.add(0, entry);
		asyncRefresh();
	}

	private void asyncRefresh() {
		final Control control = tableTreeViewer.getControl();
		if (control.isDisposed())
			return;

		Display display = control.getDisplay();
		if (display != null) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						tableTreeViewer.refresh();
				}
			});
		}
	}
	public void setFocus() {
		tableTreeViewer.getTableTree().getTable().setFocus();
	}
	
	private void handleSelectionChanged(ISelection selection) {
		propertiesAction.setEnabled(!selection.isEmpty());
		updateStatus(selection);
		copyAction.setEnabled(!selection.isEmpty());
	}
	
	private void updateStatus(ISelection selection) {
		IStatusLineManager status = getViewSite().getActionBars().getStatusLineManager();
		if (selection.isEmpty())
			status.setMessage(null);
		else {
			LogEntry entry = (LogEntry)((IStructuredSelection)selection).getFirstElement();
			LogViewLabelProvider provider = (LogViewLabelProvider)getTableTreeViewer().getLabelProvider();
			status.setMessage(provider.getColumnText(entry, 2));
		}
	}
	private void copyToClipboard(ISelection selection) {
		StringWriter writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer);
		
		if (selection.isEmpty())
			return;
		LogEntry entry = (LogEntry)((IStructuredSelection)selection).getFirstElement();
		entry.write(pwriter);
		pwriter.flush();
		String textVersion = writer.toString();
		try {
			pwriter.close();
			writer.close();
		} catch (IOException e) {
		}
		// set the clipboard contents
		clipboard.setContents(
			new Object[] { textVersion },
			new Transfer[] {
				TextTransfer.getInstance()});
	}
}
