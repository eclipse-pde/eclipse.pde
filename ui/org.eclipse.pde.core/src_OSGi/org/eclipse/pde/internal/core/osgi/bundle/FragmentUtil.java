/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.util.StringTokenizer;

import org.eclipse.pde.core.osgi.bundle.IBundle;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IMatchRules;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FragmentUtil {
	private String pluginId;
	private String pluginVersion;
	private int match;
	
	public FragmentUtil() {
	}	

	public FragmentUtil(String value) {
		if (value!=null)
			setHeader(value);
	}
	
	public FragmentUtil(IFragment fragment) {
		pluginId = fragment.getPluginId();
		pluginVersion = fragment.getPluginVersion();
		match = fragment.getRule();
	}
	
	public String getPluginId() {
		return pluginId;
	}
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}
	public String getPluginVersion() {
		return pluginVersion;
	}
	public void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}
	public int getMatch() {
		return match;
	}
	public void setMatch(int match) {
		this.match = match;
	}
	public String getHeader() {
		if (pluginId==null) return null;
		StringBuffer hostBundle = new StringBuffer();

		hostBundle.append(pluginId);
		if (pluginVersion!=null) {
			hostBundle.append("; version=");
			hostBundle.append(pluginVersion);
			if (match!=IMatchRules.NONE) {
				hostBundle.append("; match=");
				hostBundle.append(getEquivalentRule(match));
			}
		}
		return hostBundle.toString();
	}
	public void setHeader(String header) {
		StringTokenizer stok = new StringTokenizer(header, ";");

		if (stok.hasMoreTokens()) {
			pluginId = stok.nextToken().trim();
		}
		if (stok.hasMoreTokens()) {
			String vtoken = stok.nextToken().trim();
			int loc = vtoken.indexOf('=');
			pluginVersion = vtoken.substring(loc+1).trim();
		}
		if (stok.hasMoreTokens()) {
			String mtoken = stok.nextToken().trim();
			int loc = mtoken.indexOf('=');
			String matchName = mtoken.substring(loc+1).trim();
			match = getEquivalentRule(matchName);
		}
	}
	private int getEquivalentRule(String name) {
		if (name != null) {
			String mname = name.toLowerCase();
			if (mname.equals(IBundle.PERFECT_MATCH))
				return IMatchRules.PERFECT;
			if (mname.equals(IBundle.EQUIVALENT_MATCH))
				return IMatchRules.EQUIVALENT;
			if (mname.equals(IBundle.COMPATIBLE_MATCH))
				return IMatchRules.COMPATIBLE;
			if (mname.equals(IBundle.GREATERTHANOREQUAL_MATCH))
				return IMatchRules.GREATER_OR_EQUAL;
		}
		return IMatchRules.NONE;
	}
	
	private String getEquivalentRule(int rule) {
		String ruleName;
		switch (rule) {
			case IMatchRules.PERFECT:
				ruleName = IBundle.PERFECT_MATCH;
				break;
			case IMatchRules.EQUIVALENT:
				ruleName = IBundle.EQUIVALENT_MATCH;
				break;
			case IMatchRules.COMPATIBLE:
				ruleName = IBundle.COMPATIBLE_MATCH;
				break;
			case IMatchRules.GREATER_OR_EQUAL:
				ruleName = IBundle.GREATERTHANOREQUAL_MATCH;
				break;
			case IMatchRules.NONE:
			default:
				ruleName=null;
		}
		return ruleName;
	}
}
