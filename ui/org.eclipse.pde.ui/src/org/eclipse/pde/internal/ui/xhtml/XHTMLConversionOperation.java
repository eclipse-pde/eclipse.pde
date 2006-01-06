package org.eclipse.pde.internal.ui.xhtml;

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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.ui.xhtml.TocReplaceTable.TocReplaceEntry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;


public class XHTMLConversionOperation implements IWorkspaceRunnable {
	
	private TocReplaceEntry[] fEntries;
	private Shell fShell;
	private Hashtable fTocs = new Hashtable();
	
	public XHTMLConversionOperation(TocReplaceEntry[] entries, Shell shell) {
		fShell = shell;
		fEntries = entries;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		XHTMLConverter converter = new XHTMLConverter(XHTMLConverter.XHTML_TRANSITIONAL);
		
		monitor.beginTask("XHTML Converter", fEntries.length * 3);
		long time = System.currentTimeMillis();
		int success = 0;
		for (int i = 0; i < fEntries.length; i++) {
			String replacement = converter.prepareXHTMLFileName(fEntries[i].getHref());
			fEntries[i].setReplacement(replacement);
			if (convert(fEntries[i], converter, monitor)) {
				addTocUpdate(fEntries[i]);
				success++;
			}
		}
		
		monitor.worked(fEntries.length - success);
		
		if (fTocs.size() > 0) {
			Iterator it = fTocs.keySet().iterator();
			while (it.hasNext()) {
				IFile tocFile = (IFile)it.next();
				ArrayList changeList = (ArrayList)fTocs.get(tocFile);
				updateToc(tocFile, changeList, monitor);
			}
		}
		
		monitor.worked(fEntries.length - success);
		monitor.done();
		
		System.out.println("failed: " + (fEntries.length - success));
		System.out.println("succeeded: " + success);
		System.out.println("total time: " + (System.currentTimeMillis() - time));
	}

	private boolean convert(TocReplaceEntry entry, XHTMLConverter converter, IProgressMonitor monitor) throws CoreException {
		if (monitor.isCanceled())
			return false;
		monitor.subTask("creating " + entry.getHref() + " xhtml file");
		boolean success = converter.convert(entry.getOriginalFile(), monitor);
		monitor.worked(2);
		return success;
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
						
						Iterator it = changeList.iterator();
						while (it.hasNext()) {
							TocReplaceEntry entry = (TocReplaceEntry)it.next();
//							monitor.subTask("Updating toc entry: " + entry.getHref() + " and removing HTML file");
							FindReplaceDocumentAdapter frda = new FindReplaceDocumentAdapter(doc);
							String findString = "\"" + entry.getHref() + "\"";
							String replacement = entry.getReplacement();
//							String replacement = entry.getOriginalFile().getProjectRelativePath().toString();
							String replaceString = "\"" + replacement + "\"";
							
							frda.find(0, findString, true, false, false, false);
							try {
								frda.replace(replaceString, false);
							} catch (IllegalStateException illegalState1) { // did not find anything...
								findString = "'" + entry.getHref() + "'";   // search for attribute with singlequotes
								replaceString = "'" + replacement + "'";
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
