package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public class PluginBaseNode extends PluginObjectNode implements IPluginBase {
	
	
	private String fSchemaVersion;
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void add(IPluginLibrary library) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		ArrayList result = new ArrayList();
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginElement) {
				if (((PluginElementNode)children[i]).getName().equals("runtime")) {
					IDocumentNode[] grandChildren = children[i].getChildNodes();
					for (int j = 0; j < grandChildren.length; j++) {
						if (grandChildren[j] instanceof IPluginLibrary)
							result.add(grandChildren[j]);
					}
					
				}
			}
		}
		return (IPluginLibrary[]) result.toArray(new IPluginLibrary[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getImports()
	 */
	public IPluginImport[] getImports() {
		ArrayList result = new ArrayList();
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginElement) {
				if (((PluginElementNode)children[i]).getName().equals("requires")) {
					IDocumentNode[] grandChildren = children[i].getChildNodes();
					for (int j = 0; j < grandChildren.length; j++) {
						if (grandChildren[j] instanceof IPluginImport)
							result.add(grandChildren[j]);
					}
					
				}
			}
		}
		return (IPluginImport[]) result.toArray(new IPluginImport[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		return getXMLAttributeValue(P_PROVIDER);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getVersion()
	 */
	public String getVersion() {
		return getXMLAttributeValue(P_VERSION);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		setXMLAttribute(P_PROVIDER, providerName);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		setXMLAttribute(P_VERSION, version);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginLibrary, org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void swap(IPluginLibrary l1, IPluginLibrary l2) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getSchemaVersion()
	 */
	public String getSchemaVersion() {
		return fSchemaVersion;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setSchemaVersion(java.lang.String)
	 */
	public void setSchemaVersion(String schemaVersion) throws CoreException {
		fSchemaVersion = schemaVersion;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void add(IPluginExtension extension) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void add(IPluginExtensionPoint extension) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensionPoints()
	 */
	public IPluginExtensionPoint[] getExtensionPoints() {
		ArrayList result = new ArrayList();
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginExtensionPoint)
				result.add(children[i]);
		}
		return (IPluginExtensionPoint[]) result.toArray(new IPluginExtensionPoint[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensions()
	 */
	public IPluginExtension[] getExtensions() {
		ArrayList result = new ArrayList();
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginExtension)
				result.add(children[i]);
		}
		return (IPluginExtension[]) result.toArray(new IPluginExtension[result.size()]);
	}
	public int getIndexOf(IPluginExtension e) {
		IPluginExtension [] children = getExtensions();
		for (int i=0; i<children.length; i++) {
			if (children[i].equals(e))
				return i;
		}
		return -1;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void remove(IPluginExtension extension) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void remove(IPluginExtensionPoint extensionPoint)
			throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension, org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2)
			throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}
	
}
