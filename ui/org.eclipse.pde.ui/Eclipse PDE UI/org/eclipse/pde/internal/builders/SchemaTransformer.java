package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.apache.xerces.parsers.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.io.*;
import org.eclipse.pde.internal.PDEPlugin;

public class SchemaTransformer implements ISchemaTransformer {

private void appendAttlist(
	StringBuffer out,
	ISchemaAttribute att,
	int maxWidth) {
	// add three spaces
	out.append("<br><samp>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	// add name
	out.append(att.getName());
	// fill spaces to allign data type
	int delta = maxWidth - att.getName().length();
	for (int i = 0; i < delta + 1; i++) {
		out.append("&nbsp;");
	}
	// add data type
	ISchemaSimpleType type = att.getType();
	ISchemaRestriction restriction = type.getRestriction();
	String typeName = type.getName().toLowerCase();
	if (typeName.equals("boolean")) {
		out.append("(true | false) \"false\"");
	} else
		if (restriction != null) {
			appendRestriction(restriction, out);
		} else {
			out.append("CDATA ");
		}

	// add use
	if (att.getUse() == ISchemaAttribute.REQUIRED)
		out.append("#REQUIRED");
	else
		if (att.getUse() == ISchemaAttribute.DEFAULT) {
			out.append("\""+att.getValue()+"\"");
		} else
			out.append("#IMPLIED");
	out.append("</samp>\n");
}
private void appendRestriction(ISchemaRestriction restriction, StringBuffer out) {
	if (restriction instanceof ChoiceRestriction) {
		ChoiceRestriction cr = (ChoiceRestriction)restriction;
		String [] choices = cr.getChoicesAsStrings();
		out.append("(");
		for (int i=0; i<choices.length; i++) {
			if (i>0) out.append("|");
			out.append(choices[i]);
		}
		out.append(") ");
	}
}
private int calculateMaxAttributeWidth(ISchemaAttribute [] attributes) {
	int width = 0;
	for (int i=0; i<attributes.length; i++) {
		ISchemaAttribute att = attributes[i];
		width = Math.max(width, att.getName().length());
	}
	return width;
}
private Node createDOMTree(InputStream schema, PluginErrorReporter reporter) {
	DOMParser parser = new DOMParser();
	parser.setErrorHandler(reporter);
	try {
		InputSource source = new InputSource(schema);
		parser.parse(source);
		return parser.getDocument().getDocumentElement();
	}
	catch (SAXException e) {
	}
	catch (IOException e) {
		PDEPlugin.logException(e);
	}
	return null;
}
private boolean isPreEnd(String text, int loc) {
	if (loc + 5 >= text.length()) return false;
	String tag = text.substring(loc, loc+6);
	if (tag.toLowerCase().equals("</pre>")) return true;
	return false;
}
private boolean isPreStart(String text, int loc) {
	if (loc + 4 >= text.length()) return false;
	String tag = text.substring(loc, loc+5);
	if (tag.toLowerCase().equals("<pre>")) return true;
	return false;
}
public void transform(InputStream is, StringBuffer out, PluginErrorReporter reporter) {
	Node root = createDOMTree(is, reporter);
	if (root==null) return;
	Schema schema = new Schema((ISchemaDescriptor)null, null);
	schema.traverseDocumentTree(root);
	transform(out, schema);
}
public void transform(StringBuffer out, ISchema schema) {
	out.append("<HTML>\n");
	out.append("<BODY>\n");
	out.append("<H1><CENTER>" + schema.getName() + "</CENTER></H1>\n");
	out.append("<H2>Identifier</H2>\n");
	out.append(schema.getPointId());
	out.append("<H2>Description</H2>\n");
	transformText(out, schema.getDescription());
	out.append("<H2>Markup</H2>\n");
	transformMarkup(out, schema);
	out.append("<H2>Example</H2>\n");
	transformSection(out, schema, IDocumentSection.EXAMPLES);
	out.append("<H2>API Information</H2>\n");
	transformSection(out, schema, IDocumentSection.API_INFO);
	out.append("<H2>Supplied Implementation</H2>\n");
	transformSection(out, schema, IDocumentSection.IMPLEMENTATION);
	transformSection(out, schema, IDocumentSection.COPYRIGHT);
	out.append("</BODY>\n");
	out.append("</HTML>\n");
}
private void transformElement(StringBuffer out, ISchemaElement element) {
	String name = element.getName();
	String dtd = element.getDTDRepresentation();
	out.append("<p><samp>&nbsp;&nbsp; &lt;!ELEMENT " + name + " " + dtd);
	out.append("&gt;</samp>\n");

	ISchemaAttribute[] attributes = element.getAttributes();
	if (attributes.length == 0)
		return;

	out.append("<br><samp>&nbsp;&nbsp; &lt;!ATTLIST " + name + "</samp>\n");
	int maxWidth = calculateMaxAttributeWidth(element.getAttributes());
	for (int i = 0; i < attributes.length; i++) {
		appendAttlist(out, attributes[i], maxWidth);
	}
	out.append("<br><samp>&nbsp;&nbsp; &gt;</samp>\n");

	out.append("<ul>\n");
	for (int i = 0; i < attributes.length; i++) {
		ISchemaAttribute att = attributes[i];
		out.append(
			"<li><b>" + att.getName() + "</b> - " + att.getDescription() + "</li>");
	}
	out.append("</ul>\n");
}
private void transformMarkup(StringBuffer out, ISchema schema) {
	ISchemaElement[] elements = schema.getElements();

	for (int i = 0; i < elements.length; i++) {
		ISchemaElement element = elements[i];
		transformElement(out, element);
	}
}
private void transformSection(
	StringBuffer out,
	ISchema schema,
	String sectionId) {
	IDocumentSection [] sections = schema.getDocumentSections();
	IDocumentSection section = null;
	for (int i=0; i<sections.length; i++) {
		if (sections[i].getSectionId().equals(sectionId)) {
			section = sections[i];
			break;
		}
	}
	if (section==null) return;
	transformText(out, section.getDescription());
}
private void transformText(StringBuffer out, String text) {
	boolean preformatted = false;
	for (int i = 0; i < text.length(); i++) {
		char c = text.charAt(i);
		if (c == '<') {
			if (isPreStart(text, i)) {
				out.append("<pre>");
				i += 4;
				preformatted = true;
				continue;
			}
			if (isPreEnd(text, i)) {
				out.append("</pre>");
				i += 5;
				preformatted = false;
				continue;
			}
		}
		if (preformatted) {
			switch (c) {
				case '<' :
					out.append("&lt;");
					break;
				case '>' :
					out.append("&gt;");
					break;
				case '&' :
					out.append("&amp;");
					break;
				default :
					out.append(c);
			}
		} else
			out.append(c);
	}
}
}
