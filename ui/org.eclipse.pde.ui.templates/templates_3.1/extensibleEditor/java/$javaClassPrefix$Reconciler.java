package $packageName$;

import $packageName$.$javaClassPrefix$ReconcilerStrategy;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

public class $javaClassPrefix$Reconciler extends Reconciler {

    private $javaClassPrefix$ReconcilerStrategy fStrategy;

    public $javaClassPrefix$Reconciler() {
        // TODO this is logic for .project file to fold tags. Replace with your language logic!
        fStrategy = new $javaClassPrefix$ReconcilerStrategy();
        this.setReconcilingStrategy(fStrategy, IDocument.DEFAULT_CONTENT_TYPE);
    }

    @Override
    public void install(ITextViewer textViewer) {
        super.install(textViewer);
        ProjectionViewer pViewer =(ProjectionViewer)textViewer;
        fStrategy.setProjectionViewer(pViewer);
    }
}