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
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.part.*;

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
	
	public static final String P_ACTIVATE = "activate";
			
	private int MESSAGE_ORDER = -1;
	private int PLUGIN_ORDER = -1;
	private int DATE_ORDER = -1;

	public static byte MESSAGE = 0x0;
	public static byte PLUGIN = 0x1;
	public static byte DATE = 0x2;
	
	private static int ASCENDING = 1;
	private static int DESCENDING = -1;
	
	private Action clearAction;
	private Action copyAction;
	private Action readLogAction; 
	private Action deleteLogAction;
	private Action exportAction;
	private Action importAction;
	private Action activateViewAction;
	private Action propertiesAction;
	private Action viewLogAction;
	
	private Action filterAction;
	private Clipboard clipboard;
	private IMemento memento;
	private File inputFile;
	private String directory;
	
	private TableColumn column0;
	private TableColumn column1;
	private TableColumn column2;
	private TableColumn column3;
	private TableColumn column4;
	
	private static Font boldFont;
	private Comparator comparator;
	private Collator collator;
	
	// hover text
	private boolean canOpenTextShell; 
	private Text textLabel;
	private Shell textShell;
	
	private boolean firstEvent = true;

	public LogView() {
		logs = new ArrayList();
		inputFile = Platform.getLogFileLocation().toFile();
	}
	
	public void createPartControl(Composite parent) {
		readLogFile();
		TableTree tableTree = new TableTree(parent, SWT.FULL_SELECTION);
		tableTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		createColumns(tableTree.getTable());		
		createViewer(tableTree);
		createPopupMenuManager(tableTree);
		makeActions(tableTree.getTable());
		fillToolBar();
		
		Platform.addLogListener(this);
		getSite().setSelectionProvider(tableTreeViewer);
		clipboard = new Clipboard(tableTree.getDisplay());
		
		WorkbenchHelp.setHelp(tableTree,IHelpContextIds.LOG_VIEW);
		tableTreeViewer.getTableTree().getTable().setToolTipText("");
		initializeFonts();
		applyFonts();
	}
	
	private void initializeFonts(){
		Font tableFont = tableTreeViewer.getTableTree().getFont();
		FontData[] fontDataList = tableFont.getFontData();
		FontData fontData;
		if (fontDataList.length >0)
			fontData = fontDataList[0];
		else
			fontData = new FontData();
		fontData.setStyle(SWT.BOLD);
		boldFont = new Font(tableTreeViewer.getTableTree().getDisplay(), fontData);
	}
	
	
	/*
	 * Set all rows where the tableTreeItem has children to have a <b>bold</b> font.
	 */
	private void applyFonts() {
		int max = tableTreeViewer.getTableTree().getItemCount();
		int index = 0, tableIndex=0;
		while (index <max){
			LogEntry entry = (LogEntry)tableTreeViewer.getElementAt(index);
			if (entry == null)
				return;
			if (entry.hasChildren()){
				tableTreeViewer.getTableTree().getItems()[index].setFont(boldFont);
				tableIndex = applyChildFonts(entry, tableIndex);
			} else {
				tableTreeViewer.getTableTree().getItems()[index].setFont(tableTreeViewer.getTableTree().getFont());
			}

			index++;
			tableIndex++;
			
		}
	}
	private int applyChildFonts(LogEntry parent, int index){
		if (!tableTreeViewer.getExpandedState(parent) || !parent.hasChildren())
			return index;

		LogEntry[] children = getEntryChildren(parent);
		for (int i = 0; i<children.length; i++){
			index ++;
			if (children[i].hasChildren()){
				TableItem tableItem = getTableItem(index);
				if (tableItem!=null){
					tableItem.setFont(boldFont);
				}
				index = applyChildFonts(children[i], index) ;
			} else {
				TableItem tableItem = getTableItem(index);
				if (tableItem!=null){
					tableItem.setFont(tableTreeViewer.getTableTree().getFont());
				}
			}
		}
		return index;
	}
	
	private LogEntry[] getEntryChildren(LogEntry parent){
		Object[] entryChildren = parent.getChildren(parent);
		if (comparator != null)
			Arrays.sort(entryChildren, comparator);
		LogEntry[] children = new LogEntry[entryChildren.length];
		System.arraycopy(entryChildren,0,children,0,entryChildren.length);
		return children;
	}

	private TableItem getTableItem(int index){
		TableItem[] tableItems = tableTreeViewer.getTableTree().getTable().getItems();
		if (index > tableItems.length -1)
			return null;
		return tableItems[index];
	}

	private void fillToolBar() {
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);

		IToolBarManager toolBarManager = bars.getToolBarManager();
		toolBarManager.add(exportAction);
		toolBarManager.add(importAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(clearAction);
		toolBarManager.add(deleteLogAction);
		toolBarManager.add(viewLogAction);
		toolBarManager.add(readLogAction);
		toolBarManager.add(new Separator());
		
		IMenuManager mgr = bars.getMenuManager();
		mgr.add(filterAction);
		mgr.add(new Separator());
		mgr.add(activateViewAction);		
	}
	
	private void createViewer(TableTree tableTree) {
		tableTreeViewer = new TableTreeViewer(tableTree);
		tableTreeViewer.setContentProvider(new LogViewContentProvider(this));
		tableTreeViewer.setLabelProvider(new LogViewLabelProvider());
		tableTreeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e.getSelection());				
				if (propertiesAction.isEnabled())
					((EventDetailsDialogAction)propertiesAction).resetSelection();
			}
		});	
		tableTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				propertiesAction.run();
			}
		});
		tableTreeViewer.addTreeListener(new ITreeViewerListener(){

			public void treeCollapsed(TreeExpansionEvent event) {
				applyFonts();
			}

			public void treeExpanded(TreeExpansionEvent event) {
				applyFonts();
			}
			
		});
		addMouseListeners();
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
		column0 = new TableColumn(table, SWT.NULL);
		column0.setText("");
		
		column1 = new TableColumn(table, SWT.NULL);
		column1.setText(PDERuntimePlugin.getResourceString("LogView.column.severity"));

		column2 = new TableColumn(table, SWT.NULL);
		column2.setText(PDERuntimePlugin.getResourceString("LogView.column.message"));
		column2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MESSAGE_ORDER *= -1;
				ViewerSorter sorter = getViewerSorter(MESSAGE);
				tableTreeViewer.setSorter(sorter);
				collator = sorter.getCollator();
				((EventDetailsDialogAction)propertiesAction).resetSelection(MESSAGE, MESSAGE_ORDER);
				setComparator(MESSAGE);
				applyFonts();
			}
		});
		
		column3 = new TableColumn(table, SWT.NULL);
		column3.setText(PDERuntimePlugin.getResourceString("LogView.column.plugin"));
		column3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PLUGIN_ORDER *= -1;
				ViewerSorter sorter = getViewerSorter(PLUGIN);
				tableTreeViewer.setSorter(sorter);
				collator = sorter.getCollator();
				((EventDetailsDialogAction)propertiesAction).resetSelection(PLUGIN, PLUGIN_ORDER);
				setComparator(PLUGIN);
				applyFonts();
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
				ViewerSorter sorter = getViewerSorter(DATE);
				tableTreeViewer.setSorter(sorter);
				collator = sorter.getCollator();
				((EventDetailsDialogAction)propertiesAction).resetSelection(DATE, DATE_ORDER);
				setComparator(DATE);
				applyFonts();
			}
		});
		
		TableLayout tlayout = new TableLayout();
		tlayout.addColumnData(new ColumnPixelData(21));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_1).intValue()));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_2).intValue()));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_3).intValue()));
		tlayout.addColumnData(new ColumnPixelData(memento.getInteger(P_COLUMN_4).intValue()));
		table.setLayout(tlayout);
		table.setHeaderVisible(true);
	}
	
	private void makeActions(Table table) {
		propertiesAction = new EventDetailsDialogAction(table.getShell(), tableTreeViewer);
		propertiesAction.setImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES);
		propertiesAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_PROPERTIES_DISABLED);
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
		clearAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.clear.tooltip"));
		clearAction.setText(PDERuntimePlugin.getResourceString("LogView.clear"));

		readLogAction =
			new Action(PDERuntimePlugin.getResourceString("LogView.readLog.restore")) {
			public void run() {
				inputFile = Platform.getLogFileLocation().toFile();
				reloadLog();
			}
		};
		readLogAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.readLog.restore.tooltip"));
		readLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_READ_LOG);
		readLogAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_READ_LOG_DISABLED);

		deleteLogAction =
			new Action(PDERuntimePlugin.getResourceString("LogView.delete")) {
			public void run() {
				doDeleteLog();
			}
		};
		deleteLogAction.setToolTipText(
			PDERuntimePlugin.getResourceString("LogView.delete.tooltip"));
		deleteLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG);
		deleteLogAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_REMOVE_LOG_DISABLED);
		deleteLogAction.setEnabled(inputFile.exists() && inputFile.equals(Platform.getLogFileLocation().toFile()));

		copyAction = new Action(PDERuntimePlugin.getResourceString("LogView.copy")) {
			public void run() {
				copyToClipboard(tableTreeViewer.getSelection());
			}
		};
		copyAction.setImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_TOOL_COPY));


		filterAction = new Action(PDERuntimePlugin.getResourceString("LogView.filter")) {
			public void run() {
				handleFilter();
			}
		};
		filterAction.setToolTipText(PDERuntimePlugin.getResourceString("LogView.filter"));
		filterAction.setImageDescriptor(PDERuntimePluginImages.DESC_FILTER);
		filterAction.setDisabledImageDescriptor(
			PDERuntimePluginImages.DESC_FILTER_DISABLED);

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
		
		activateViewAction = new Action(PDERuntimePlugin.getResourceString("LogView.activate")) {
			public void run() {				
			}
		};
		activateViewAction.setChecked(memento.getString(P_ACTIVATE).equals("true"));
		
		viewLogAction = new Action(PDERuntimePlugin.getResourceString("LogView.view.currentLog")){
			public void run(){
				
				if (inputFile.exists())
					if (SWT.getPlatform().equals("win32"))
						Program.launch(inputFile.getAbsolutePath());
					else {
						Program p = Program.findProgram (".txt");
						if (p != null) p.execute (inputFile.getAbsolutePath());
					}
			}
		};
		viewLogAction.setImageDescriptor(PDERuntimePluginImages.DESC_OPEN_LOG);
		viewLogAction.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_OPEN_LOG_DISABLED);
		viewLogAction.setEnabled(inputFile.exists());
		viewLogAction.setToolTipText(PDERuntimePlugin.getResourceString("LogView.view.currentLog.tooltip"));
	}
	
	public void dispose() {
		Platform.removeLogListener(this);
		clipboard.dispose();
		LogReader.reset();
		boldFont.dispose();
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
			
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
						monitor.beginTask(PDERuntimePlugin.getResourceString("LogView.operation.importing"), IProgressMonitor.UNKNOWN);
						readLogFile();
				}
			};
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getViewSite().getShell());
			try {
				pmd.run(true, true, op);
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			} finally {
				readLogAction.setText(PDERuntimePlugin.getResourceString("LogView.readLog.reload"));
				readLogAction.setToolTipText(PDERuntimePlugin.getResourceString("LogView.readLog.reload"));
				asyncRefresh(false);	
				resetDialogButtons();
			}
		}	
	}
	
	private void handleExport() {
		FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
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
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
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
			reloadLog();
		
	}
	
	private void doDeleteLog() {
		String title = PDERuntimePlugin.getResourceString("LogView.confirmDelete.title");
		String message =
			PDERuntimePlugin.getResourceString("LogView.confirmDelete.message");
		if (!MessageDialog
			.openConfirm(tableTreeViewer.getControl().getShell(), title, message))
			return;
		if (inputFile.delete()) {
			logs.clear();
			asyncRefresh(false);
			resetDialogButtons();
		}
	}
	
	public void fillContextMenu(IMenuManager manager) {
		manager.add(copyAction);
		manager.add(new Separator());
		manager.add(clearAction);
		manager.add(deleteLogAction);
		manager.add(viewLogAction);
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
				asyncRefresh(false);
				resetDialogButtons();
			}
		});
	}
	
	protected void reloadLog() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				monitor.beginTask(
					PDERuntimePlugin.getResourceString("LogView.operation.reloading"),
					IProgressMonitor.UNKNOWN);
				readLogFile();
			}
		};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getViewSite().getShell());
		try {
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			readLogAction.setText(
				PDERuntimePlugin.getResourceString("LogView.readLog.restore"));
			readLogAction.setToolTipText(
				PDERuntimePlugin.getResourceString("LogView.readLog.restore"));
			asyncRefresh(false);	
			resetDialogButtons();
		}
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
			readLogFile();
			asyncRefresh();
			firstEvent = false;
		} else {
			pushStatus(status);
		}
	}
	
	private void pushStatus(IStatus status) {
		LogEntry entry = new LogEntry(status);
		LogReader.addEntry(entry, logs, memento, true);
		asyncRefresh();
	}


	private void asyncRefresh() {
		asyncRefresh(true);
	}
	
	private void asyncRefresh(final boolean activate) {
		final Control control = tableTreeViewer.getControl();
		if (control.isDisposed())
			return;

		Display display = control.getDisplay();
		final ViewPart view = this;

		if (display != null) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed()) {
						tableTreeViewer.refresh();
						deleteLogAction.setEnabled(
							inputFile.exists()
								&& inputFile.equals(Platform.getLogFileLocation().toFile()));
						viewLogAction.setEnabled(inputFile.exists());
						if (activate && activateViewAction.isChecked()) {
							IWorkbenchPage page = PDERuntimePlugin.getActivePage();
							if (page != null)
								page.bringToTop(view);
						}
					}
					applyFonts();
				}
			});
		}
	}
	
	public void setFocus() {
		if (tableTreeViewer != null && !tableTreeViewer.getTableTree().isDisposed())
			tableTreeViewer.getTableTree().getTable().setFocus();
	}
	
	private void handleSelectionChanged(ISelection selection) {
		updateStatus(selection);
		copyAction.setEnabled(!selection.isEmpty());
		propertiesAction.setEnabled(!selection.isEmpty());
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
			
		Integer width = memento.getInteger(P_COLUMN_1);
		if (width == null || width.intValue() == 0)
			memento.putInteger(P_COLUMN_1, 20);
		width = memento.getInteger(P_COLUMN_2);	
		if (width == null || width.intValue() == 0)
			memento.putInteger(P_COLUMN_2, 300);
		width = memento.getInteger(P_COLUMN_3);
		if (width == null || width.intValue() == 0)
			memento.putInteger(P_COLUMN_3, 150);
		width = memento.getInteger(P_COLUMN_4);
		if (width == null || width.intValue() == 0)
			memento.putInteger(P_COLUMN_4, 150);
			
		if (memento.getString(P_ACTIVATE) == null)
			memento.putString(P_ACTIVATE, "true");
	}
	
	public void saveState(IMemento memento) {
		if (this.memento == null || memento == null)
			return;
		this.memento.putInteger(P_COLUMN_1, column1.getWidth());
		this.memento.putInteger(P_COLUMN_2, column2.getWidth());
		this.memento.putInteger(P_COLUMN_3, column3.getWidth());
		this.memento.putInteger(P_COLUMN_4, column4.getWidth());
		this.memento.putString(
			P_ACTIVATE,
			activateViewAction.isChecked() ? "true" : "false");
		memento.putMemento(this.memento);
	}	
	
	private void addMouseListeners(){
		Listener tableListener = new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.MouseMove: onMouseMove(e); break;
				case SWT.MouseHover: onMouseHover(e); break;
				case SWT.MouseDown: onMouseDown(e); break;
				}
			}
		};
		int[] tableEvents = new int[]{SWT.MouseDown, 
									   SWT.MouseMove,
									   SWT.MouseHover};
		for (int i = 0; i < tableEvents.length; i++) {
			tableTreeViewer.getTableTree().getTable().addListener(tableEvents[i], tableListener);
		}
	}
	

	private void makeHoverShell(){
		Control control= tableTreeViewer.getControl();

		textShell= new Shell(control.getShell(), SWT.NO_FOCUS | SWT.ON_TOP);
		Display display= textShell.getDisplay();	
		textShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		GridLayout layout= new GridLayout(1, false);
		int border= ((control.getShell().getStyle() & SWT.NO_TRIM) == 0) ? 0: 1;
		layout.marginHeight= border;
		layout.marginWidth= border;
		textShell.setLayout(layout);
		textShell.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite shellComposite = new Composite(textShell, SWT.NONE);
		layout =new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		shellComposite.setLayout(layout);
		shellComposite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));
		
		textLabel= new Text(shellComposite,  SWT.WRAP | SWT.MULTI );
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= 100;
		gd.grabExcessHorizontalSpace = true;
		textLabel.setLayoutData(gd);

		Color c= control.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		textLabel.setBackground(c);
		c= control.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		textLabel.setForeground(c);
		textLabel.setEditable(false);
		
		textShell.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				onTextShellDispose(e);
			} 
		});
	}

	void onTextShellDispose(DisposeEvent e){
		canOpenTextShell = true;
		setFocus();
	}
	void onMouseDown(Event e){
		if (textShell!=null && !textShell.isDisposed() && !textShell.isFocusControl()){
			textShell.close();
			canOpenTextShell = true;
		}
	}
	void onMouseHover(Event e){
		if (!canOpenTextShell)
			return;

		canOpenTextShell = false;
		Point point = new Point (e.x, e.y);
		TableTree table = tableTreeViewer.getTableTree();
		TableTreeItem item = table.getItem(point);
		if (item == null)
			return;

		String message = ((LogEntry)item.getData()).getStack();
		if (message == null)
			return;

		makeHoverShell();	
		textLabel.setText(message);

		int x = point.x + 5;
		int y = point.y - (table.getItemHeight()*2) - 20;
		textShell.setLocation(table.toDisplay(x,y));
		textShell.setSize(tableTreeViewer.getTableTree().getSize().x - x, 125);
		textShell.open();
		setFocus();
	}

	void onMouseMove(Event e){
		if (textShell != null && !textShell.isDisposed()){
			textShell.close();
			canOpenTextShell = textShell.isDisposed() && e.x > column0.getWidth() && e.x < (column0.getWidth() + column1.getWidth());
		} else {
			canOpenTextShell = e.x > column0.getWidth() && e.x < (column0.getWidth() + column1.getWidth());
		}
	}
	
	private void setComparator(byte sortType){
		if (sortType == DATE){
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
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
			};
		} else if (sortType == PLUGIN){
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
					LogEntry entry1 = (LogEntry)e1;
					LogEntry entry2 = (LogEntry)e2;
					return collator.compare(entry1.getPluginId(), entry2.getPluginId()) * PLUGIN_ORDER;
				}
			};
		} else {
			comparator = new Comparator(){
				public int compare(Object e1, Object e2) {
					LogEntry entry1 = (LogEntry)e1;
					LogEntry entry2 = (LogEntry)e2;
					return collator.compare(entry1.getMessage(), entry2.getMessage()) * MESSAGE_ORDER;
				}
			};
		}
	}
	
	private ViewerSorter getViewerSorter(byte sortType){
		if (sortType == PLUGIN){
			return	new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				LogEntry entry1 = (LogEntry)e1;
				LogEntry entry2 = (LogEntry)e2;
				return super.compare(viewer, entry1.getPluginId(), entry2.getPluginId()) * PLUGIN_ORDER;
			}
			};
		} else if (sortType == MESSAGE){
			return new ViewerSorter() {
				public int compare(Viewer viewer, Object e1, Object e2) {
					LogEntry entry1 = (LogEntry)e1;
					LogEntry entry2 = (LogEntry)e2;
					return super.compare(viewer, entry1.getMessage(), entry2.getMessage()) * MESSAGE_ORDER;
				}
			};
		} else {
			return new ViewerSorter() {
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
			};
		}
	}
	
	private void resetDialogButtons(){
		((EventDetailsDialogAction)propertiesAction).resetDialogButtons();
	}

}
