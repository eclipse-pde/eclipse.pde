/*
 * Created on Sep 14, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.site;

import org.w3c.dom.*;

/**
 * @author melhem
 */
public class SiteEditorUtil {
	
	public static boolean isArchiveNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("archive") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isCategoryDefNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("category-def") && isLevelTwoNode(node);
		return false;
	}
	
	public static boolean isCategoryNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("category") && isFeatureNode(node.getParentNode());
		return false;
	}
	
	public static boolean isDescriptionNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("description")
				&& (isLevelTwoNode(node) || isCategoryDefNode(node.getParentNode()));
		return false;
	}
	
	public static boolean isFeatureNode(Node node) {
		if (node != null)
			return node.getNodeName().equals("feature") && isLevelTwoNode(node);
		return false;
	}
	
	private static boolean isLevelTwoNode(Node node) {
		if (node != null)
			return isSiteNode(node.getParentNode());
		return false;
	}
	
	
	public static boolean isSiteNode(Node node) {
		if (node != null) 
			return node.getNodeName().equals("site") && isTopLevelNode(node);
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
