package org.eclipse.pde.internal.core.product;

import java.io.*;
import java.util.*;

import org.eclipse.pde.internal.core.iproduct.*;
import org.w3c.dom.*;

public class LauncherInfo extends ProductObject implements ILauncherInfo {

	private static final long serialVersionUID = 1L;
	private boolean fUseIcoFile;
	private String fIcoFilePath;
	private Map fIcons = new HashMap();
	private String fLauncherName;

	public LauncherInfo(IProductModel model) {
		super(model);
	}

	public String getLauncherName() {
		return fLauncherName;
	}

	public void setLauncherName(String name) {
		String old = fLauncherName;
		fLauncherName = name;
		if (isEditable())
			firePropertyChanged(P_LAUNCHER, old, fLauncherName);
	}

	public void setIconPath(String iconId, String path) {
		if (path == null)
			path = "";
		String old = (String)fIcons.get(iconId);
		fIcons.put(iconId, path);
		if (isEditable())
			firePropertyChanged(iconId, old, path);
	}

	public String getIconPath(String iconId) {
		return (String)fIcons.get(iconId);
	}

	public boolean usesWinIcoFile() {
		return fUseIcoFile;
	}

	public void setUseWinIcoFile(boolean use) {
		boolean old = fUseIcoFile;
		fUseIcoFile = use;
		if (isEditable())
			firePropertyChanged(P_USE_ICO, Boolean.toString(old), Boolean.toString(fUseIcoFile));
	}

	public void setIcoFilePath(String path) {
		String old = fIcoFilePath;
		fIcoFilePath = path;
		if (isEditable())
			firePropertyChanged(P_ICO_PATH, old, fIcoFilePath);
	}

	public String getIcoFilePath() {
		return fIcoFilePath;
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			fLauncherName = ((Element)node).getAttribute("name");
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					String name = child.getNodeName();
					if (name.equals("linux")) {
						parseLinux((Element)child);
					} else if (name.equals("macosx")) {
						parseMac((Element)child);
					} else if (name.equals("solaris")) {
						parseSolaris((Element)child);
					} else if (name.equals("win")) {
						parseWin((Element)child);
					}
				}
			}
		}
	}
	
	private void parseWin(Element element) {
		fUseIcoFile = "true".equals(element.getAttribute(P_USE_ICO));
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element)children.item(i);
				String name = child.getNodeName();
				if (name.equals("ico")) {
					fIcoFilePath = child.getAttribute("path");
				} else if (name.equals("bmp")) {
					fIcons.put(WIN32_16_HIGH, element.getAttribute(WIN32_16_HIGH));
					fIcons.put(WIN32_16_LOW, element.getAttribute(WIN32_16_LOW));
					fIcons.put(WIN32_32_HIGH, element.getAttribute(WIN32_32_HIGH));
					fIcons.put(WIN32_32_LOW, element.getAttribute(WIN32_32_LOW));
					fIcons.put(WIN32_48_HIGH, element.getAttribute(WIN32_48_HIGH));
					fIcons.put(WIN32_48_LOW, element.getAttribute(WIN32_48_LOW));
				}
			}
		}
	}

	private void parseSolaris(Element element) {
		fIcons.put(SOLARIS_LARGE, element.getAttribute(SOLARIS_LARGE));
		fIcons.put(SOLARIS_MEDIUM, element.getAttribute(SOLARIS_MEDIUM));
		fIcons.put(SOLARIS_SMALL, element.getAttribute(SOLARIS_SMALL));
		fIcons.put(SOLARIS_TINY, element.getAttribute(SOLARIS_TINY));
	}

	private void parseMac(Element element) {
		fIcons.put(MACOSX_ICON, element.getAttribute(MACOSX_ICON));
	}

	private void parseLinux(Element element) {
		fIcons.put(LINUX_ICON, element.getAttribute(LINUX_ICON));
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<launcher ");
		if (fLauncherName != null && fLauncherName.length() > 0)
			writer.print("name=\"" + fLauncherName + "\"");
		writer.println(">");
		
		writeLinux(indent + "   ", writer);
		writeMac(indent + "   ", writer);
		writeSolaris(indent + "   ", writer);
		writerWin(indent + "   ", writer);
		writer.println(indent + "</launcher>");
	}

	private void writerWin(String indent, PrintWriter writer) {
		writer.println(indent + "<win " + P_USE_ICO + "=\"" + Boolean.toString(fUseIcoFile) + "\">");
		if (fIcoFilePath != null && fIcoFilePath.length() > 0)
			writer.println(indent + "   <ico path=\"" + fIcoFilePath + "\"/>");
		writer.print(indent + "   <bmp");
		writeIcon(indent + "   ", WIN32_16_HIGH, writer);
		writeIcon(indent + "   ", WIN32_16_LOW, writer);
		writeIcon(indent + "   ", WIN32_32_HIGH, writer);
		writeIcon(indent + "   ", WIN32_32_LOW, writer);
		writeIcon(indent + "   ", WIN32_48_HIGH, writer);
		writeIcon(indent + "   ", WIN32_48_LOW, writer);
		writer.println(indent + "   />");
		writer.println(indent + "</win>");
	}

	private void writeSolaris(String indent, PrintWriter writer) {
		writer.print(indent + "<solaris");
		writeIcon(indent + "   ", SOLARIS_LARGE, writer);
		writeIcon(indent + "   ", SOLARIS_MEDIUM, writer);
		writeIcon(indent + "   ", SOLARIS_SMALL, writer);
		writeIcon(indent + "   ", SOLARIS_TINY, writer);
		writer.println(indent + "/>");
	}
	
	private void writeIcon(String indent, String iconId, PrintWriter writer) {
		String icon = fIcons.get(iconId).toString();
		if (icon != null && icon.length() > 0) {
			writer.println();
			writer.print(indent + "   " + iconId + "=\"" + icon + "\"");
		}
		
	}

	private void writeMac(String indent, PrintWriter writer) {
		String icon = (String)fIcons.get(MACOSX_ICON);
		if (icon != null && icon.length() > 0)
			writer.println(indent + "<macosx " + MACOSX_ICON + "=\"" + icon + "\"/>");
	}

	private void writeLinux(String indent, PrintWriter writer) {
		String icon = (String)fIcons.get(LINUX_ICON);
		if (icon != null && icon.length() > 0)
			writer.println(indent + "<linux " + LINUX_ICON + "=\"" + icon + "\"/>");
	}

}
