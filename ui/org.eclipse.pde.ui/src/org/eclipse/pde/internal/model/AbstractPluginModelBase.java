package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.apache.xerces.parsers.*;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.*;
import org.eclipse.pde.internal.*;


public abstract class AbstractPluginModelBase extends AbstractModel implements IPluginModelBase {
	public static final String KEY_ERROR = "AbstractPluginModelBase.error";
	protected PluginBase pluginBase;
	private PluginModelFactory factory;
	private boolean enabled;

public AbstractPluginModelBase() {
	super();
}
public abstract IPluginBase createPluginBase();
public IPluginModelFactory getFactory() {
	if (factory == null) factory = new PluginModelFactory(this);
	return factory;
}
public IPluginBase getPluginBase() {
	return pluginBase;
}
public boolean isEnabled() {
	return enabled;
}
public boolean isFragmentModel() {
	return false;
}
public void load(InputStream stream) throws CoreException {
	XMLErrorHandler errorHandler = new XMLErrorHandler();
	DOMParser parser = new DOMParser();
	parser.setErrorHandler(errorHandler);
	if (pluginBase == null) {
		pluginBase = (PluginBase)createPluginBase();
		pluginBase.setModel(this);
	}
	pluginBase.reset();
	try {
		InputSource source = new InputSource(stream);
		parser.parse(source);
		if (errorHandler.getErrorCount()>0 ||
			errorHandler.getFatalErrorCount()>0) {
				throwParseErrorsException();
		}
		processDocument(parser.getDocument());
		loaded=true;
	} catch (SAXException e) {
		throwParseErrorsException();
	} catch (IOException e) {
		throwParseErrorsException();
	}
}
private void processDocument(Document doc) {
	Node pluginNode = doc.getDocumentElement();
	pluginBase.load(pluginNode);
}
public void reload(InputStream stream) throws CoreException {
/*
	if (pluginBase != null)
		pluginBase.reset();
*/
	load(stream);
	fireModelChanged(
		new ModelChangedEvent(
			IModelChangedEvent.WORLD_CHANGED,
			new Object[] { pluginBase },
			null));
}
public void setEnabled(boolean newEnabled) {
	enabled = newEnabled;
}
private void throwParseErrorsException() throws CoreException {
	Status status =
		new Status(
			IStatus.ERROR,
			PDEPlugin.getPluginId(),
			IStatus.OK,
			PDEPlugin.getResourceString(KEY_ERROR),
			null);
	throw new CoreException(status);
}
public String toString() {
	IPluginBase pluginBase = getPluginBase();
	if (pluginBase!=null) return getResourceString(pluginBase.getName());
	return super.toString();
}
}
