package org.disorg.service;

import org.springframework.web.client.RestTemplate;

/**
 * General interface for Translation service, providing a framework for an injectable mock RestTemplate.
 * @author billsa
 *
 */
public interface Translator {

	RestTemplate getRestTemplate();

}