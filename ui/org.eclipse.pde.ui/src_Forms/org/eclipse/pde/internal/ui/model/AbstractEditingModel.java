package org.eclipse.pde.internal.ui.model;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;

/**
 * @author melhem
 *
 */
public abstract class AbstractEditingModel extends PlatformObject implements IEditingModel {
	
	private ArrayList fListeners = new ArrayList();
	protected boolean fIsReconciling;
	protected boolean fIsInSync = true;
	protected boolean fIsValid;
	protected boolean fIsLoaded;
	protected boolean fIsDisposed;
	protected long fTimestamp;
	private transient NLResourceHelper fNLResourceHelper;
	private IDocument fDocument;
	private boolean fDirty;
	
	public AbstractEditingModel(IDocument document, boolean isReconciling) {
		fDocument = document;
		fIsReconciling = isReconciling;		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#dispose()
	 */
	public void dispose() {
		if (fNLResourceHelper != null) {
			fNLResourceHelper.dispose();
			fNLResourceHelper = null;
		}
		fIsDisposed = true;
		fListeners.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		if (key == null || key.length() == 0)
			return "";
		
		if (fNLResourceHelper == null) 
			fNLResourceHelper = createNLResourceHelper();
		
		return (fNLResourceHelper == null) ? key : fNLResourceHelper.getResourceString(key);
	}

	protected abstract NLResourceHelper createNLResourceHelper();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isDisposed()
	 */
	public boolean isDisposed() {
		return fIsDisposed;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isEditable()
	 */
	public boolean isEditable() {
		return fIsReconciling;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isLoaded()
	 */
	public boolean isLoaded() {
		return fIsLoaded;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isInSync()
	 */
	public boolean isInSync() {
		return fIsInSync;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isValid()
	 */
	public boolean isValid() {
		return fIsValid;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#getTimeStamp()
	 */
	public final long getTimeStamp() {
		return fTimestamp;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync)
			throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isReconcilingModel()
	 */
	public boolean isReconcilingModel() {
		return fIsReconciling;
	}
	
	public IDocument getDocument() {
		return fDocument;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.text.IReconcilingParticipant#reconciled(org.eclipse.jface.text.IDocument)
	 */
	public final void reconciled(IDocument document) {
		if (isReconcilingModel()) {
			try {
				reload(getInputStream(document), false);
			} catch (Exception e) {
			} 	
		}
	}
	
	protected InputStream getInputStream(IDocument document) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(document.get().getBytes(getCharSetName()));
	}
	
	protected abstract String getCharSetName();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangeProvider#addModelChangedListener(org.eclipse.pde.core.IModelChangedListener)
	 */
	public void addModelChangedListener(IModelChangedListener listener) {
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangeProvider#fireModelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(true);
		for (int i = 0; i < fListeners.size(); i++) {
			((IModelChangedListener)fListeners.get(i)).modelChanged(event);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangeProvider#fireModelObjectChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void fireModelObjectChanged(Object object, String property, Object oldValue, Object newValue) {
		fireModelChanged(new ModelChangedEvent(this, object, property, oldValue, newValue));
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangeProvider#removeModelChangedListener(org.eclipse.pde.core.IModelChangedListener)
	 */
	public void removeModelChangedListener(IModelChangedListener listener) {
		fListeners.remove(listener);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#isDirty()
	 */
	public boolean isDirty() {
		return fDirty;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#save(java.io.PrintWriter)
	 */
	public void save(PrintWriter writer) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#setDirty(boolean)
	 */
	public void setDirty(boolean dirty) {
		this.fDirty = dirty;
	}
}
