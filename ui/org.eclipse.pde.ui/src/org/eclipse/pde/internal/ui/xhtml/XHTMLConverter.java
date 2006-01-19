package org.eclipse.pde.internal.ui.xhtml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Stack;

import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.demo.html.Token;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class XHTMLConverter {

	public static final int XHTML_STRICT = 0;
	public static final int XHTML_TRANSITIONAL = 1;
	public static final int XHTML_FRAMESET = 2;
	private static final String[] XHTML_DOCTYPES = new String[3];
	static {
		XHTML_DOCTYPES[XHTML_STRICT] = 
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"; //$NON-NLS-1$
		XHTML_DOCTYPES[XHTML_TRANSITIONAL] = 
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"; //$NON-NLS-1$
		XHTML_DOCTYPES[XHTML_FRAMESET] =
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">"; //$NON-NLS-1$
	}
	private static final String XHTML_DEFAULT_DOCTYPE = XHTML_DOCTYPES[XHTML_TRANSITIONAL];
	private static final String XMLNS = "xmlns"; //$NON-NLS-1$
	private static final String XMLNS_LOC = "http://www.w3.org/1999/xhtml"; //$NON-NLS-1$
	private static final String F_XHTML_FE = "xhtml"; //$NON-NLS-1$
	
	private int fDoctype;
	private Stack fTagStack;
	private StringWriter fWriter;
	
	public XHTMLConverter(int docType) {
		fDoctype = docType;
		fTagStack = new Stack();
	}
	
	public void setType(int docType) {
		fDoctype = docType;
	}
	
	public boolean convert(IFile htmlIFile, IProgressMonitor monitor) {
		if (!htmlIFile.exists())
			return false;
		File htmlFile = new File(htmlIFile.getLocation().toString());
		if (!htmlFile.exists())
			return false;
		
		fTagStack.clear();
		fWriter = new StringWriter();
		PrintWriter pwriter = new PrintWriter(fWriter);
		write(htmlFile, pwriter);
		monitor.worked(1);
		pwriter.flush();
		pwriter.close();
		
		fWriter.flush();
		try {
			modifyFile(htmlIFile, monitor);
			fWriter.close();
		} catch (CoreException e) {
			return false;
		} catch (IOException e) {
		}
		
		return true;
	}
	
	private void write(File file, PrintWriter pw) {
		try {
			HTMLParser parser = new HTMLParser(file);
			pw.println(getDoctypeString(fDoctype));
			XHTMLTag htmlTag = grabNextTag(parser, "<html"); //$NON-NLS-1$
			htmlTag.addAttribute(XMLNS, XMLNS_LOC);
			// fill in any remaning attributes the html tag had
			convertTagContents(parser, htmlTag);
			htmlTag.write(pw);
			fTagStack.push(htmlTag);
			
			Token token = parser.getNextToken();
			while (isValid(token)) {
				switch (token.kind) {
				case HTMLParser.TagName:
					XHTMLTag tag = new XHTMLTag(token.image, fDoctype);
					convertTagContents(parser, tag);
					if (tag.isClosingTag()) {
						// Closinsg tag encountered:
						// - pop a tag from the stack and close it
						if (fTagStack.isEmpty())
							break;
						XHTMLTag topStack = (XHTMLTag)fTagStack.pop();
						topStack.writeCloseVersion(pw);
						break;
					}
					if (!tag.isEmptyTag())
						// Non-empty tags get pushed on the stack for closing
						fTagStack.push(tag);
					
					tag.write(pw);
					break;
				default:
					pw.print(token.image);
				}
				token = parser.getNextToken();
			}
			
			// close all remaining tags
			while (!fTagStack.isEmpty()) {
				XHTMLTag topStack = (XHTMLTag)fTagStack.pop();
				topStack.writeCloseVersion(pw);
			}
		} catch (FileNotFoundException e) {
		}
	}
	
	public String prepareXHTMLFileName(String filename) {
		int period = filename.lastIndexOf("."); //$NON-NLS-1$
		if (period > -1)
			return filename.substring(0, period + 1) + F_XHTML_FE;
		return filename + F_XHTML_FE;
	}
	
	private void modifyFile(IFile htmlFile, IProgressMonitor monitor) throws CoreException {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(fWriter.toString().getBytes());
			htmlFile.setContents(bais, IFile.KEEP_HISTORY | IFile.FORCE, monitor);
			monitor.worked(1);
			bais.close();
			IPath newPath = htmlFile.getFullPath().removeFileExtension().addFileExtension(F_XHTML_FE);
			htmlFile.move(newPath, IFile.KEEP_HISTORY | IFile.FORCE, monitor);
			monitor.worked(1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private XHTMLTag grabNextTag(HTMLParser parser, String tag) {
		Token token = parser.getNextToken();
		while (isValid(token)) {
			if (token.kind == HTMLParser.TagName
					&& token.image.equalsIgnoreCase(tag))
				return new XHTMLTag(token.image, fDoctype);
			token = parser.getNextToken();
		}
		return null;
	}
	
	private void convertTagContents(HTMLParser parser, XHTMLTag tag) {
		if (tag == null)
			return;
		Token token = parser.getNextToken();
		while (isValid(token) && token.kind != HTMLParser.TagEnd) {
			tag.eatToken(token);
			token = parser.getNextToken();
		}
		tag.expandLeftoverAttribute();
		// last token read is either invalid or a TagEnd - we don't care about either
	}
	
	private boolean isValid(Token token) {
		return token != null && token.kind != HTMLParser.EOF;
	}
	
	private String getDoctypeString(int version) {
		if (version != XHTML_FRAMESET &&
				version != XHTML_STRICT &&
				version != XHTML_FRAMESET)
			return XHTML_DEFAULT_DOCTYPE;
		return XHTML_DOCTYPES[version];
	}
}
