package org.eclipse.pde.internal.ui.model.plugin;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public class PluginObjectNode extends DocumentNode implements IPluginObject {
	
	private boolean fInTheModel;
	private ISharedPluginModel fModel;
	private String fName;
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getModel()
	 */
	public ISharedPluginModel getModel() {
		return fModel;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginModel()
	 */
	public IPluginModelBase getPluginModel() {
		return (IPluginModelBase)fModel;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return fName;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		return getResourceString(getName());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getParent()
	 */
	public IPluginObject getParent() {
		return (IPluginObject)getParentNode();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return fModel != null ? ((IPluginModelBase)fModel).getPluginBase() : null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return fModel != null ? fModel.getResourceString(key) : key;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		fName = name;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}
	
	public void setModel(ISharedPluginModel model) {
		fModel = model;
	}
}
