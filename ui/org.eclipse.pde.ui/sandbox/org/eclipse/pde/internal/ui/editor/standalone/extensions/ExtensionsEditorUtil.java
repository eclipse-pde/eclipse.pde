/*
 * Created on Oct 7, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.extensions;

import org.w3c.dom.*;

/**
 * @author melhem
 */
public class ExtensionsEditorUtil {

	public static boolean isTopLevelNode(Node node) {
		if (node != null) {
			if (node.getNodeName().equalsIgnoreCase("extensions")) {
				Node parent = node.getParentNode();
				return  parent != null && parent instanceof Document;
			}
		}
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
	
	private static boolean isLevelTwoNode(Node node) {
		if (node != null)
			return isTopLevelNode(node.getParentNode());
		return false;
	}
	

}
