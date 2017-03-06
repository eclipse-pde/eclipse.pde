package $packageName$;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.filebuffers.IDocumentSetupParticipantExtension;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class ValidatorDocumentSetupParticipant implements IDocumentSetupParticipant, IDocumentSetupParticipantExtension {

	private final class DocumentValidator implements IDocumentListener {
		private final IFile file;
		private IMarker marker;

		private DocumentValidator(IFile file) {
			this.file = file;
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			if (this.marker != null) {
				try {
					this.marker.delete();
				} catch (CoreException e) {
					e.printStackTrace();
				}
				this.marker = null;
			}
			try (StringReader reader = new StringReader(event.getDocument().get());) {
				DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				documentBuilder.parse(new InputSource(reader));
			} catch (Exception ex) {
				try {
					this.marker = file.createMarker(IMarker.PROBLEM);
					this.marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					this.marker.setAttribute(IMarker.MESSAGE, ex.getMessage());
					if (ex instanceof SAXParseException) {
						SAXParseException saxParseException = (SAXParseException)ex;
						int lineNumber = saxParseException.getLineNumber();
						int offset = event.getDocument().getLineInformation(lineNumber - 1).getOffset() + saxParseException.getColumnNumber() - 1;
						this.marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
						this.marker.setAttribute(IMarker.CHAR_START, offset);
						this.marker.setAttribute(IMarker.CHAR_END, offset + 1);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}
	}

	@Override
	public void setup(IDocument document) {
	}

	@Override
	public void setup(IDocument document, IPath location, LocationKind locationKind) {
		if (locationKind == LocationKind.IFILE) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(location);
			document.addDocumentListener(new DocumentValidator(file));
		}
	}

}
