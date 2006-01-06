package org.eclipse.pde.internal.ui.xhtml;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class TocReplaceTable {

	protected class TocReplaceEntry implements IAdaptable {
		private String fEntry;
		private String fLabel;
		private String fReplacement;
		private IFile fTocFile;
		private IFile fEntryFile;
		private TocReplaceEntry(String replaceString, String title, IFile parentFile) {
			fLabel = title;
			fEntry = replaceString;
			fTocFile = parentFile;
			if (fEntry != null && fTocFile != null)
				fEntryFile = fTocFile.getProject().getFile(fEntry);
		}
		public String getHref() {
			return fEntry;
		}
		public IFile getTocFile() {
			return fTocFile;
		}
		public String getLabel() {
			return fLabel;
		}
		public String getCompleteEntryPath() {
			return fEntryFile.getLocation().toString();
		}
		public boolean fileMissing() {
			return fEntryFile == null || !fEntryFile.exists();
		}
		public IFile getOriginalFile() {
			return fEntryFile;
		}
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class)
				return new IWorkbenchAdapter() {
					public Object[] getChildren(Object o) {
						return null;
					}
					public ImageDescriptor getImageDescriptor(Object object) {
						if (fileMissing())
							return PDEPluginImages.DESC_ALERT_OBJ;
						return PDEPluginImages.DESC_DISCOVERY;
					}
					public String getLabel(Object o) {
						String label = TocReplaceEntry.this.getLabel();
						String href = TocReplaceEntry.this.getHref();
						return label == null ? href : href + " (" + label + ")";
					}
					public Object getParent(Object o) {
						return TocReplaceEntry.this.getTocFile();
					}
				};
			return null;
		}
		public void setReplacement(String string) {
			fReplacement = string;
		}
		public String getReplacement() {
			return fReplacement;
		}
	}
	
	private Hashtable fEntries = new Hashtable();
	private Hashtable fInvalidEntries = new Hashtable();
	private int fNumValidEntries;
	
	public void addToTable(String href, String title, IFile tocFile) {
		TocReplaceEntry tro = new TocReplaceEntry(href, title, tocFile);
		if (href.endsWith(".xhtml") && !tro.fileMissing())
			return;
		Hashtable entries = tro.fileMissing() ? fInvalidEntries : fEntries;
		if (entries.containsKey(tocFile)) {
			ArrayList tocList = (ArrayList)entries.get(tocFile);
			tocList.add(tro);
		} else {
			ArrayList tocList = new ArrayList();
			tocList.add(tro);
			entries.put(tocFile, tocList);
		}
		if (!tro.fileMissing())
			fNumValidEntries++;
	}
	
	public IFile[] getTocs(boolean invalid) {
		Set keys = invalid ? fInvalidEntries.keySet() : fEntries.keySet();
		Iterator it = keys.iterator();
		IFile[] files = new IFile[invalid ? 
				fInvalidEntries.size() : 
					fEntries.size()];
		int i = 0;
		while (it.hasNext())
			files[i++] = (IFile)it.next();
		return files;
	}
	
	public TocReplaceEntry[] getToBeConverted(IFile file, boolean invalid) {
		ArrayList list = invalid ? 
				(ArrayList)fInvalidEntries.get(file) :
					(ArrayList)fEntries.get(file);
		return (TocReplaceEntry[]) list.toArray(new TocReplaceEntry[list.size()]);
	}

	public int numEntries() {
		return fEntries.size() + fInvalidEntries.size();
	}
	
	public int numValidEntries() {
		return fNumValidEntries;
	}
	
	public void clear() {
		fNumValidEntries = 0;
		fEntries.clear();
		fInvalidEntries.clear();
	}
	
	public boolean containsInvalidEntires() {
		return fInvalidEntries.size() > 0;
	}
}
