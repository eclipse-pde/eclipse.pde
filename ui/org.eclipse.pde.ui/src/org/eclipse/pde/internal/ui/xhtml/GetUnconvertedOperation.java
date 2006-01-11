package org.eclipse.pde.internal.ui.xhtml;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.builders.ValidatingSAXParser;
import org.eclipse.pde.internal.builders.XMLErrorReporter;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetUnconvertedOperation implements IRunnableWithProgress {

	private static final String F_TOC_EXTENSION = "org.eclipse.help.toc"; //$NON-NLS-1$
	
	private IFile fBaseFile;
	private TocReplaceTable fReplaceTable = new TocReplaceTable();
	
	public GetUnconvertedOperation(ISelection selection) {
		Object object = ((IStructuredSelection)selection).getFirstElement();
		if (object instanceof IFile)
			fBaseFile = (IFile)object;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		if (fBaseFile == null)
			return;
		fReplaceTable.clear();
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(fBaseFile.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(fBaseFile.getFullPath());
			IDocument document = buffer.getDocument();
			PluginModelBase pluginModel = new PluginModel(document, false);
			inspectModel(monitor, pluginModel);
		} catch (CoreException e) {
		} finally {
			try {
				manager.disconnect(fBaseFile.getFullPath(), monitor);
			} catch (CoreException e) {
			}
		}

	}

	private void inspectModel(IProgressMonitor monitor, PluginModelBase pluginModel) throws CoreException {
		if (!pluginModel.isLoaded())
			pluginModel.load();
		
		ArrayList tocs = grabTocFiles(monitor, pluginModel);
		if (tocs.size() == 0)
			return;
		Iterator it = tocs.iterator();
		while (it.hasNext()) {
			IFile file = (IFile)it.next();
			if (file.getName().equals("toc.xml")) //$NON-NLS-1$
				file.getName().toString();
			XMLErrorReporter xml = new XMLErrorReporter(file);
			ValidatingSAXParser.parse(file, xml);
			Element root = xml.getDocumentRoot();
			if (root != null) {
				NodeList children = root.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					if (children.item(i) instanceof Element) {
						checkXML((Element)children.item(i), file);
					}
				}
			}
		}
	}

	private void checkXML(Element root, IFile file) {
		String href = root.getAttribute("href"); //$NON-NLS-1$
		if (href != null 
				&& (href.endsWith(".html")  //$NON-NLS-1$
				|| href.endsWith(".htm") //$NON-NLS-1$
				|| href.endsWith(".xhtml"))) { //$NON-NLS-1$
			fReplaceTable.addToTable(href, root.getAttribute("label"), file); //$NON-NLS-1$
		}
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				checkXML((Element)children.item(i), file);
			}
		}
	}
	
	private ArrayList grabTocFiles(IProgressMonitor monitor, PluginModelBase pluginModel) {
		IPluginExtension[] extensions = pluginModel.getPluginBase().getExtensions();
		ArrayList tocLocations = new ArrayList();
		for (int i = 0; i < extensions.length && !monitor.isCanceled(); i++) {
			if (extensions[i].getPoint().equals(F_TOC_EXTENSION)
					&& !monitor.isCanceled()) {
				IPluginObject[] children = extensions[i].getChildren();
				for (int j = 0; j < children.length && !monitor.isCanceled(); j++) {
					if (children[j].getName().equals("toc") //$NON-NLS-1$
							&& children[j] instanceof IPluginElement) {
						IPluginElement element = (IPluginElement) children[j];
						IPluginAttribute fileAttrib = element.getAttribute("file"); //$NON-NLS-1$
						if (fileAttrib != null) {
							String location = fileAttrib.getValue();
							IProject project = fBaseFile.getProject();
							IFile file = project.getFile(location);
							if (file != null && file.exists())
								tocLocations.add(file);
						}
					}
				}
			}
		}
		return tocLocations;
	}

	public boolean needsWork() {
		return fReplaceTable.numEntries() > 0;
	}

	public TocReplaceTable getChanges() {
		return fReplaceTable;
	}
}
