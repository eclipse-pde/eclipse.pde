package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.net.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteArchive extends SiteObject implements ISiteArchive {
	private URL url;
	private String path;

	public URL getURL() {
		return url;
	}
	public void setURL(URL url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.path;
		this.path = path;
		firePropertyChanged(P_PATH, oldValue, path);
	}
	public void reset() {
		super.reset();
		url = null;
		path = null;
	}
	protected void parse(Node node) {
		super.parse(node);
		path = getNodeAttribute(node, "path");
		url = parseURL(getNodeAttribute(node, "url"));
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<archive");
		if (path != null)
			writer.print(" path=\"" + path + "\"");
		if (url != null)
			writer.print(" url=\"" + url.toString() + "\"");
		writer.println("/>");
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_PATH)) {
			setPath(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL) && newValue instanceof URL) {
			setURL((URL) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

}