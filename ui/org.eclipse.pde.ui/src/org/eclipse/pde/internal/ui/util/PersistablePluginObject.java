package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.runtime.*;
import org.eclipse.ui.*;


public class PersistablePluginObject extends PlatformObject implements
		IPersistableElement, IElementFactory {
	
	public static final String FACTORY_ID = "org.eclipse.pde.ui.elementFactory"; //$NON-NLS-1$	
	public static final String KEY = "org.eclipse.pde.workingSetKey"; //$NON-NLS-1$
	
	private String fPluginID;

	public PersistablePluginObject() {
	}
	
	public PersistablePluginObject(String pluginID) {
		fPluginID = pluginID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	public String getFactoryId() {
		return FACTORY_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		memento.putString(KEY, fPluginID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public IAdaptable createElement(IMemento memento) {
		return new PersistablePluginObject(memento.getString(KEY));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IPersistableElement.class))
			return this;
		return super.getAdapter(adapter);
	}
	
	public String getPluginID() {
		return fPluginID;
	}

}
