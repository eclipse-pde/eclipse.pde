/*
 * Created on Sep 12, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.standalone.manifest;

import org.w3c.dom.*;

/**
 * @author melhem
 */

public class ManifestEditorUtil {
	
	public static boolean isPluginNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("plugin") && isTopLevelNode(node);
		return false;
	}

	public static boolean isFragmentNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("fragment") && isTopLevelNode(node);
		return false;
	}
	
	public static boolean isLibraryNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("library") && isRuntimeNode(node.getParentNode());
		return false;
	}
	
	public static boolean isExtensionNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("extension") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isExtensionPointNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("extension-point") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isRequiresNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("requires") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isRuntimeNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("runtime") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isImportNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("import") && isRequiresNode(node.getParentNode());
		return false;
	}
	
	private static boolean isLevelTwoNode(Node node) {
		if (node != null)
			return isPluginNode(node.getParentNode()) || isFragmentNode(node.getParentNode());
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
