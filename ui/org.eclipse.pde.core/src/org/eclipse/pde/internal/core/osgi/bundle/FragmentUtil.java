/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.util.StringTokenizer;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.osgi.framework.*;

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
			if (name.equalsIgnoreCase(Constants.VERSION_MATCH_QUALIFIER) || 
					name.equalsIgnoreCase(Constants.VERSION_MATCH_MICRO))
				return IMatchRules.PERFECT;
			if (name.equalsIgnoreCase(Constants.VERSION_MATCH_MINOR))
				return IMatchRules.EQUIVALENT;
			if (name.equalsIgnoreCase(Constants.VERSION_MATCH_GREATERTHANOREQUAL))
				return IMatchRules.GREATER_OR_EQUAL;
			return IMatchRules.COMPATIBLE;
		}
		return IMatchRules.NONE;
	}
	
	private String getEquivalentRule(int rule) {
		String ruleName;
		switch (rule) {
			case IMatchRules.PERFECT:
				ruleName = Constants.VERSION_MATCH_QUALIFIER;
				break;
			case IMatchRules.EQUIVALENT:
				ruleName = Constants.VERSION_MATCH_MINOR;
				break;
			case IMatchRules.GREATER_OR_EQUAL:
				ruleName = Constants.VERSION_MATCH_GREATERTHANOREQUAL;
				break;
			case IMatchRules.NONE:
			default:
				ruleName=null;
		}
		return ruleName;
	}
}
