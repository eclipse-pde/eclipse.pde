package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *
 */
public abstract class PluginBaseNode extends PluginObjectNode implements IPluginBase {
	
	
	private String fSchemaVersion;
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void add(IPluginLibrary library) throws CoreException {
		IDocumentNode parent = getEnclosingElement("runtime", true);
		if (library instanceof IDocumentNode) {
			library.setInTheModel(true);
			IDocumentNode node = (IDocumentNode)library;
			node.setParentNode(parent);
			parent.addChildNode(node);
			library.setInTheModel(true);
			fireStructureChanged(library, IModelChangedEvent.INSERT);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
		IDocumentNode parent = getEnclosingElement("requires", true);
		if (pluginImport instanceof IDocumentNode) {
			pluginImport.setInTheModel(true);
			IDocumentNode node = (IDocumentNode)pluginImport;
			node.setParentNode(parent);
			parent.addChildNode(node);
			fireStructureChanged(pluginImport, IModelChangedEvent.INSERT);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		IDocumentNode parent = getEnclosingElement("requires", false);
		if (parent != null) {
			parent.removeChildNode((IDocumentNode)pluginImport);
			pluginImport.setInTheModel(false);
			fireStructureChanged(pluginImport, IModelChangedEvent.REMOVE);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		ArrayList result = new ArrayList();
		IDocumentNode requiresNode = getEnclosingElement("runtime", false);
		if (requiresNode != null) {
			IDocumentNode[] children = requiresNode.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IPluginLibrary)
					result.add(children[i]);
			}
		}
		
		return (IPluginLibrary[]) result.toArray(new IPluginLibrary[result.size()]);
	}
	
	private IDocumentNode getEnclosingElement(String elementName, boolean create) {
		PluginElementNode element = null;
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IPluginElement) {
				if (((PluginElementNode)children[i]).getXMLTagName().equals(elementName)) {
					element = (PluginElementNode)children[i];
					break;
				}
			}
		}
		if (element == null && create) {
			element = new PluginElementNode();
			element.setXMLTagName(elementName);
			element.setParentNode(this);
			element.setModel(getModel());
			element.setInTheModel(true);
			if (elementName.equals("runtime")) {
				addChildNode(element, 0);
			} else if (elementName.equals("requires")) {
				if (children.length > 0 && children[0].getXMLTagName().equals("runtime")) {
					addChildNode(element, 1);
				} else {
					addChildNode(element, 0);
				}
			}			
		}
		return element;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getImports()
	 */
	public IPluginImport[] getImports() {
		ArrayList result = new ArrayList();
		IDocumentNode requiresNode = getEnclosingElement("requires", false);
		if (requiresNode != null) {
			IDocumentNode[] children = requiresNode.getChildNodes();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IPluginImport)
					result.add(children[i]);
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
		IDocumentNode parent = getEnclosingElement("runtime", false);
		if (parent != null) {
			parent.removeChildNode((IDocumentNode)library);
			library.setInTheModel(false);
			fireStructureChanged(library, IModelChangedEvent.REMOVE);
		}	
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
		IDocumentNode node = getEnclosingElement("runtime", false);
		if (node != null) {
			node.swap((IDocumentNode)l1, (IDocumentNode)l2);
			firePropertyChanged(node, P_LIBRARY_ORDER, l1, l2);
		}
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
	public void add(IPluginExtensionPoint extensionPoint) throws CoreException {
		extensionPoint.setInTheModel(true);
		IDocumentNode node = (IDocumentNode)extensionPoint;
		node.setParentNode(this);
		IPluginExtensionPoint[] extPoints = getExtensionPoints();
		if (extPoints.length > 0)
			addChildNode(node, indexOf((IDocumentNode)extPoints[extPoints.length - 1]) + 1);
		else {
			IDocumentNode requires = getEnclosingElement("requires", false);
			if (requires != null) {
				addChildNode(node, indexOf(requires) + 1);
			} else {
				IDocumentNode runtime = getEnclosingElement("runtime", false);
				if (runtime != null)
					addChildNode(node, indexOf(runtime) + 1);
				else
					addChildNode(node, 0);
			}
		}
		fireStructureChanged(extensionPoint, IModelChangedEvent.INSERT);
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
		if (extensionPoint instanceof IDocumentNode) {
			removeChildNode((IDocumentNode)extensionPoint);
			extensionPoint.setInTheModel(false);
			fireStructureChanged(extensionPoint, IModelChangedEvent.REMOVE);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension, org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2)
			throws CoreException {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginImport, org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void swap(IPluginImport import1, IPluginImport import2)
			throws CoreException {
		IDocumentNode node = getEnclosingElement("requires", false);
		if (node != null) {
			node.swap((IDocumentNode)import1, (IDocumentNode)import2);
			firePropertyChanged(node, P_IMPORT_ORDER, import1, import2);
		}
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String newLine = System.getProperty("line.separator");
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + newLine);
		if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			buffer.append("<?eclipse version=\"3.0\"?>" + newLine);
		}
		buffer.append(writeShallow(false) + newLine);
		
		IDocumentNode runtime = getEnclosingElement("runtime", false);
		if (runtime != null) {
			runtime.setLineIndent(getLineIndent() + 3);
			buffer.append(runtime.write(true) + newLine);
		}
		
		IDocumentNode requires = getEnclosingElement("requires", false);
		if (requires != null) {
			requires.setLineIndent(getLineIndent() + 3);
			buffer.append(requires.write(true) + newLine);
		}
		
		IPluginExtensionPoint[] extPoints = getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			IDocumentNode extPoint = (IDocumentNode)extPoints[i];
			extPoint.setLineIndent(getLineIndent() + 3);
			buffer.append(extPoint.write(true) + newLine);
		}
		
		IPluginExtension[] extensions = getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IDocumentNode extension = (IDocumentNode)extensions[i];
			extension.setLineIndent(getLineIndent() + 3);
			buffer.append(extension.write(true) + newLine);
		}
		
		buffer.append("</" + getXMLTagName() + ">");
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow()
	 */
	public String writeShallow(boolean terminate) {
		String newLine = System.getProperty("line.separator");
		StringBuffer buffer = new StringBuffer();
		buffer.append("<" + getXMLTagName());
		buffer.append(newLine);
		
		String id = getId();
		if (id != null && id.length() > 0)
			buffer.append("\tid=\"" + getWritableString(id) + "\"" + newLine);
		
		String version = getVersion();
		if (version != null && version.length() > 0)
			buffer.append("\tversion=\"" + getWritableString(version) + "\"" + newLine);
		
		String name = getName();
		if (name != null && name.length() > 0)
			buffer.append("\tname=\"" + getWritableString(name) + "\"" + newLine);
		
		String provider = getProviderName();
		if (provider != null && provider.length() > 0) {
			buffer.append("\tprovider-name=\"" + getWritableString(provider) + "\"");
		}
		
		String specific = getSpecificAttributes();
		if (specific.length() > 0)
			buffer.append(newLine + specific);
		if (terminate)
			buffer.append("/");
		buffer.append(">");

		return buffer.toString();
	}
	
	protected abstract String getSpecificAttributes();
	
}
