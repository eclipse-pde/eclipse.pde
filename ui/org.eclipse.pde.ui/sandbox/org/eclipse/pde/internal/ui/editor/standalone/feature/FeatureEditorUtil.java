/*
 * Created on Sep 13, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.feature;

import org.w3c.dom.*;

/**
 * @author melhem
 */
public class FeatureEditorUtil {
	
	public static boolean isCopyrightNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("copyright") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isDataNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("data") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isDescriptionNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("description") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isDiscoveryNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("discovery") && isURLNode(node.getParentNode());
		return false;
	}
	
	public static boolean isFeatureNode(Node node) {
		if (node != null) 
			return node.getNodeName().equals("feature") && isTopLevelNode(node);
		return false;		
	}
	
	public static boolean isImportFeatureNode(Node node) {
		if (node != null) {
			if (isImportNode(node))
				return ((Element) node).hasAttribute("feature");
		}
		return false;
	}

	public static boolean isImportNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("import") && isRequiresNode(node.getParentNode());
		return false;
	}
	
	public static boolean isImportPluginNode(Node node) {
		if (node != null) {
			if (isImportNode(node))
				return ((Element) node).hasAttribute("plugin");
		}
		return false;
	}
	
	public static boolean isIncludesNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("includes") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isInstallHandlerNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("install-handler") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isLicenseNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("license") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isPluginNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("plugin") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isRequiresNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("requires") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isUpdateNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("update") && isURLNode(node.getParentNode());
		return false;
	}
	
	public static boolean isURLNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("url") && isLevelTwoNode(node);
		return false;
	}
	
	private static boolean isLevelTwoNode(Node node) {
		if (node != null)
			return isFeatureNode(node.getParentNode());
		return false;
	}
	
	private static boolean isTopLevelNode(Node node) {
		if (node != null) {
			Node parent = node.getParentNode();
			return parent != null && parent instanceof Document;
		}
		return false;
	}

}
