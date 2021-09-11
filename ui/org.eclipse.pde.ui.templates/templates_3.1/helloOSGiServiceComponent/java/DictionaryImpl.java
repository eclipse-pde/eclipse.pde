package $packageName$;

import java.util.Set;

import org.osgi.service.component.annotations.*;

@Component
public class DictionaryImpl implements Dictionary {

	private final Set<String> fWords = Set.of("$word1$", "$word2$", "$word3$");
	private final String fLanguage = "en_US";
	
	@Override
	public String getLanguage() {
		return fLanguage;
	}

	@Override
	public boolean check(String word) {
		return fWords.contains(word);
	}
	
	@Override
	public String toString() {
		return fLanguage;
	}

}
