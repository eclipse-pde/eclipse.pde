package org.eclipse.pde.ant;

import java.io.*;
import java.net.*;

import org.apache.tools.ant.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.SourceDOMParser;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.schema.Schema;
import org.xml.sax.*;

public class ConvertSchemaToHTML extends Task {

	private SourceDOMParser parser = new SourceDOMParser();
	private SchemaTransformer transformer = new SchemaTransformer();
	private String manifest;
	private String destination;

	public void execute() throws BuildException {
		validateDestination();
		IPluginModelBase model = readManifestFile();

		StringBuffer buildMessage = new StringBuffer();
		IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			String schemaLocation = extPoints[i].getSchema();
			FileInputStream is = null;
			PrintWriter printWriter = null;

			if (schemaLocation == null || schemaLocation.equals(""))
				continue;

			try {
				File schemaFile =
					new File(
						model.getInstallLocation() + Path.SEPARATOR + schemaLocation);
				is = new FileInputStream(schemaFile);
				parser.parse(new InputSource(is));

				URL url = null;
				try {
					url = new URL("file:" + schemaFile.getAbsolutePath());
				} catch (MalformedURLException e) {
				}
				Schema schema = new Schema((ISchemaDescriptor) null, url);
				schema.traverseDocumentTree(
					parser.getDocument().getDocumentElement(),
					parser.getLineTable());

				File file = new File(destination);
				if (!file.exists() || !file.isDirectory())
					file.mkdirs();
				printWriter =
					new PrintWriter(
						new FileWriter(
							destination
								+ Path.SEPARATOR
								+ extPoints[i].getFullId().replace('.', '_')
								+ ".html"),
						true);
				transformer.transform(printWriter, schema);
			} catch (Exception e) {
				if (e.getMessage() != null)
					buildMessage.append(
						e.getMessage() + System.getProperty("line.separator"));
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
		if (buildMessage.length() > 0)
			throw new BuildException(buildMessage.toString());
	}

	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	private IPluginModelBase readManifestFile() throws BuildException {
		if (manifest == null)
			throw new BuildException("'manifest' attribute is not specified");

		InputStream stream = null;
		File file = new File(manifest);
		try {
			stream = new FileInputStream(manifest);
		} catch (FileNotFoundException e) {
			throw new BuildException(
				"File:" + file.getAbsolutePath() + " does not exist.");
		}

		ExternalPluginModelBase model = null;
		if (file.getName().toLowerCase().equals("fragment.xml"))
			model = new ExternalFragmentModel();
		else if (file.getName().toLowerCase().equals("plugin.xml"))
			model = new ExternalPluginModel();
		else
			throw new BuildException("Illegal value for 'manifest' attribute");

		String parentPath = file.getParentFile().getAbsolutePath();
		model.setInstallLocation(parentPath);
		try {
			model.load(stream, false);
			stream.close();
		} catch (Exception e) {}
		return model;
	}

	private void validateDestination() throws BuildException {
		if (destination == null)
			throw new BuildException("'destination' attribute is not specified");
		if (!new Path(destination).isValidPath(destination))
			throw new BuildException("Illegal value for 'destination' attribute");
	}
}
