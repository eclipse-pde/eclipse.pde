/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteBuild extends SiteBuildObject implements ISiteBuild {
	final static String INDENT = "   "; //$NON-NLS-1$
	private Vector features = new Vector();
	public static final String DEFAULT_PLUGIN_DIR = "plugins"; //$NON-NLS-1$
	public static final String DEFAULT_FEATURE_DIR = "features"; //$NON-NLS-1$
	private final IPath pluginLocation = new Path(DEFAULT_PLUGIN_DIR);
	private final IPath featureLocation = new Path(DEFAULT_FEATURE_DIR);
	private boolean useConsole;
	private boolean autobuild;
	private boolean scrubOutput;


	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getType()
	 */
	public IPath getPluginLocation() {
		return pluginLocation;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getType()
	 */
	public IPath getFeatureLocation() {
		return featureLocation;
	}
	
	public void setAutobuild(boolean value) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(this.autobuild);
		this.autobuild = value;
		firePropertyChanged(P_AUTOBUILD, oldValue, new Boolean(value));
	}
	
	public boolean isAutobuild() {
		return autobuild;
	}
	
	public void setScrubOutput(boolean value) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(this.scrubOutput);
		this.scrubOutput = value;
		firePropertyChanged(P_AUTOBUILD, oldValue, new Boolean(value));
	}

	public boolean getScrubOutput() {
		return scrubOutput;
	}
	
	public void setShowConsole(boolean value) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(this.useConsole);
		this.useConsole = value;
		firePropertyChanged(P_SHOW_CONSOLE, oldValue, new Boolean(value));
	}

	public boolean getShowConsole() {
		return useConsole;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#addFeatures(org.eclipse.pde.internal.core.isite.ISiteFeature)
	 */
	public void addFeatures(ISiteBuildFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteBuildFeature feature = newFeatures[i];
			((SiteBuildFeature) feature).setInTheModel(true);
			features.add(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.INSERT);
	}


	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeFeatures(org.eclipse.pde.internal.core.isite.ISiteFeature)
	 */
	public void removeFeatures(ISiteBuildFeature[] newFeatures)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteBuildFeature feature = newFeatures[i];
			((SiteBuildFeature) feature).setInTheModel(false);
			features.remove(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getFeatures()
	 */
	public ISiteBuildFeature[] getFeatures() {
		return (ISiteBuildFeature[]) features.toArray(
			new ISiteBuildFeature[features.size()]);
	}

	protected void reset() {
		features.clear();
		useConsole = false;
		autobuild = false;
		scrubOutput = false;
	}
	
	protected void parse(Node node) {
		String value = getNodeAttribute(node, "plugin-location"); //$NON-NLS-1$
		autobuild = getBooleanAttribute(node, "autobuild"); //$NON-NLS-1$
		scrubOutput = getBooleanAttribute(node, "scrub-output"); //$NON-NLS-1$
		useConsole = getBooleanAttribute(node, "use-console"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				parseChild(child);
			}
		}
	}

	protected void parseChild(Node child) {
		String tag = child.getNodeName().toLowerCase();
		if (tag.equals("feature")) { //$NON-NLS-1$
			ISiteBuildFeature feature = getModel().createFeature();
			((SiteBuildFeature) feature).parse(child);
			((SiteBuildFeature) feature).setInTheModel(true);
			features.add(feature);
		}
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
	    if (name.equals(P_AUTOBUILD)) {
			setAutobuild(newValue!=null?((Boolean)newValue).booleanValue():false);
		} else if (name.equals(P_SCRUB_OUTPUT)) {
			setScrubOutput(newValue!=null?((Boolean)newValue).booleanValue():false);
		} else if (name.equals(P_SHOW_CONSOLE)) {
			setShowConsole(newValue!=null?((Boolean)newValue).booleanValue():false);
		}
		else
			super.restoreProperty(name, oldValue, newValue);
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<site-build"); //$NON-NLS-1$
		String indent2 = indent + INDENT;
		String indenta = indent + INDENT + INDENT;
		writeIfDefined(indenta, writer, "autobuild", autobuild?"true":"false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writeIfDefined(indenta, writer, "scrub-output", scrubOutput?"true":"false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writeIfDefined(indenta, writer, "use-console", useConsole?"true":"false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println(">"); //$NON-NLS-1$

		writeChildren(indent2, features, writer);
		writer.println(indent + "</site-build>"); //$NON-NLS-1$
	}
	private void writeChildren(
		String indent,
		Vector children,
		PrintWriter writer) {
		for (int i = 0; i < children.size(); i++) {
			IWritable writable = (IWritable) children.get(i);
			writable.write(indent, writer);
		}
	}
	
	public void resetReferences() {
		for (int i=0; i<features.size(); i++) {
			ISiteBuildFeature sbfeature = (ISiteBuildFeature)features.get(i);
			sbfeature.setReferencedFeature(null);
		}
	}
	
	private void writeIfDefined(
		String indent,
		PrintWriter writer,
		String attName,
		String attValue) {
		if (attValue == null)
			return;
		writer.println();
		writer.print(indent + attName + "=\"" + attValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
