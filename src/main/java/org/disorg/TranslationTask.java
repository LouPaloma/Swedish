/**
 * 
 */
package org.disorg;

import java.io.BufferedWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.disorg.service.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Thread worker for concurrent processing of translation requests. Each instance will process a single translation request.
 * 
 * @author billsa
 *
 */
public class TranslationTask implements Callable<String> {

	/**
	 * Translation URL
	 */
	final private static String URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=sv&dt=t&q={0}";
	
	/**
	 * Regex to get translation from results, allowing for escaped double quotes 
	 */
	final private static String REGEX = "(?<!\\\\)\"";

	/**
	 * Input string to translate
	 */
	private String input;
	
	/**
	 * Translation service
	 */
	private Translator translator;
	
	/**
	 * As You Go output
	 */
	private BufferedWriter output;
	
	/**
	 * Synchronization latch
	 */
	private CountDownLatch latch;
	
	final private static Logger LOGGER = LoggerFactory.getLogger(TranslationTask.class);

	TranslationTask(String input, Translator translator, BufferedWriter asYouGo, CountDownLatch latch) {
		this.input = input;
		this.output = asYouGo;
		this.translator = translator;
		this.latch = latch;
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public String call() throws Exception {
		//Using generic REST approach, not the available Google Cloud translation libraries
		
		try {
			//build RestTemplate
			RestTemplate template = translator.getRestTemplate();

			//add required agent header
			HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", "none");
			headers.set("Accept", MediaType.ALL.toString());
			HttpEntity<String> request = new HttpEntity<String>(headers);
			
			//submit request - extract body from response
			//https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=sv&dt=t&q={toTranslate}
			ResponseEntity<String> response = template.exchange(URL, HttpMethod.GET, request, String.class, input);
			//System.err.println("Response: " + response);
			String result = response.getBody();
			
			//process response
			String translation = null;
			
			//[[["{att översätta}","{toTranslate}",null,null,1]],null,"en"]
			//[[["{ToTranslate [med parentes]}","{toTranslate[with brackets]}",null,null,3]],null,"en"]
			//[[["{ToTranslate \"med citat\"}","{toTranslate\"with quotes\"}",null,null,3]],null,"en"]
			//[[["Ord här","words here",null,null,3]],null,"en"]
			//[[["\"Citerade\" ord här","\"quoted\" words here",null,null,3]],null,"en"]
			//System.out.println(Arrays.toString(s.split("(?<!\\\\)\"")));
			
			LOGGER.debug("Returned RAW response: " + result);
			translation = result.split(REGEX)[1];
			LOGGER.debug("Parsed RAW translation: " + translation);

			//convert escaped quotations
			translation = translation.replace("\\\"", "\"");
			LOGGER.info("translation: " + translation);
			
			//write out result
			output.write(translation + "\n");
			output.flush();
			
			//notify processing done
			latch.countDown();
			
			return translation;
		} catch (Exception e) {
			//signal and throw
			latch.countDown();
			throw e;
		}
	}
}
