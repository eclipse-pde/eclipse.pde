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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
	
	public static final String P_LOG_WARNING = "warning";
	public static final String P_LOG_ERROR = "error";
	public static final String P_LOG_INFO = "info";
	public static final String P_LOG_LIMIT = "limit";
	public static final String P_USE_LIMIT = "useLimit";
	public static final String P_SHOW_ALL_SESSIONS = "allSessions";
	
	private static final String P_COLUMN_1 = "column1";
	private static final String P_COLUMN_2 = "column2";
	private static final String P_COLUMN_3 = "column3";
	private static final String P_COLUMN_4 = "column4";
	
		
	private int MESSAGE_ORDER = -1;
	private int PLUGIN_ORDER = -1;
	private int DATE_ORDER = -1;
	
	private static int ASCENDING = 1;
	private static int DESCENDING = -1;
	
	private Action propertiesAction;
	private Action clearAction;
	private Action copyAction;
	private Action readLogAction; 
	private Action deleteLogAction;
	private Action exportAction;
	private Action importAction;
	
	private Action filterAction;
	private Clipboard clipboard;
	private IMemento memento;
	private File inputFile;
	private String directory;
	
	private TableColumn column1;
	private TableColumn column2;
	private TableColumn column3;
	private TableColumn column4;
	
	private boolean firstEvent = true;

	public LogView() {
		logs = new ArrayList();
		inputFile = Platform.getLogFileLocation().toFile();
	}
	
	public void createPartControl(Composite parent) {
		readLogFile();
		
		TableTree tableTree = new TableTree(parent, SWT.FULL_SELECTION);

		createColumns(tableTree.getTable());		
		createViewer(tableTree);
		createPopupMenuManager(tableTree);
		makeActions(tableTree.getTable());
		fillToolBar();
		
		Platform.addLogListener(this);
		getSite().setSelectionProvider(tableTreeViewer);
		clipboard = new Clipboard(tableTree.getDisplay());
		
		WorkbenchHelp.setHelp(tableTree,IHelpContextIds.LOG_VIEW);
	}
	
	private void fillToolBar() {
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(IWorkbenchActionConstants.PROPERTIES, propertiesAction);
		bars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, copyAction);

		IToolBarManager toolBarManager = bars.getToolBarManager();
		toolBarManager.add(exportAction);
		toolBarManager.add(importAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(deleteLogAction);
		toolBarManager.add(clearAction);
		toolBarManager.add(readLogAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(filterAction);
		toolBarManager.add(propertiesAction);		
		
	}
	private void createViewer(TableTree tableTree) {
		tableTreeViewer = new TableTreeViewer(tableTree);
		tableTreeViewer.setContentProvider(new LogViewContentProvider(this));
		tableTreeViewer.setLabelProvider(new LogViewLabelProvider());
		tableTreeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e.getSelection());
			}
		});		
		tableTreeViewer.setInput(Platform.class);
	}
	
	private void createPopupMenuManager(TableTree tableTree) {
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
		
	}
	private void createColumns(Table table) {
		TableColumn column = new TableColumn(table, SWT.NULL);
		column.setText("");
		
		column1 = new TableColumn(table, SWT.NULL);
		column1.setText(PDERuntimePlugin.getResourceString("LogView.column.severity"));

		column2 = new TableColumn(table, SWT.NULL);
		column2.setText(PDERuntimePlugin.getResourceString("LogView.column.message"));
		column2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MESSAGE_ORDER *= -1;
				tableTreeViewer.setSorter(new ViewerSorter() {
					public int compare(Viewer viewer, Object e1, Object e2) {
						LogEntry entry1 = (LogEntry)e1;
						LogEntry entry2 = (LogEntry)e2;
						return super.compare(viewer, entry1.getMessage(), entry2.getMessage()) * MESSAGE_ORDER;
					}
				});
			}
		});
		
		column3 = new TableColumn(table, SWT.NULL);
		column3.setText(PDERuntimePlugin.getResourceString("LogView.column.plugin"));
		column3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PLUGIN_ORDER *= -1;
				tableTreeViewer.setSorter(new ViewerSorter() {
					public int compare(Viewer viewer, Object e1, Object e2) {
						LogEntry entry1 = (LogEntry)e1;
						LogEntry entry2 = (LogEntry)e2;
						return super.compare(viewer, entry1.getPluginId(), entry2.getPluginId()) * PLUGIN_ORDER;
					}
				});
			}
		});
		
		column4 = new TableColumn(table, SWT.NULL);
		column4.setText(PDERuntimePlugin.getResourceString("LogView.column.date"));
		column4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (DATE_ORDER == ASCENDING) {
					DATE_ORDER = DESCENDING;
				} else {
					DATE_ORDER = ASCENDING;
				}
				tableTreeViewer.setSorter(new ViewerSorter() {
					public int compare(Viewer viewer, Object e1, Object e2) {
						try {
							SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SS"); //$NON-NLS-1$
							Date date1 = formatter.parse(((LogEntry)e1).getDate());
							Date date2 = formatter.parse(((LogEntry)e2).getDate());
							if (DATE_ORDER == ASCENDING) {
								return date1.before(date2) ? -1 : 1;
							} else {
								return date1.after(date2) ? -1 : 1;
							}
						} catch (ParseException e) {
						}
						return 0;
					}
				});
			}
		});
		
		TableLayout tlayout = new TableLayout();
		tlayout.addColumnData(new ColumnPixelData(10));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_1).intValue()));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_2).intValue()));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_3).intValue()));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_4).intValue()));
		table.setLayout(tlayout);
		table.setHeaderVisible(true);
	}
	
	private void makeActions(Table table) {
		propertiesAction = new PropertyDialogAction(table.getShell(), tableTreeViewer);
		propertiesAction.setImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES);
		propertiesAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_DISABLED);
		propertiesAction.setHoverImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_HOVER);
		propertiesAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.properties.tooltip"));
		propertiesAction.setEnabled(false);

		clearAction = new Action(PDERuntimePlugin.getResourceString("LogView.clear")) {
			public void run() {
				handleClear();
			}
		};
		clearAction.setImageDescriptor(PDERuntimePluginImages.DESC_CLEAR);
		clearAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_CLEAR_DISABLED);
		clearAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_CLEAR_HOVER);
		clearAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.clear"));
		clearAction.setText(PDERuntimePlugin.getResourceString("LogView.clear"));

		readLogAction =
			new Action(PDERuntimePlugin.getResourceString("LogView.readLog.restore")) {
			public void run() {
				loadWorkspaceLog();
			}
		};
		readLogAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.readLog.restore"));
		readLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_READ_LOG);
		readLogAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_READ_LOG_DISABLED);
		readLogAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_READ_LOG_HOVER);

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
		deleteLogAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.delete"));
		deleteLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG);
		deleteLogAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_REMOVE_LOG_DISABLED);
		deleteLogAction.setHoverImageDescriptor(
			PDERuntimePluginImages.DESC_REMOVE_LOG_HOVER);

		copyAction = new Action(PDERuntimePlugin.getResourceString("LogView.copy")) {
			public void run() {
				copyToClipboard(tableTreeViewer.getSelection());
			}
		};
		copyAction.setImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_COPY));
		copyAction.setDisabledImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_COPY_DISABLED));
		copyAction.setHoverImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_COPY_HOVER));


		filterAction = new Action(PDERuntimePlugin.getResourceString("LogView.filter")) {
			public void run() {
				handleFilter();
			}
		};
		filterAction.setToolTipText(PDERuntimePlugin.getResourceString("LogView.filter"));
		filterAction.setImageDescriptor(PDERuntimePluginImages.DESC_FILTER);
		filterAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_FILTER_DISABLED);
		filterAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_FILTER_HOVER);

		exportAction = new Action(PDERuntimePlugin.getResourceString("LogView.export")) {
			public void run() {
				handleExport();
			}
		};
		exportAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.export.tooltip"));
		exportAction.setImageDescriptor(PDERuntimePluginImages.DESC_EXPORT);
		exportAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_EXPORT_DISABLED);
		exportAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_EXPORT_HOVER);

		importAction = new Action(PDERuntimePlugin.getResourceString("LogView.import")) {
			public void run() {
				handleImport();
			}
		};
		importAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.import.tooltip"));
		importAction.setImageDescriptor(PDERuntimePluginImages.DESC_IMPORT);
		importAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_IMPORT_DISABLED);
		importAction.setHoverImageDescriptor(PDERuntimePluginImages.DESC_IMPORT_HOVER);
	}
	
	public void dispose() {
		Platform.removeLogListener(this);
		clipboard.dispose();
		super.dispose();
	}
	
	private void handleImport() {
		FileDialog dialog = new FileDialog(getViewSite().getShell());
		dialog.setFilterExtensions(new String[] { "*.log" });
		if (directory != null)
			dialog.setFilterPath(directory);
		String path = dialog.open();
		if (path != null && new Path(path).toFile().exists()) {
			inputFile = new Path(path).toFile();
			directory = inputFile.getParent();
			BusyIndicator
				.showWhile(
					tableTreeViewer.getControl().getDisplay(),
					new Runnable() {
				public void run() {
					readLogFile();
					readLogAction.setText(PDERuntimePlugin.getResourceString("LogView.readLog.reload"));
					readLogAction.setToolTipText(PDERuntimePlugin.getResourceString("LogView.readLog.reload"));
					tableTreeViewer.refresh();
				}
			});
		}	
	}
	
	private void handleExport() {
		FileDialog dialog = new FileDialog(getViewSite().getShell());
		dialog.setFilterExtensions(new String[] { "*.log" });
		if (directory != null) 
			dialog.setFilterPath(directory);
		String path = dialog.open();
		if (path != null) {
			if (!path.endsWith(".log"))
				path += ".log";
			File outputFile = new Path(path).toFile();
			directory = outputFile.getParent();
			if (outputFile.exists()) {
				String message =
					PDERuntimePlugin.getFormattedMessage(
						"LogView.confirmOverwrite.message",
						outputFile.toString());
				if (!MessageDialog
					.openQuestion(
						getViewSite().getShell(),
						exportAction.getText(),
						message))
					return;
			}
			copy(inputFile, outputFile);
		}
	}
	
	private void copy(File inputFile, File outputFile) {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
			writer = new BufferedWriter(new FileWriter(outputFile));
			while (reader.ready()) {
				writer.write(reader.readLine());
				writer.write(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
		} finally {
			try {
				if (reader != null)
					reader.close();
				if (writer != null)
					writer.close();
			} catch (IOException e1) {
			}
		}

	}
	private void handleFilter() {
		FilterDialog dialog =
			new FilterDialog(PDERuntimePlugin.getActiveWorkbenchShell(), memento);
		dialog.create();
		dialog.getShell().setText(PDERuntimePlugin.getResourceString("LogView.FilterDialog.title"));
		if (dialog.open() == FilterDialog.OK)
			loadWorkspaceLog();
		
	}
	private void doDeleteLog() {
		if (inputFile.exists()) {
			String title = PDERuntimePlugin.getResourceString("LogView.confirmDelete.title");
			String message = PDERuntimePlugin.getResourceString("LogView.confirmDelete.message");
			if (!MessageDialog.openConfirm(tableTreeViewer.getControl().getShell(), title, message))
				return;
			inputFile.delete();
			logs.clear();
			tableTreeViewer.refresh();
		}
	}
	
	public void fillContextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(new Separator());
		manager.add(clearAction);
		manager.add(deleteLogAction);
		manager.add(readLogAction);
		manager.add(new Separator());
		manager.add(exportAction);
		manager.add(importAction);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}
	public LogEntry[] getLogs() {
		return (LogEntry[]) logs.toArray(new LogEntry[logs.size()]);
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
	protected void loadWorkspaceLog() {
		BusyIndicator
			.showWhile(tableTreeViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				inputFile = Platform.getLogFileLocation().toFile();
				readLogAction.setText(
					PDERuntimePlugin.getResourceString("LogView.readLog.restore"));
				readLogAction.setToolTipText(
					PDERuntimePlugin.getResourceString("LogView.readLog.restore"));
				readLogFile();
				tableTreeViewer.refresh();
			}
		});
	}
	private void readLogFile() {
		logs.clear();
		if (!inputFile.exists())
			return;
		LogReader.parseLogFile(inputFile, logs, memento);
	}
	
	public void logging(IStatus status, String plugin) {
		if (!inputFile.equals(Platform.getLogFileLocation().toFile()))
			return;
			
		if (firstEvent) {
			loadWorkspaceLog();
			firstEvent = false;
		} else {
			pushStatus(status);
		}
	}
	private void pushStatus(IStatus status) {
		LogEntry entry = new LogEntry(status);
		LogReader.addEntry(entry, logs, memento, true);
		asyncRefresh();
		PDERuntimePlugin.getActivePage().activate(this);
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
			status.setMessage(((LogViewLabelProvider)tableTreeViewer.getLabelProvider()).getColumnText(entry, 2));
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
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento == null)
			this.memento = XMLMemento.createWriteRoot("LOGVIEW");
		else 
			this.memento = memento;
		initializeMemento();		
	}
	
	private void initializeMemento() {
		if (memento.getString(P_USE_LIMIT) == null)
			memento.putString(P_USE_LIMIT, "true");
		if (memento.getInteger(P_LOG_LIMIT) == null)
			memento.putInteger(P_LOG_LIMIT, 50);
		if (memento.getString(P_LOG_INFO) == null)
			memento.putString(P_LOG_INFO, "true");
		if (memento.getString(P_LOG_WARNING) == null)
			memento.putString(P_LOG_WARNING, "true");
		if (memento.getString(P_LOG_ERROR) == null)
			memento.putString(P_LOG_ERROR, "true");
		if (memento.getString(P_SHOW_ALL_SESSIONS) == null)
			memento.putString(P_SHOW_ALL_SESSIONS, "true");
		if (memento.getInteger(P_COLUMN_1) == null)
			memento.putInteger(P_COLUMN_1, 20);
		if (memento.getInteger(P_COLUMN_2) == null)
			memento.putInteger(P_COLUMN_2, 300);
		if (memento.getInteger(P_COLUMN_3) == null)
			memento.putInteger(P_COLUMN_3, 150);
		if (memento.getInteger(P_COLUMN_4) == null)
			memento.putInteger(P_COLUMN_4, 150);	
	}
	
	public void saveState(IMemento memento) {
		this.memento.putInteger(P_COLUMN_1, column1.getWidth());
		this.memento.putInteger(P_COLUMN_2, column2.getWidth());
		this.memento.putInteger(P_COLUMN_3, column3.getWidth());
		this.memento.putInteger(P_COLUMN_4, column4.getWidth());
		
		memento.putMemento(this.memento);
	}
	
	
}
