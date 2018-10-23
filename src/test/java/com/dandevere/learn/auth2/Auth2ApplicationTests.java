package com.dandevere.learn.auth2;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.xml.sax.InputSource;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.RANDOM_PORT)
public class Auth2ApplicationTests {
	
	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	public void testGoogle() throws Exception {
		ResponseEntity<String> response = testRestTemplate.getForEntity("/login", String.class);
		String csrf = getByCssQuery("input[name='_csrf']", response.getBody()).val();
		MyCookieStore store = new MyCookieStore();
		URI uri = store.addCookies(response.getHeaders());
		store.printCookies();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cookie", store.getCookieString(uri));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> user = new LinkedMultiValueMap<>();
		user.add("username", "dan");
		user.add("password", "password");
		user.add("_csrf", csrf);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String,String>>(user, headers);
		response = testRestTemplate.exchange("/login", HttpMethod.POST, entity, String.class);
		System.out.println(response.getStatusCodeValue());
		System.out.println(response.getHeaders().getLocation());
		uri = store.addCookies(response.getHeaders());
		store.printCookies();
		headers = new HttpHeaders();
		headers.add("Cookie", store.getCookieString(uri));
		entity = new HttpEntity<>(headers);
		response = testRestTemplate.exchange("/admin/info", HttpMethod.GET, entity, String.class);
		System.out.println(response.getBody());
	}
	
	private Elements getByCssQuery(String query, String html) {
		Document doc = Jsoup.parse(html);
		return doc.select(query);
	}
	
	public class MyCookieStore implements CookieStore {
		
		ConcurrentMap<URI, List<HttpCookie>> cookies = new ConcurrentHashMap<>();
		
		public URI addCookies(HttpHeaders headers) throws URISyntaxException {
			URI uri = null;
			for(String header : headers.get("Set-Cookie")) {
				HttpCookie cookie = HttpCookie.parse(header).get(0);
				uri = new URI(cookie.getDomain() + cookie.getPath());
				add(uri, cookie);
			}
			return uri;
		}
		
		public void printCookies() {
			for(HttpCookie cookie : getCookies()) {
				System.out.println(cookie.toString());
			}
		}
		
		public String getCookieString(URI uri) {
			StringBuilder sb = new StringBuilder();
			for(HttpCookie cookie : get(uri)) {
				sb.append(cookie.toString()).append("; ");
			}
			return sb.toString();
		}
		
		public HttpCookie getCookie(URI uri, String name) {
			List<HttpCookie> cooks = get(uri);
			if(cooks == null) {
				return null;
			}
			for(HttpCookie cookie : cooks) {
				if(cookie.getName().equals(name)) {
					return cookie;
				}
 			}
			return null;
		}

		@Override
		public void add(URI uri, HttpCookie cookie) {
			if(cookies.get(uri) == null) {
				cookies.put(uri, new ArrayList<HttpCookie>());
			}
			remove(uri, cookie);
			cookies.get(uri).add(cookie);
		}

		@Override
		public List<HttpCookie> get(URI uri) {
			// TODO Auto-generated method stub
			return cookies.get(uri);
		}

		@Override
		public List<HttpCookie> getCookies() {
			List<HttpCookie> all = new ArrayList<>();
			for(Map.Entry<URI, List<HttpCookie>> entry : cookies.entrySet()) {
				all.addAll(entry.getValue());
			}
			return all;
		}

		@Override
		public List<URI> getURIs() {
			List<URI> uris = new ArrayList<>();
			for(Map.Entry<URI, List<HttpCookie>> entry : cookies.entrySet()) {
				uris.add(entry.getKey());
			}
			return uris;
		}

		@Override
		public boolean remove(URI uri, HttpCookie cookie) {
			List<HttpCookie> uriCookies = cookies.get(uri);
			if(uriCookies == null) {
				return false;
			}
			for(int i = 0; i < uriCookies.size(); i++) {
				if(uriCookies.get(i).getName().equals(cookie.getName())) {
					uriCookies.remove(i);
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean removeAll() {
			cookies = new ConcurrentHashMap<>();
			return true;
		}
		
	}

}
