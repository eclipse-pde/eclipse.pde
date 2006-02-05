package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.xhtml.TocReplaceTable.TocReplaceEntry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;


public class XHTMLConversionOperation implements IWorkspaceRunnable {
	
	private TocReplaceEntry[] fEntries;
	private Shell fShell;
	private Hashtable fTocs = new Hashtable();
	private XHTMLConverter fConverter;
	
	public XHTMLConversionOperation(TocReplaceEntry[] entries, Shell shell) {
		fShell = shell;
		fEntries = entries;
		fConverter = new XHTMLConverter(XHTMLConverter.XHTML_TRANSITIONAL);
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		MultiStatus ms = new MultiStatus(
				"org.eclipse.pde.ui", IStatus.OK, //$NON-NLS-1$
				PDEUIMessages.XHTMLConversionOperation_failed, null);
		
		monitor.beginTask(PDEUIMessages.XHTMLConversionOperation_taskName, fEntries.length * 4);
		
		for (int i = 0; i < fEntries.length; i++) {
			convert(fEntries[i], ms, monitor);
		}
		
		if (fTocs.size() > 0) {
			Iterator it = fTocs.keySet().iterator();
			while (it.hasNext()) {
				IFile tocFile = (IFile)it.next();
				ArrayList changeList = (ArrayList)fTocs.get(tocFile);
				updateToc(tocFile, changeList, monitor);
			}
		}
		
		checkFailed(ms);
	}

	private void checkFailed(final MultiStatus ms) {
		if (ms.getChildren().length > 0) {
			fShell.getDisplay().syncExec(new Runnable() {
				public void run() {
					String message;
					if (ms.getChildren().length == 1)
						message = PDEUIMessages.XHTMLConversionOperation_1prob;
					else
						message = NLS.bind(PDEUIMessages.XHTMLConversionOperation_multiProb, Integer.toString(ms.getChildren().length));
					ErrorDialog.openError(fShell, PDEUIMessages.XHTMLConversionOperation_title, message, ms);
				}
			});
		}
	}
	
	private void convert(TocReplaceEntry entry, MultiStatus ms, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		monitor.subTask(NLS.bind(PDEUIMessages.XHTMLConversionOperation_createXHTML, entry.getHref()));
		entry.setReplacement(fConverter.prepareXHTMLFileName(entry.getHref()));
		try {
			fConverter.convert(entry.getOriginalFile(), monitor);
			addTocUpdate(entry);
		} catch (CoreException e) {
			ms.add(new Status(
					IStatus.WARNING, "org.eclipse.pde.ui",  //$NON-NLS-1$
					IStatus.OK, entry.getTocFile().getName(), e));
		}
	}
	
	private void addTocUpdate(TocReplaceEntry entry) {
		if (fTocs.containsKey(entry.getTocFile())) {
			ArrayList tocList = (ArrayList)fTocs.get(entry.getTocFile());
			tocList.add(entry);
		} else {
			ArrayList tocList = new ArrayList();
			tocList.add(entry);
			fTocs.put(entry.getTocFile(), tocList);
		}
	}

	private void updateToc(final IFile tocFile, final ArrayList changeList, final IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		fShell.getDisplay().syncExec(new Runnable() {
			public void run() {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				try {
					try {
						manager.connect(tocFile.getFullPath(), monitor);
						ITextFileBuffer buffer = manager.getTextFileBuffer(tocFile.getFullPath());
						IDocument doc = buffer.getDocument();
						
						monitor.subTask(NLS.bind(PDEUIMessages.XHTMLConversionOperation_updatingToc, tocFile.getName()));
						Iterator it = changeList.iterator();
						while (it.hasNext()) {
							TocReplaceEntry entry = (TocReplaceEntry)it.next();
							FindReplaceDocumentAdapter frda = new FindReplaceDocumentAdapter(doc);
							String findString = "\"" + entry.getHref() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
							String replacement = entry.getReplacement();
							String replaceString = "\"" + replacement + "\""; //$NON-NLS-1$ //$NON-NLS-2$
							
							frda.find(0, findString, true, false, false, false);
							try {
								frda.replace(replaceString, false);
							} catch (IllegalStateException illegalState1) { // did not find anything...
								findString = "'" + entry.getHref() + "'";   // search for attribute with singlequotes //$NON-NLS-1$ //$NON-NLS-2$
								replaceString = "'" + replacement + "'"; //$NON-NLS-1$ //$NON-NLS-2$
								frda.find(0, findString, true, false, false, false);
								try {
									frda.replace(replaceString, false);
								} catch (IllegalStateException illegalState2) { // did not find anything, move on
									monitor.worked(1);
									continue;
								}
							}
							monitor.worked(1);
						}
						buffer.commit(monitor, true);
					} catch (MalformedTreeException e) {
					} catch (BadLocationException e) {
					} finally {
						manager.disconnect(tocFile.getFullPath(), monitor);
					}
				} catch (CoreException e) {
				}
			}
		});
	}
}
