package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.w3c.dom.*;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteDescription extends SiteObject implements ISiteDescription {
	private String url;
	private String text;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getURL()
	 */
	public String getURL() {
		return url;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#getText()
	 */
	public String getText() {
		return text;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setURL(java.net.URL)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteDescription#setText(java.lang.String)
	 */
	public void setText(String text) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.text;
		this.text = text;
		firePropertyChanged(P_TEXT, oldValue, text);
	}

	protected void reset() {
		url = null;
		text = null;
	}

	protected void parse(Node node) {
		url = getNodeAttribute(node, "url");
		NodeList children = node.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType()==Node.TEXT_NODE) {
				Node firstChild = node.getFirstChild();
				if (firstChild!=null)
					text = getNormalizedText(firstChild.getNodeValue());
				break;
			}
		}
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_TEXT)) {
			setText(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}
	
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<description");
		if (url!=null)
			writer.print(" url=\""+url+"\"");
		writer.println(">");
		if (text!=null) {
			writer.println(indent+Site.INDENT+ getNormalizedText(text));
		}
		writer.println(indent+"</description>");
	}

}
