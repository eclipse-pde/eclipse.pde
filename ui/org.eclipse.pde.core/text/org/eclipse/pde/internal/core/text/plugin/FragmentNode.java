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
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IMatchRules;

public class FragmentNode extends PluginBaseNode implements IFragment {
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginId()
	 */
	public String getPluginId() {
		return getXMLAttributeValue(P_PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		return getXMLAttributeValue(P_PLUGIN_VERSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getRule()
	 */
	public int getRule() {
		String match = getXMLAttributeValue("match"); //$NON-NLS-1$
		if (match == null || match.trim().length() == 0)
			return IMatchRules.NONE;
		if (match.equals("compatible")) //$NON-NLS-1$
			return IMatchRules.COMPATIBLE;
		if (match.equals("perfect")) //$NON-NLS-1$
			return IMatchRules.PERFECT;
		if (match.equals("equivalent")) //$NON-NLS-1$
			return IMatchRules.EQUIVALENT;
		return IMatchRules.GREATER_OR_EQUAL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginId(java.lang.String)
	 */
	public void setPluginId(String id) throws CoreException {
		setXMLAttribute(P_PLUGIN_ID, id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
		setXMLAttribute(P_PLUGIN_VERSION, version);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
		String match = ""; //$NON-NLS-1$
		switch (rule) {
			case IMatchRules.COMPATIBLE :
				match = "compatible"; //$NON-NLS-1$
				break;
			case IMatchRules.EQUIVALENT :
				match = "equivalent"; //$NON-NLS-1$
				break;
			case IMatchRules.PERFECT :
				match = "perfect"; //$NON-NLS-1$
				break;
			case IMatchRules.GREATER_OR_EQUAL :
				match = "greaterOrEqual"; //$NON-NLS-1$
		}
		setXMLAttribute(P_RULE, match);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginBaseNode#getSpecificAttributes()
	 */
	protected String[] getSpecificAttributes() {
		ArrayList result = new ArrayList();

		String pluginID = getPluginId();
		if (pluginID != null && pluginID.trim().length() > 0)
			result.add("   " + P_PLUGIN_ID + "=\"" + pluginID + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String pluginVersion = getPluginVersion();
		if (pluginVersion != null && pluginVersion.trim().length() > 0)
			result.add("   " + P_PLUGIN_VERSION + "=\"" + pluginVersion + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		String match = getXMLAttributeValue(P_RULE);
		if (match != null && match.trim().length() > 0)
			result.add("   " + P_RULE + "=\"" + match + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		return (String[]) result.toArray(new String[result.size()]);
	}
}
