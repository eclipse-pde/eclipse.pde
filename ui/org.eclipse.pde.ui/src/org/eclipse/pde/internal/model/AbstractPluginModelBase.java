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
import org.eclipse.pde.internal.builders.SourceDOMParser;


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
public IPluginBase getPluginBase(boolean createIfMissing) {
	if (pluginBase==null) {
		pluginBase = (PluginBase)createPluginBase();
	}
	return pluginBase;
}
public boolean isEnabled() {
	return enabled;
}
public boolean isFragmentModel() {
	return false;
}
public void load(InputStream stream, boolean outOfSync) throws CoreException {
	XMLErrorHandler errorHandler = new XMLErrorHandler();
	SourceDOMParser parser = new SourceDOMParser();
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
		processDocument(parser.getDocument(), parser.getLineTable());
		loaded=true;
		if (!outOfSync) updateTimeStamp();
	} catch (SAXException e) {
		throwParseErrorsException();
	} catch (IOException e) {
		throwParseErrorsException();
	}
}
private void processDocument(Document doc, Hashtable lineTable) {
	Node pluginNode = doc.getDocumentElement();
	pluginBase.load(pluginNode, lineTable);
}
public void reload(InputStream stream, boolean outOfSync) throws CoreException {
	load(stream, outOfSync);
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
	if (pluginBase!=null) return pluginBase.getTranslatedName();
	return super.toString();
}

protected abstract void updateTimeStamp();
}
