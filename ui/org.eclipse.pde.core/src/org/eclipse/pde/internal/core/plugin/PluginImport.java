/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.ManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.osgi.framework.Constants;
import org.w3c.dom.Node;

public class PluginImport extends IdentifiablePluginObject implements IPluginImport, Serializable {

	private static final long serialVersionUID = 1L;
	private int match = NONE;
	private boolean reexported = false;
	private boolean optional = false;
	private String version;

	public PluginImport() {
	}

	public PluginImport(ISharedPluginModel model, String id) {
		try {
			setModel(model);
			ensureModelEditable();
			this.fID = id;
		} catch (CoreException e) {
		}
	}

	public boolean isValid() {
		return getId() != null;
	}

	public int getMatch() {
		return match;
	}

	public String getVersion() {
		return version;
	}

	public boolean isReexported() {
		return reexported;
	}

	public boolean isOptional() {
		return optional;
	}

	public void load(BundleDescription description) {
		this.fID = description.getSymbolicName();
	}

	public void load(ManifestElement element, int bundleManifestVersion) {
		this.fID = element.getValue();
		if (bundleManifestVersion >= 2) {
			this.optional = Constants.RESOLUTION_OPTIONAL.equals(element.getDirective(Constants.RESOLUTION_DIRECTIVE));
			this.reexported = Constants.VISIBILITY_REEXPORT.equals(element.getDirective(Constants.VISIBILITY_DIRECTIVE));
		} else {
			this.optional = "true".equals(element.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
			this.reexported = "true".equals(element.getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE)); //$NON-NLS-1$
		}
		String bundleVersion = element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (bundleVersion != null) {
			try {
				VersionRange versionRange = new VersionRange(bundleVersion);
				this.version = bundleVersion;
				this.match = PluginBase.getMatchRule(versionRange);
			} catch (IllegalArgumentException e) {
			}
		}
	}

	public void load(BundleSpecification importModel) {
		this.fID = importModel.getName();
		this.reexported = importModel.isExported();
		this.optional = importModel.isOptional();
		VersionRange versionRange = importModel.getVersionRange();
		if (versionRange == null || VersionRange.emptyRange.equals(versionRange)) {
			this.version = null;
			match = IMatchRules.NONE;
		} else {
			this.version = versionRange.getMinimum() != null ? versionRange.getMinimum().toString() : null;
			match = PluginBase.getMatchRule(versionRange);
		}
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IPluginImport) {
			IPluginImport target = (IPluginImport) obj;
			// Objects from the same model must be
			// binary equal
			if (target.getModel().equals(getModel()))
				return false;

			if (target.getId().equals(getId()) && target.isReexported() == isReexported() && stringEqualWithNull(target.getVersion(), getVersion()) && target.getMatch() == getMatch() && target.isOptional() == isOptional())
				return true;
		}
		return false;
	}

	void load(Node node) {
		String id = getNodeAttribute(node, "plugin"); //$NON-NLS-1$
		String export = getNodeAttribute(node, "export"); //$NON-NLS-1$
		String option = getNodeAttribute(node, "optional"); //$NON-NLS-1$
		String version = getNodeAttribute(node, "version"); //$NON-NLS-1$
		String match = getNodeAttribute(node, "match"); //$NON-NLS-1$
		boolean reexport = export != null && export.toLowerCase(Locale.ENGLISH).equals("true"); //$NON-NLS-1$
		boolean optional = option != null && option.toLowerCase(Locale.ENGLISH).equals("true"); //$NON-NLS-1$
		this.match = NONE;
		if (match != null) {
			String lmatch = match.toLowerCase(Locale.ENGLISH);
			if (lmatch.equals("exact")) //$NON-NLS-1$
				lmatch = RULE_EQUIVALENT;
			for (int i = 0; i < RULE_NAME_TABLE.length; i++) {
				if (lmatch.equals(RULE_NAME_TABLE[i])) {
					this.match = i;
					break;
				}
			}
		}
		this.version = version;
		this.fID = id;
		this.reexported = reexport;
		this.optional = optional;
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Integer oldValue = new Integer(this.match);
		this.match = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public void setReexported(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(reexported);
		this.reexported = value;
		firePropertyChanged(P_REEXPORTED, oldValue, new Boolean(value));
	}

	public void setOptional(boolean value) throws CoreException {
		ensureModelEditable();
		Boolean oldValue = new Boolean(this.optional);
		this.optional = value;
		firePropertyChanged(P_OPTIONAL, oldValue, new Boolean(value));
	}

	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		String oldValue = this.version;
		this.version = version;
		firePropertyChanged(P_VERSION, oldValue, version);
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_MATCH)) {
			setMatch(((Integer) newValue).intValue());
			return;
		}
		if (name.equals(P_REEXPORTED)) {
			setReexported(((Boolean) newValue).booleanValue());
			return;
		}
		if (name.equals(P_OPTIONAL)) {
			setOptional(((Boolean) newValue).booleanValue());
			return;
		}
		if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// This is a round-about way to do this; but, leveraging existing
		// functionality is key.  The fact we have to do this suggests a model 
		// limitation. 
		// Emulating the behaviour of the text edit operations.  
		// RequireBundleObjects are created from PluginImport objects and have
		// access to the MANIFEST.MF write mechanism 

		// Get the model
		IPluginModelBase modelBase = getPluginModel();
		// Ensure the model is a bundle model
		if ((modelBase instanceof IBundlePluginModelBase) == false) {
			writer.print(indent);
			writer.print("<import plugin=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			if (isReexported())
				writer.print(" export=\"true\""); //$NON-NLS-1$
			if (isOptional())
				writer.print(" optional=\"true\""); //$NON-NLS-1$
			if (version != null && version.length() > 0)
				writer.print(" version=\"" + version + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			if (match != NONE && match != COMPATIBLE) {
				String matchValue = RULE_NAME_TABLE[match];
				writer.print(" match=\"" + matchValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			writer.println("/>"); //$NON-NLS-1$
			return;
		}
		IBundleModel bundleModel = ((IBundlePluginModelBase) modelBase).getBundleModel();
		// Ensure the bundle manifest is present
		if (bundleModel == null) {
			return;
		}
		// Get the bundle
		IBundle bundle = bundleModel.getBundle();
		// Get the require bundle manifest header
		IManifestHeader manifestHeader = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
		// Ensure the header was found (it has to be there since the calling
		// of this method is a result of a copy operation)
		if ((manifestHeader instanceof ManifestHeader) == false) {
			return;
		}
		ManifestHeader header = (ManifestHeader) manifestHeader;
		// Create the new temporary require bundle object (used only for 
		// writing)
		RequireBundleObject element = new RequireBundleObject(header, fID);
		// Get the manifest version for backwards compatibility
		int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(bundle);
		// Configure its properties using the values of this object
		// Field:  Optional
		if (optional) {
			if (bundleManifestVersion > 1) {
				element.setDirective(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
			} else {
				element.setAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE, "true"); //$NON-NLS-1$
			}
		}
		// Field:  Re-exported
		if (reexported) {
			if (bundleManifestVersion > 1) {
				element.setDirective(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT);
			} else {
				element.setAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE, "true"); //$NON-NLS-1$
			}
		}
		// Field:  Version
		if ((version != null) && (version.trim().length() > 0)) {
			element.setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version.trim());
		}
		// Write the textual representation
		writer.print(element.write());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.IdentifiablePluginObject#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, org.eclipse.pde.core.plugin.IPluginObject)
	 */
	public void reconnect(ISharedPluginModel model, IPluginObject parent) {
		super.reconnect(model, parent);
		// No transient fields
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.PluginObject#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		writer.println(',');
		writer.print(' ');
	}

}
