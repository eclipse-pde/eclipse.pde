package org.eclipse.pde.internal.ui.model.bundle;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.model.*;
import org.osgi.framework.*;

/**
 * @author melhem
 *
 */
public class BundleModel extends AbstractEditingModel implements IBundleModel {
	
	private Hashtable fHeaders = new Hashtable();
	private Hashtable fManifest = new Hashtable();
	/**
	 * @param document
	 * @param isReconciling
	 */
	public BundleModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundleModel#getManifest()
	 */
	public Dictionary getManifest() {
		return fManifest;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundleModel#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return fManifest.get(Constants.FRAGMENT_HOST) != null;
	}
	
	public Dictionary getHeaders() {
		return fHeaders;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fHeaders.clear();
			fManifest.clear();
			fLoaded = true;
			Manifest manifest = new Manifest(source);
			Map attributes = manifest.getMainAttributes();
			Iterator iter = attributes.keySet().iterator();
			while (iter.hasNext()) {
				Attributes.Name key = (Attributes.Name) iter.next();
				ManifestHeader header = new ManifestHeader();
				header.setName(key.toString());
				header.setValue((String)attributes.get(key));
				fHeaders.put(key.toString(), header);
				fManifest.put(key.toString(), attributes.get(key));
			}
			addOffsets();
		} catch (IOException e) {
			fLoaded = false;
		}
	}
	private void addOffsets() {
		IDocument document = getDocument();
		int lines = document.getNumberOfLines();
		try {
			IDocumentKey currentKey = null;
			for (int i = 0; i < lines; i++) {
				int offset = document.getLineOffset(i);
				int length = document.getLineLength(i);
				String line = document.get(offset, length);
				
				if (currentKey != null) {
					if (!line.startsWith(" ")) {
						currentKey.setLength(offset - 1 - currentKey.getOffset());
						currentKey = null;
					}
				} 

				if (currentKey == null) {
					int index = line.indexOf(':');				
					String name = (index != -1) ? line.substring(0, index) : line;
					currentKey = (IDocumentKey)fHeaders.get(name);
					if (currentKey != null) {
						currentKey.setOffset(offset);
						currentKey.setLength(offset + document.getLineLength(i) - currentKey.getOffset());
					}
				}
			}
		} catch (BadLocationException e) {
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ibundle.IBundleModel#getFactory()
	 */
	public IBundleModelFactory getFactory() {
		return null;
	}
	
}
