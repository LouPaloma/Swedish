package org.disorg.service;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("test")
/**
 * Translation component to provide canned responses
 * 
 * @author billsa
 *
 */
public class MockTranslationService implements Translator, InitializingBean {

	/**
	 * Canned google response template
	 */
	private static final String RESPONSE_TEMPLATE = "[[[\"%s\",\"%s\",null,null,%d]],null,\"en\"]";
	
	/**
	 * Indicates whether to respond with a value or an HTTP error code
	 */
	@Value("${throwError:false}")
	private boolean throwError;
	
	private HashMap<String, String> lookupMap = new HashMap<>();

	private static final Logger LOGGER = LoggerFactory.getLogger(MockTranslationService.class);
	
	@Override
	public void afterPropertiesSet() throws Exception {
		System.err.println("Using Mock translation service.");

		//load English:Swedish map
		lookupMap.put("one", "en");
		lookupMap.put("two", "två");
		lookupMap.put("three", "tre");
		lookupMap.put("four", "frya");
		lookupMap.put("five", "fem");
		lookupMap.put("six", "sex");
		lookupMap.put("seven", "seju");
		lookupMap.put("eight", "åtta");
		lookupMap.put("nine", "nio");
	}
	
	@Override
	public RestTemplate getRestTemplate() {
		RestTemplate template = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
		
		//extract parameter and use lookup map
		if (throwError) {
			server.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.anything()).andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
				.andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED));
		} else {
			server.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.anything()).andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
				.andRespond(request -> {
					return MockRestResponseCreators.withSuccess(getBody(request), MediaType.APPLICATION_JSON_UTF8).createResponse(request);
				});
		}
		
		return template;
	}

	/**
	 * Lookup the translation based on the input parameter on the URI
	 * 
	 * @param request URI
	 * @return strings translation from known map, or null if not in the map
	 */
	private String getBody(ClientHttpRequest request) {
		LOGGER.debug("Request: " + request);
		
		MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
	    List<String> translateList = parameters.get("q");
	    String toTranslate = translateList.get(0);
		LOGGER.debug("Translation target: " + toTranslate);
		
		//use lookup map
		return String.format(RESPONSE_TEMPLATE, lookupMap.get(toTranslate), toTranslate, toTranslate.trim().split("\\s+").length);
	}

}
