package org.disorg.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("default")
/**
 * Default Translation service implementation.  Provides a real RestTemplate for server queries
 * @author billsa
 *
 */
public class TranslationService implements Translator {

	@Autowired
	private RestTemplateBuilder templateBuilder;
	
	/* (non-Javadoc)
	 * @see org.disorg.service.Translator#getRestTemplate()
	 */
	@Override
	public RestTemplate getRestTemplate() {
		return templateBuilder.build();
	}

}
