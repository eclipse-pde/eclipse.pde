import org.eclipse.pde.internal.build.tasks.*;

public class Test2 {
	public static void main(String[] args) {
		ManifestModifier replacer = new ManifestModifier();
		replacer.setManifestLocation("d:/tmp/META-INF/manifest.mf");
		replacer.setKeyValue("Bundle-Version|3.0.0#Generated-from|null");
		replacer.execute();
	}
}
