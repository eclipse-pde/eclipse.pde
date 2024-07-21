/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.PDEState;
import org.osgi.framework.VersionRange;
import org.w3c.dom.Node;

public class Fragment extends PluginBase implements IFragment {
	private static final long serialVersionUID = 1L;

	private String fPluginId = ""; //$NON-NLS-1$

	private String fPluginVersion = ""; //$NON-NLS-1$

	private int fMatchRule = IMatchRules.NONE;

	private boolean fPatch;

	public Fragment(boolean readOnly) {
		super(readOnly);
	}

	@Override
	public String getPluginId() {
		return fPluginId;
	}

	@Override
	public String getPluginVersion() {
		return fPluginVersion;
	}

	@Override
	public int getRule() {
		return fMatchRule;
	}

	@Override
	protected boolean hasRequiredAttributes() {
		if (fPluginId == null || fPluginVersion == null) {
			return false;
		}
		return super.hasRequiredAttributes();
	}

	@Override
	void load(BundleDescription bundleDescription, PDEState state) {
		HostSpecification host = bundleDescription.getHost();
		fPluginId = host.getName();
		VersionRange versionRange = host.getVersionRange();
		if (versionRange != null) {
			fPluginVersion = versionRange.getLeft().toString();
			fMatchRule = PluginBase.getMatchRule(versionRange);
		}
		fPatch = state.isPatchFragment(bundleDescription.getBundleId());
		super.load(bundleDescription, state);
	}

	@Override
	void load(Node node, String schemaVersion) {
		fPluginId = getNodeAttribute(node, "plugin-id"); //$NON-NLS-1$
		fPluginVersion = getNodeAttribute(node, "plugin-version"); //$NON-NLS-1$
		String match = getNodeAttribute(node, "match"); //$NON-NLS-1$
		if (match != null) {
			String[] table = IMatchRules.RULE_NAME_TABLE;
			for (int i = 0; i < table.length; i++) {
				if (match.equalsIgnoreCase(table[i])) {
					fMatchRule = i;
					break;
				}
			}
		}
		super.load(node, schemaVersion);
	}

	@Override
	public void reset() {
		fPluginId = ""; //$NON-NLS-1$
		fPluginVersion = ""; //$NON-NLS-1$
		fMatchRule = IMatchRules.NONE;
		super.reset();
	}

	@Override
	public void setPluginId(String newPluginId) throws CoreException {
		ensureModelEditable();
		String oldValue = this.fPluginId;
		fPluginId = newPluginId;
		firePropertyChanged(P_PLUGIN_ID, oldValue, fPluginId);
	}

	@Override
	public void setPluginVersion(String newPluginVersion) throws CoreException {
		ensureModelEditable();
		String oldValue = this.fPluginVersion;
		fPluginVersion = newPluginVersion;
		firePropertyChanged(P_PLUGIN_VERSION, oldValue, fPluginVersion);
	}

	@Override
	public void setRule(int rule) throws CoreException {
		ensureModelEditable();
		Integer oldValue = Integer.valueOf(this.fMatchRule);
		fMatchRule = rule;
		firePropertyChanged(P_RULE, oldValue, Integer.valueOf(rule));
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_PLUGIN_ID)) {
			setPluginId(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_PLUGIN_VERSION)) {
			setPluginVersion(newValue != null ? newValue.toString() : null);
			return;
		}
		if (name.equals(P_RULE)) {
			setRule(((Integer) newValue).intValue());
			return;
		}
		super.restoreProperty(name, oldValue, newValue);
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		if (getSchemaVersion() != null) {
			writer.println("<?eclipse version=\"" + getSchemaVersion() + "\"?>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.print("<fragment"); //$NON-NLS-1$
		if (getId() != null) {
			writer.println();
			writer.print("   id=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getName() != null) {
			writer.println();
			writer.print("   name=\"" + getWritableString(getName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getVersion() != null) {
			writer.println();
			writer.print("   version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getProviderName() != null) {
			writer.println();
			writer.print("   provider-name=\"" + getWritableString(getProviderName()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String pid = getPluginId();
		if (pid != null && pid.length() > 0) {
			writer.println();
			writer.print("   plugin-id=\"" + getPluginId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String pver = getPluginVersion();
		if (pver != null && pver.length() > 0) {
			writer.println();
			writer.print("   plugin-version=\"" + getPluginVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getRule() != IMatchRules.NONE) {
			writer.println();
			writer.print("   match=\"" + IMatchRules.RULE_NAME_TABLE[getRule()] + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(">"); //$NON-NLS-1$
		writer.println();

		String firstIndent = "   "; //$NON-NLS-1$

		// add runtime
		Object[] children = getLibraries();
		if (children.length > 0) {
			writeChildren(firstIndent, "runtime", children, writer); //$NON-NLS-1$
			writer.println();
		}

		// add requires
		children = getImports();
		if (children.length > 0) {
			writeChildren(firstIndent, "requires", children, writer); //$NON-NLS-1$
			writer.println();
		}

		children = getExtensionPoints();
		if (children.length > 0) {
			for (Object element : children) {
				((IPluginExtensionPoint) element).write(firstIndent, writer);
			}
			writer.println();
		}

		// add extensions
		children = getExtensions();
		for (Object element : children) {
			((IPluginExtension) element).write(firstIndent, writer);
		}
		writer.println("</fragment>"); //$NON-NLS-1$
	}

	public boolean isPatch() {
		return fPatch;
	}
}
