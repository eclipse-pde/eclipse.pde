/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.runtime.logview;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.runtime.IHelpContextIds;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

import com.ibm.icu.text.SimpleDateFormat;

public class LogView extends ViewPart implements ILogListener {
    public static final String P_LOG_WARNING = "warning"; //$NON-NLS-1$
    public static final String P_LOG_ERROR = "error"; //$NON-NLS-1$
    public static final String P_LOG_INFO = "info"; //$NON-NLS-1$
    public static final String P_LOG_LIMIT = "limit"; //$NON-NLS-1$
    public static final String P_USE_LIMIT = "useLimit"; //$NON-NLS-1$
    public static final String P_SHOW_ALL_SESSIONS = "allSessions"; //$NON-NLS-1$
    private static final String P_COLUMN_1 = "column2"; //$NON-NLS-1$
    private static final String P_COLUMN_2 = "column3"; //$NON-NLS-1$
    private static final String P_COLUMN_3 = "column4"; //$NON-NLS-1$
    public static final String P_ACTIVATE = "activate"; //$NON-NLS-1$
    public static final String P_ORDER_TYPE = "orderType"; //$NON-NLS-1$
    public static final String P_ORDER_VALUE = "orderValue"; //$NON-NLS-1$
    
    private int MESSAGE_ORDER;
    private int PLUGIN_ORDER;
    private int DATE_ORDER;
    
    public final static byte MESSAGE = 0x0;
    public final static byte PLUGIN = 0x1;
    public final static byte DATE = 0x2;
    public static int ASCENDING = 1;
    public static int DESCENDING = -1;
    
    private ArrayList fLogs;

    private Clipboard fClipboard;
    
    private IMemento fMemento;
    private File fInputFile;
    private String fDirectory;

    private Comparator comparator;
    private Collator collator;
    
    // hover text
    private boolean canOpenTextShell;
    private Text textLabel;
    private Shell textShell;

    private boolean fFirstEvent = true;
    
	private TreeColumn fColumn1;
	private TreeColumn fColumn2;
	private TreeColumn fColumn3;
	
	private Tree fTree;
	private TreeViewer fTreeViewer;
	
	private Action fPropertiesAction;
	private Action fDeleteLogAction;
	private Action fReadLogAction;
	private Action fCopyAction;
	private Action fActivateViewAction;
	private Action fOpenLogAction;
	private Action fExportAction;

    public LogView() {
        fLogs = new ArrayList();
        fInputFile = Platform.getLogFileLocation().toFile();
    }

    public void createPartControl(Composite parent) {
        readLogFile();
        createViewer(parent);
        createActions();
        fClipboard = new Clipboard(fTree.getDisplay());
        fTree.setToolTipText(""); //$NON-NLS-1$
        getSite().setSelectionProvider(fTreeViewer);
        initializeViewerSorter();
        
        makeHoverShell();
        
        Platform.addLogListener(this);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fTree, IHelpContextIds.LOG_VIEW);
    }

     private void createActions() {
        IActionBars bars = getViewSite().getActionBars();
        
        fCopyAction = createCopyAction();
        bars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
        
        IToolBarManager toolBarManager = bars.getToolBarManager();
        
        fExportAction = createExportAction();
		toolBarManager.add(fExportAction);
        
        final Action importLogAction = createImportLogAction();
        toolBarManager.add(importLogAction);
        
        toolBarManager.add(new Separator());
        
        final Action clearAction = createClearAction();
        toolBarManager.add(clearAction);
        
        fDeleteLogAction = createDeleteLogAction();
        toolBarManager.add(fDeleteLogAction);
        
        fOpenLogAction = createOpenLogAction();
        toolBarManager.add(fOpenLogAction);
        
        fReadLogAction = createReadLogAction();
        toolBarManager.add(fReadLogAction);
        
        toolBarManager.add(new Separator());
        
        IMenuManager mgr = bars.getMenuManager();
        mgr.add(createFilterAction());
        mgr.add(new Separator());
        
        fActivateViewAction = createActivateViewAction();
        mgr.add(fActivateViewAction);
        
        createPropertiesAction();
        
        MenuManager popupMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        IMenuListener listener = new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(fCopyAction);
                manager.add(new Separator());
                manager.add(clearAction);
                manager.add(fDeleteLogAction);
                manager.add(fOpenLogAction);
                manager.add(fReadLogAction);
                manager.add(new Separator());
                manager.add(fExportAction);
                manager.add(importLogAction);
                manager.add(new Separator());
                ((EventDetailsDialogAction) fPropertiesAction).setComparator(comparator);
                manager.add(fPropertiesAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        };
        popupMenuManager.addMenuListener(listener);
        popupMenuManager.setRemoveAllWhenShown(true);
        getSite().registerContextMenu(popupMenuManager, getSite().getSelectionProvider());
        Menu menu = popupMenuManager.createContextMenu(fTree);
        fTree.setMenu(menu);
    }
     
     private Action createActivateViewAction() {
        Action action = new Action(PDERuntimeMessages.LogView_activate) { //       	
            public void run() {
            	fMemento.putString(P_ACTIVATE, isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        };
        action.setChecked(fMemento.getString(P_ACTIVATE).equals("true")); //$NON-NLS-1$
        return action;
     }
     
     private Action createClearAction() {
        Action action = new Action(PDERuntimeMessages.LogView_clear) { 
            public void run() {
                handleClear();
            }
        };
        action.setImageDescriptor(PDERuntimePluginImages.DESC_CLEAR);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_CLEAR_DISABLED);
        action.setToolTipText(PDERuntimeMessages.LogView_clear_tooltip); 
        action.setText(PDERuntimeMessages.LogView_clear); 
    	return action;
    }
     
    private Action createCopyAction() {
        Action action = new Action(PDERuntimeMessages.LogView_copy) { 
            public void run() {
                copyToClipboard(fTreeViewer.getSelection());
            }
        };
        action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
    	return action;
    }
    
    private Action createDeleteLogAction() {
        Action action = new Action(PDERuntimeMessages.LogView_delete) { 
            public void run() {
                doDeleteLog();
            }
        };
        action.setToolTipText(PDERuntimeMessages.LogView_delete_tooltip); 
        action.setImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_REMOVE_LOG_DISABLED);
        action.setEnabled(fInputFile.exists() && fInputFile.equals(Platform.getLogFileLocation().toFile()));
        return action;
    }

    private Action createExportAction() {
        Action action = new Action(PDERuntimeMessages.LogView_export) { 
            public void run() {
                handleExport();
            }
        };
        action.setToolTipText(PDERuntimeMessages.LogView_export_tooltip); 
        action.setImageDescriptor(PDERuntimePluginImages.DESC_EXPORT);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_EXPORT_DISABLED);
		action.setEnabled(fInputFile.exists());
    	return action;
    }
    
    private Action createFilterAction() {
        Action action = new Action(PDERuntimeMessages.LogView_filter) { 
            public void run() {
                handleFilter();
            }
        };
        action.setToolTipText(PDERuntimeMessages.LogView_filter); 
        action.setImageDescriptor(PDERuntimePluginImages.DESC_FILTER);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_FILTER_DISABLED);
    	return action;
    }
    
    private Action createImportLogAction() {
        Action action = new Action(PDERuntimeMessages.LogView_import) { 
            public void run() {
                handleImport();
            }
        };
        action.setToolTipText(PDERuntimeMessages.LogView_import_tooltip); 
        action.setImageDescriptor(PDERuntimePluginImages.DESC_IMPORT);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_IMPORT_DISABLED);
        return action;
    }
    
    private Action createOpenLogAction() {
        Action action = new Action(PDERuntimeMessages.LogView_view_currentLog) { 
            public void run() {
                if (fInputFile.exists()) {
                    if (fInputFile.length() > LogReader.MAX_FILE_LENGTH) {
                        OpenLogDialog openDialog = new OpenLogDialog(getViewSite()
                                .getShell(), fInputFile);
                        openDialog.create();
                        openDialog.open();
                        return;
                    } 
                    if (!Program.launch(fInputFile.getAbsolutePath())) {
                        Program p = Program.findProgram(".txt"); //$NON-NLS-1$
                        if (p != null)
                            p.execute(fInputFile.getAbsolutePath());
                        else {
                            OpenLogDialog openDialog = new OpenLogDialog(getViewSite().getShell(), fInputFile);
                            openDialog.create();
                            openDialog.open();
                        }                  
                    }
                }
            }
        };
        action.setImageDescriptor(PDERuntimePluginImages.DESC_OPEN_LOG);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_OPEN_LOG_DISABLED);
        action.setEnabled(fInputFile.exists());
        action.setToolTipText(PDERuntimeMessages.LogView_view_currentLog_tooltip); 
    	return action;
    }
    
    private void createPropertiesAction() {
        fPropertiesAction = new EventDetailsDialogAction(fTree.getShell(), fTreeViewer);
        fPropertiesAction.setImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES);
        fPropertiesAction.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_PROPERTIES_DISABLED);
        fPropertiesAction.setToolTipText(PDERuntimeMessages.LogView_properties_tooltip); 
        fPropertiesAction.setEnabled(false);
    }
    
    private Action createReadLogAction() {
        Action action = new Action(PDERuntimeMessages.LogView_readLog_restore) { 
            public void run() {
                fInputFile = Platform.getLogFileLocation().toFile();
                reloadLog();
            }
        };
        action.setToolTipText(PDERuntimeMessages.LogView_readLog_restore_tooltip); 
        action.setImageDescriptor(PDERuntimePluginImages.DESC_READ_LOG);
        action.setDisabledImageDescriptor(PDERuntimePluginImages.DESC_READ_LOG_DISABLED);
    	return action;
    }
    
    private void createViewer(Composite parent) {
        fTreeViewer = new TreeViewer(parent, SWT.FULL_SELECTION);
        fTree = fTreeViewer.getTree();
        fTree.setLinesVisible(true);
        createColumns(fTree);
        fTreeViewer.setContentProvider(new LogViewContentProvider(this));
        fTreeViewer.setLabelProvider(new LogViewLabelProvider());
        fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent e) {
                handleSelectionChanged(e.getSelection());
                if (fPropertiesAction.isEnabled())
                    ((EventDetailsDialogAction) fPropertiesAction).resetSelection();
            }
        });
        fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ((EventDetailsDialogAction) fPropertiesAction).setComparator(comparator);
                fPropertiesAction.run();
            }		
        });
        fTreeViewer.setInput(this);
        addMouseListeners();
   }

    private void createColumns(Tree tree) {
        fColumn1 = new TreeColumn(tree, SWT.LEFT);
        fColumn1.setText(PDERuntimeMessages.LogView_column_message); 
        fColumn1.setWidth(fMemento.getInteger(P_COLUMN_1).intValue());
        fColumn1.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                MESSAGE_ORDER *= -1;
                ViewerSorter sorter = getViewerSorter(MESSAGE);
                fTreeViewer.setSorter(sorter);
                collator = sorter.getCollator();
                boolean isComparatorSet = 
                	((EventDetailsDialogAction) fPropertiesAction).resetSelection(MESSAGE, MESSAGE_ORDER);
                setComparator(MESSAGE);
                if (!isComparatorSet)
                    ((EventDetailsDialogAction) fPropertiesAction).setComparator(comparator);
                fMemento.putInteger(P_ORDER_VALUE, MESSAGE_ORDER);
                fMemento.putInteger(P_ORDER_TYPE, MESSAGE);
                setColumnSorting(fColumn1, MESSAGE_ORDER);
            }
        });

        fColumn2 = new TreeColumn(tree, SWT.LEFT);
        fColumn2.setText(PDERuntimeMessages.LogView_column_plugin); 
        fColumn2.setWidth(fMemento.getInteger(P_COLUMN_2).intValue());
        fColumn2.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                PLUGIN_ORDER *= -1;
                ViewerSorter sorter = getViewerSorter(PLUGIN);
                fTreeViewer.setSorter(sorter);
                collator = sorter.getCollator();
                boolean isComparatorSet = 
                	((EventDetailsDialogAction) fPropertiesAction).resetSelection(PLUGIN, PLUGIN_ORDER);
                setComparator(PLUGIN);
                if (!isComparatorSet)
                    ((EventDetailsDialogAction) fPropertiesAction).setComparator(comparator);
                fMemento.putInteger(P_ORDER_VALUE, PLUGIN_ORDER);
                fMemento.putInteger(P_ORDER_TYPE, PLUGIN);
                setColumnSorting(fColumn2, PLUGIN_ORDER);
            }
        });

        fColumn3 = new TreeColumn(tree, SWT.LEFT);
        fColumn3.setText(PDERuntimeMessages.LogView_column_date);
        fColumn3.setWidth(fMemento.getInteger(P_COLUMN_3).intValue());
        fColumn3.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DATE_ORDER *= -1;
                ViewerSorter sorter = getViewerSorter(DATE);
                fTreeViewer.setSorter(sorter);
                collator = sorter.getCollator();
                setComparator(DATE);
				((EventDetailsDialogAction) fPropertiesAction).setComparator(comparator);
                fMemento.putInteger(P_ORDER_VALUE, DATE_ORDER);
                fMemento.putInteger(P_ORDER_TYPE, DATE);
                setColumnSorting(fColumn3, DATE_ORDER);
            }
        });
        
        tree.setHeaderVisible(true);
    }

	private void initializeViewerSorter() {
		byte orderType = fMemento.getInteger(P_ORDER_TYPE).byteValue();
        ViewerSorter sorter = getViewerSorter(orderType);
        fTreeViewer.setSorter(sorter);
        if (orderType == MESSAGE )
        	setColumnSorting(fColumn1, MESSAGE_ORDER);
        else if (orderType == PLUGIN)
        	setColumnSorting(fColumn2, PLUGIN_ORDER);
        else if (orderType == DATE)
        	setColumnSorting(fColumn3, DATE_ORDER);
	}

	private void setColumnSorting(TreeColumn column, int order) {
		fTree.setSortColumn(column);
        fTree.setSortDirection(order == ASCENDING ? SWT.UP : SWT.DOWN);
	}
	
    public void dispose() {
        writeSettings();
        Platform.removeLogListener(this);
        fClipboard.dispose();
        textShell.dispose();
        LogReader.reset();
        super.dispose();
    }

    
    private void handleImport() {
        FileDialog dialog = new FileDialog(getViewSite().getShell());
        dialog.setFilterExtensions(new String[] { "*.log" }); //$NON-NLS-1$
        if (fDirectory != null)
            dialog.setFilterPath(fDirectory);
        handleImportPath(dialog.open());
    }
    
    public void handleImportPath(String path) {
        if (path != null && new Path(path).toFile().exists()) {
            fInputFile = new Path(path).toFile();
            fDirectory = fInputFile.getParent();
            IRunnableWithProgress op = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(PDERuntimeMessages.LogView_operation_importing, IProgressMonitor.UNKNOWN); 
                    readLogFile();
                }
            };
            ProgressMonitorDialog pmd = new ProgressMonitorDialog(getViewSite().getShell());
            try {
                pmd.run(true, true, op);
            } catch (InvocationTargetException e) {
            } catch (InterruptedException e) {
            } finally {
                fReadLogAction.setText(PDERuntimeMessages.LogView_readLog_reload); 
                fReadLogAction.setToolTipText(PDERuntimeMessages.LogView_readLog_reload); 
                asyncRefresh(false);
                resetDialogButtons();
            }
        }
    }

    private void handleExport() {
        FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.log" }); //$NON-NLS-1$
        if (fDirectory != null)
            dialog.setFilterPath(fDirectory);
        String path = dialog.open();
        if (path != null) {
            if (path.indexOf('.') == -1 && !path.endsWith(".log")) //$NON-NLS-1$
                path += ".log"; //$NON-NLS-1$
            File outputFile = new Path(path).toFile();
            fDirectory = outputFile.getParent();
            if (outputFile.exists()) {
                String message = NLS.bind(PDERuntimeMessages.LogView_confirmOverwrite_message, outputFile.toString());
                if (!MessageDialog.openQuestion(getViewSite().getShell(), PDERuntimeMessages.LogView_exportLog, message)) 
                    return;
            }
            copy(fInputFile, outputFile);
        }
    }

    private void copy(File inputFile, File outputFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8")); //$NON-NLS-1$
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")); //$NON-NLS-1$
            while (reader.ready()) {
                writer.write(reader.readLine());
                writer.write(System.getProperty("line.separator")); //$NON-NLS-1$
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
    	FilterDialog dialog = new FilterDialog(PDERuntimePlugin.getActiveWorkbenchShell(), fMemento);
    	dialog.create();
    	dialog.getShell().setText(PDERuntimeMessages.LogView_FilterDialog_title); 
    	if (dialog.open() == Window.OK)
    		reloadLog();
    }
    
    private void doDeleteLog() {
    	String title = PDERuntimeMessages.LogView_confirmDelete_title; 
    	String message = PDERuntimeMessages.LogView_confirmDelete_message; 
    	if (!MessageDialog.openConfirm(fTree.getShell(), title, message))
    		return;
    	if (fInputFile.delete() || fLogs.size() > 0) {
    		fLogs.clear();
            asyncRefresh(false);
            resetDialogButtons();
        }
    }

    public void fillContextMenu(IMenuManager manager) {
    }

    public LogEntry[] getLogs() {
        return (LogEntry[]) fLogs.toArray(new LogEntry[fLogs.size()]);
    }

    protected void handleClear() {
        BusyIndicator.showWhile(fTree.getDisplay(),
                new Runnable() {
                    public void run() {
                        fLogs.clear();
                        asyncRefresh(false);
                        resetDialogButtons();
                    }
                });
    }

    protected void reloadLog() {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask(PDERuntimeMessages.LogView_operation_reloading, IProgressMonitor.UNKNOWN);
                readLogFile();
            }
        };
        ProgressMonitorDialog pmd = new ProgressMonitorDialog(getViewSite().getShell());
        try {
            pmd.run(true, true, op);
        } catch (InvocationTargetException e) {
        } catch (InterruptedException e) {
        } finally {
            fReadLogAction.setText(PDERuntimeMessages.LogView_readLog_restore); 
            fReadLogAction.setToolTipText(PDERuntimeMessages.LogView_readLog_restore); 
            asyncRefresh(false);
            resetDialogButtons();
        }
    }

    private void readLogFile() {
		fLogs.clear();
		if (!fInputFile.exists())
			return;
		LogReader.parseLogFile(fInputFile, fLogs, fMemento);
	}

    public void logging(IStatus status, String plugin) {
        if (!fInputFile.equals(Platform.getLogFileLocation().toFile()))
            return;
        if (fFirstEvent) {
            readLogFile();
            asyncRefresh();
            fFirstEvent = false;
        } else {
            pushStatus(status);
        }
    }

    private void pushStatus(IStatus status) {
        LogEntry entry = new LogEntry(status);
        LogReader.addEntry(entry, fLogs, fMemento, true);
        asyncRefresh();
    }

    private void asyncRefresh() {
        asyncRefresh(true);
    }

    private void asyncRefresh(final boolean activate) {
        if (fTree.isDisposed())
            return;
        Display display = fTree.getDisplay();
        final ViewPart view = this;
        if (display != null) {
            display.asyncExec(new Runnable() {
                public void run() {
                    if (!fTree.isDisposed()) {
                        fTreeViewer.refresh();
                        fDeleteLogAction.setEnabled(fInputFile.exists()
                                && fInputFile.equals(Platform.getLogFileLocation().toFile()));
                        fOpenLogAction.setEnabled(fInputFile.exists());
						fExportAction.setEnabled(fInputFile.exists());
                        if (activate && fActivateViewAction.isChecked()) {
                            IWorkbenchPage page = PDERuntimePlugin.getActivePage();
                            if (page != null)
                                page.bringToTop(view);
                        }
                    }
                }
            });
        }
    }

    public void setFocus() {
        if (fTree != null && !fTree.isDisposed())
            fTree.setFocus();
    }

    private void handleSelectionChanged(ISelection selection) {
        updateStatus(selection);
        fCopyAction.setEnabled(!selection.isEmpty());
        fPropertiesAction.setEnabled(!selection.isEmpty());
    }

    private void updateStatus(ISelection selection) {
        IStatusLineManager status = getViewSite().getActionBars().getStatusLineManager();
        if (selection.isEmpty())
            status.setMessage(null);
        else {
            LogEntry entry = (LogEntry) ((IStructuredSelection) selection).getFirstElement();
            status.setMessage(((LogViewLabelProvider) fTreeViewer.getLabelProvider()).getColumnText(entry, 0));
        }
    }

    private void copyToClipboard(ISelection selection) {
        StringWriter writer = new StringWriter();
        PrintWriter pwriter = new PrintWriter(writer);
        if (selection.isEmpty())
            return;
        LogEntry entry = (LogEntry) ((IStructuredSelection) selection).getFirstElement();
        entry.write(pwriter);
        pwriter.flush();
        String textVersion = writer.toString();
        try {
            pwriter.close();
            writer.close();
        } catch (IOException e) {
        }
        if (textVersion.trim().length() > 0) {
	        // set the clipboard contents
	        fClipboard.setContents(
	        		new Object[] { textVersion },
	        		new Transfer[] { TextTransfer.getInstance() });
        }
    }

    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        if (memento == null)
            this.fMemento = XMLMemento.createWriteRoot("LOGVIEW"); //$NON-NLS-1$
        else
            this.fMemento = memento;
        readSettings();
        
        // initialize column ordering 
        final byte type = this.fMemento.getInteger(P_ORDER_TYPE).byteValue();
        switch (type){
        case DATE:
        	DATE_ORDER = this.fMemento.getInteger(P_ORDER_VALUE).intValue();
        	MESSAGE_ORDER = DESCENDING;
        	PLUGIN_ORDER = DESCENDING;
        	break;
        case MESSAGE:
        	MESSAGE_ORDER = this.fMemento.getInteger(P_ORDER_VALUE).intValue();
        	DATE_ORDER = DESCENDING;
        	PLUGIN_ORDER = DESCENDING;
        	break;
        case PLUGIN:
        	PLUGIN_ORDER = this.fMemento.getInteger(P_ORDER_VALUE).intValue();
        	MESSAGE_ORDER = DESCENDING;
        	DATE_ORDER = DESCENDING;
        	break;
		default:
			DATE_ORDER = DESCENDING;
			MESSAGE_ORDER = DESCENDING;
			PLUGIN_ORDER = DESCENDING;
        }
		if (collator == null)
			collator = Collator.getInstance();
		setComparator(fMemento.getInteger(P_ORDER_TYPE).byteValue());
    }

    private void initializeMemento() {
        if (fMemento.getString(P_USE_LIMIT) == null)
            fMemento.putString(P_USE_LIMIT, "true"); //$NON-NLS-1$
        if (fMemento.getInteger(P_LOG_LIMIT) == null)
            fMemento.putInteger(P_LOG_LIMIT, 50);
        if (fMemento.getString(P_LOG_INFO) == null)
            fMemento.putString(P_LOG_INFO, "true"); //$NON-NLS-1$
        if (fMemento.getString(P_LOG_WARNING) == null)
            fMemento.putString(P_LOG_WARNING, "true"); //$NON-NLS-1$
        if (fMemento.getString(P_LOG_ERROR) == null)
            fMemento.putString(P_LOG_ERROR, "true"); //$NON-NLS-1$
        if (fMemento.getString(P_SHOW_ALL_SESSIONS) == null)
            fMemento.putString(P_SHOW_ALL_SESSIONS, "true"); //$NON-NLS-1$
        Integer width = fMemento.getInteger(P_COLUMN_1);
        if (width == null || width.intValue() == 0)
            fMemento.putInteger(P_COLUMN_1, 300);
        width = fMemento.getInteger(P_COLUMN_2);
        if (width == null || width.intValue() == 0)
            fMemento.putInteger(P_COLUMN_2, 150);
        width = fMemento.getInteger(P_COLUMN_3);
        if (width == null || width.intValue() == 0)
            fMemento.putInteger(P_COLUMN_3, 150);
        if (fMemento.getString(P_ACTIVATE) == null)
            fMemento.putString(P_ACTIVATE, "true"); //$NON-NLS-1$
        
       	fMemento.putInteger(P_ORDER_VALUE, DESCENDING);
        fMemento.putInteger(P_ORDER_TYPE, DATE);
    }

    public void saveState(IMemento memento) {
        if (this.fMemento == null || memento == null)
            return;
        this.fMemento.putInteger(P_COLUMN_1, fColumn1.getWidth());
        this.fMemento.putInteger(P_COLUMN_2, fColumn2.getWidth());
        this.fMemento.putInteger(P_COLUMN_3, fColumn3.getWidth());
        this.fMemento.putString(P_ACTIVATE,
                fActivateViewAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
        memento.putMemento(this.fMemento);
        writeSettings();
    }

    private void addMouseListeners() {
        Listener tableListener = new Listener() {
            public void handleEvent(Event e) {
                switch (e.type) {
                case SWT.MouseMove:
                    onMouseMove(e);
                    break;
                case SWT.MouseHover:
                    onMouseHover(e);
                    break;
                case SWT.MouseDown:
                    onMouseDown(e);
                    break;
                }
            }
        };
        int[] tableEvents = new int[] { SWT.MouseDown, SWT.MouseMove, SWT.MouseHover };
        for (int i = 0; i < tableEvents.length; i++) {
            fTree.addListener(tableEvents[i], tableListener);
        }
    }

    private void makeHoverShell() {
        textShell = new Shell(fTree.getShell(), SWT.NO_FOCUS | SWT.ON_TOP | SWT.TOOL);
        Display display = textShell.getDisplay();
        textShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        GridLayout layout = new GridLayout(1, false);
        int border = ((fTree.getShell().getStyle() & SWT.NO_TRIM) == 0) ? 0 : 1;
        layout.marginHeight = border;
        layout.marginWidth = border;
        textShell.setLayout(layout);
        textShell.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite shellComposite = new Composite(textShell, SWT.NONE);
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        shellComposite.setLayout(layout);
        shellComposite.setLayoutData(
        		new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));
        textLabel = new Text(shellComposite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 100;
        gd.grabExcessHorizontalSpace = true;
        textLabel.setLayoutData(gd);
        Color c = fTree.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        textLabel.setBackground(c);
        c = fTree.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        textLabel.setForeground(c);
        textLabel.setEditable(false);
        textShell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                onTextShellDispose(e);
            }
        });
    }

    void onTextShellDispose(DisposeEvent e) {
        canOpenTextShell = true;
        setFocus();
    }

    void onMouseDown(Event e) {
        if (textShell != null && !textShell.isDisposed() && !textShell.isFocusControl()) {
            textShell.setVisible(false);
            canOpenTextShell = true;
        }
    }

    void onMouseHover(Event e) {
        if (!canOpenTextShell)
            return;
        canOpenTextShell = false;
        Point point = new Point(e.x, e.y);
		TreeItem item = fTree.getItem(point);
        if (item == null)
            return;
        String message = ((LogEntry) item.getData()).getStack();
        if (message == null)
            return;
        
        textLabel.setText(message);
        Rectangle bounds = fTree.getDisplay().getBounds();
        Point cursorPoint = fTree.getDisplay().getCursorLocation();
        int x = point.x;
        int y = point.y + 25;
        int width = fTree.getColumn(0).getWidth();
        int height = 125;
    	if (cursorPoint.x + width > bounds.width)
    		x -= width;
    	if (cursorPoint.y + height + 25 > bounds.height)
    		y -= height + 27;
        
        textShell.setLocation(fTree.toDisplay(x, y));
        textShell.setSize(width, height);
        textShell.setVisible(true);
    }

    void onMouseMove(Event e) {
        if (textShell != null && !textShell.isDisposed() && textShell.isVisible())
            textShell.setVisible(false);
		
		Point point = new Point(e.x, e.y);
		TreeItem item = fTree.getItem(point);
		if (item == null) 
			return;
		Image image= item.getImage();
		LogEntry entry = (LogEntry)item.getData();
		int parentCount = getNumberOfParents(entry);
		int startRange = 20 + Math.max(image.getBounds().width + 2, 7 + 2)*parentCount;
		int endRange = startRange + 16;
		canOpenTextShell = e.x >= startRange && e.x <= endRange;
	}
	
	private int getNumberOfParents(LogEntry entry){
		LogEntry parent = (LogEntry)entry.getParent(entry);
		if (parent ==null)
			return 0;
		return 1 + getNumberOfParents(parent);
	}

    public Comparator getComparator() {
        return comparator;
    }

    private void setComparator(byte sortType) {
        if (sortType == DATE) {
            comparator = new Comparator() {
                public int compare(Object e1, Object e2) {
					try {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
						Date date1 = formatter.parse(((LogEntry) e1).getDate());
						Date date2 = formatter.parse(((LogEntry) e2).getDate());
						if (DATE_ORDER == ASCENDING)
							return date1.before(date2) ? DESCENDING : ASCENDING;
						return date1.after(date2) ? DESCENDING : ASCENDING;
					} catch (ParseException e) {
					}
					return 0;
                }
            };
        } else if (sortType == PLUGIN) {
            comparator = new Comparator() {
                public int compare(Object e1, Object e2) {
                    LogEntry entry1 = (LogEntry) e1;
                    LogEntry entry2 = (LogEntry) e2;
                    return collator.compare(entry1.getPluginId(), entry2.getPluginId()) * PLUGIN_ORDER;
                }
            };
        } else {
            comparator = new Comparator() {
                public int compare(Object e1, Object e2) {
                    LogEntry entry1 = (LogEntry) e1;
                    LogEntry entry2 = (LogEntry) e2;
                    return collator.compare(entry1.getMessage(), entry2.getMessage()) * MESSAGE_ORDER;
                }
            };
        }
    }

    private ViewerSorter getViewerSorter(byte sortType) {
        if (sortType == PLUGIN) {
            return new ViewerSorter() {
                public int compare(Viewer viewer, Object e1, Object e2) {
                    LogEntry entry1 = (LogEntry) e1;
                    LogEntry entry2 = (LogEntry) e2;
                    return super.compare(viewer, entry1.getPluginId(), entry2.getPluginId()) * PLUGIN_ORDER;
                }
            };
        } else if (sortType == MESSAGE) {
            return new ViewerSorter() {
                public int compare(Viewer viewer, Object e1, Object e2) {
                    LogEntry entry1 = (LogEntry) e1;
                    LogEntry entry2 = (LogEntry) e2;
                    return super.compare(viewer, entry1.getMessage(), entry2.getMessage()) * MESSAGE_ORDER;
                }
            };
        } else {
            return new ViewerSorter() {
				public int compare(Viewer viewer, Object e1, Object e2) {
					try {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
						Date date1 = formatter.parse(((LogEntry) e1).getDate());
						Date date2 = formatter.parse(((LogEntry) e2).getDate());
						if (DATE_ORDER == ASCENDING)
							return date1.before(date2) ? DESCENDING : ASCENDING;
						return date1.after(date2) ? DESCENDING : ASCENDING;
					} catch (ParseException e) {
					}
					return 0;
                }
            };
        }
    }

    private void resetDialogButtons() {
        ((EventDetailsDialogAction) fPropertiesAction).resetDialogButtons();
    }
    
    /**
     * Returns the filter dialog settings object used to maintain
     * state between filter dialogs
     * @return the dialog settings to be used
     */
    private IDialogSettings getLogSettings() {
        IDialogSettings settings= PDERuntimePlugin.getDefault().getDialogSettings();
        return settings.getSection(getClass().getName());
    }
    
    /**
     * Returns the plugin preferences used to maintain
     * state of log view
     * @return the plugin preferences
     */
    private Preferences getLogPreferences(){
    	return PDERuntimePlugin.getDefault().getPluginPreferences();
    }

    private void readSettings(){
        IDialogSettings s = getLogSettings();
        Preferences p = getLogPreferences();
        if (s == null || p == null){
            initializeMemento();
            return;
        }
        try {
			fMemento.putString(P_USE_LIMIT, s.getBoolean(P_USE_LIMIT) ? "true":"false"); //$NON-NLS-1$ //$NON-NLS-2$
			fMemento.putInteger(P_LOG_LIMIT, s.getInt(P_LOG_LIMIT));
			fMemento.putString(P_LOG_INFO, s.getBoolean(P_LOG_INFO) ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			fMemento.putString(P_LOG_WARNING, s.getBoolean(P_LOG_WARNING) ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			fMemento.putString(P_LOG_ERROR, s.getBoolean(P_LOG_ERROR) ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			fMemento.putString(P_SHOW_ALL_SESSIONS, s.getBoolean(P_SHOW_ALL_SESSIONS) ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			fMemento.putInteger(P_COLUMN_1, p.getInt(P_COLUMN_1) > 0 ? p.getInt(P_COLUMN_1) : 300);
			fMemento.putInteger(P_COLUMN_2, p.getInt(P_COLUMN_2) > 0 ? p.getInt(P_COLUMN_2) : 150);
			fMemento.putInteger(P_COLUMN_3, p.getInt(P_COLUMN_3) > 0 ? p.getInt(P_COLUMN_3) : 150);
			fMemento.putString(P_ACTIVATE, p.getBoolean(P_ACTIVATE) ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			int order = p.getInt(P_ORDER_VALUE);
			fMemento.putInteger(P_ORDER_VALUE, order == 0 ? DESCENDING : order);
			fMemento.putInteger(P_ORDER_TYPE, p.getInt(P_ORDER_TYPE));
		} catch (NumberFormatException e) {
			fMemento.putInteger(P_LOG_LIMIT, 50);
			fMemento.putInteger(P_COLUMN_1, 300);
			fMemento.putInteger(P_COLUMN_2, 150);
			fMemento.putInteger(P_COLUMN_3, 150);
			fMemento.putInteger(P_ORDER_TYPE, DATE);
			fMemento.putInteger(P_ORDER_VALUE, DESCENDING);
		}
    }
    
    private void writeSettings(){
        writeViewSettings();
        writeFilterSettings();
    }
    
    private void writeFilterSettings(){
        IDialogSettings settings = getLogSettings();
        if (settings == null)
            settings = PDERuntimePlugin.getDefault().getDialogSettings().addNewSection(getClass().getName());
        settings.put(P_USE_LIMIT, fMemento.getString(P_USE_LIMIT).equals("true")); //$NON-NLS-1$
        settings.put(P_LOG_LIMIT, fMemento.getInteger(P_LOG_LIMIT).intValue());
        settings.put(P_LOG_INFO, fMemento.getString(P_LOG_INFO).equals("true")); //$NON-NLS-1$
        settings.put(P_LOG_WARNING, fMemento.getString(P_LOG_WARNING).equals("true")); //$NON-NLS-1$
        settings.put(P_LOG_ERROR, fMemento.getString(P_LOG_ERROR).equals("true")); //$NON-NLS-1$
        settings.put(P_SHOW_ALL_SESSIONS, fMemento.getString(P_SHOW_ALL_SESSIONS).equals("true")); //$NON-NLS-1$
    }
    
    private void writeViewSettings(){
        Preferences preferences = getLogPreferences();
        preferences.setValue(P_COLUMN_1, fMemento.getInteger(P_COLUMN_1).intValue());
        preferences.setValue(P_COLUMN_2, fMemento.getInteger(P_COLUMN_2).intValue());
        preferences.setValue(P_COLUMN_3, fMemento.getInteger(P_COLUMN_3).intValue());
        preferences.setValue(P_ACTIVATE, fMemento.getString(P_ACTIVATE).equals("true")); //$NON-NLS-1$
        int order = fMemento.getInteger(P_ORDER_VALUE).intValue();
        preferences.setValue(P_ORDER_VALUE, order == 0 ? DESCENDING : order);
        preferences.setValue(P_ORDER_TYPE, fMemento.getInteger(P_ORDER_TYPE).intValue());
    }
    
    public void sortByDateDescending() {
    	setColumnSorting(fColumn3, DESCENDING);
    }
}
