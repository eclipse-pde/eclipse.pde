package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;
import org.xml.sax.SAXException;

public abstract class AbstractPluginDocumentHandler extends DocumentHandler {
	
	private PluginModelBase fModel;
	private String fSchemaVersion;

	/**
	 * @param model
	 */
	public AbstractPluginDocumentHandler(PluginModelBase model) {
		fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.DocumentHandler#getDocument()
	 */
	protected IDocument getDocument() {
		return fModel.getDocument();
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		IPluginBase pluginBase = fModel.getPluginBase();
		try {
			if (pluginBase != null)
				pluginBase.setSchemaVersion(fSchemaVersion);
		} catch (CoreException e) {
		}
	}
	

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		if ("eclipse".equals(target)) {
			fSchemaVersion = "3.0";
		}
	}
	
	protected PluginModelBase getModel() {
		return fModel;
	}
}
