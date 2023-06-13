package $packageName$;

import java.util.Set;

import org.osgi.service.component.annotations.*;

@Component
public class DictionaryImpl implements Dictionary {

	private static final Set<String> WORDS = Set.of("$word1$", "$word2$", "$word3$");
	private static final String LANGUAGE = "en_US";
	
	@Override
	public String getLanguage() {
		return LANGUAGE;
	}

	@Override
	public boolean check(String word) {
		return WORDS.contains(word);
	}
	
	@Override
	public String toString() {
		return LANGUAGE;
	}

}
