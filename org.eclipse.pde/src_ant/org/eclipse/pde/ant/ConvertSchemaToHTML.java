package org.eclipse.pde.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.pde.internal.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.SourceDOMParser;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.schema.Schema;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ConvertSchemaToHTML extends Task {
	
	private static SourceDOMParser parser = new SourceDOMParser();
	private static SchemaTransformer transformer = new SchemaTransformer();
	private String schemalocation;
	private String outputlocation;
	
	
	public void execute() throws BuildException {
		FileInputStream is = null;
		PrintWriter printWriter = null;
		try {
			if (schemalocation == null || outputlocation == null)
				return;
				
			File schemaFile = new File(schemalocation);
			if (!schemaFile.exists())
				return;

			is = new FileInputStream(schemaFile);
			parser.parse(new InputSource(is));
			Schema schema = new Schema((ISchemaDescriptor) null, null);
			schema.traverseDocumentTree(
				parser.getDocument().getDocumentElement(),
				parser.getLineTable());

			printWriter = new PrintWriter(new FileOutputStream(outputlocation), true);
			transformer.transform(printWriter, schema);
		} catch (FileNotFoundException e) {
		} catch (SAXException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
			if (printWriter != null) {
				printWriter.flush();
				printWriter.close();
			}
		}
	}
	
	public void setSchemalocation(String location) {
		this.schemalocation = location;
	}
	
	public void setOutputlocation(String location) {
		this.outputlocation = location;
	}

}
