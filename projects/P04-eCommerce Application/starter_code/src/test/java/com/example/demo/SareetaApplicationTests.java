package com.example.demo;

import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
public class SareetaApplicationTests {

	@Autowired
	private UserController userController;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JacksonTester<User> userJacksonTester;

	@Autowired
	private JacksonTester<ModifyCartRequest> modifyCartRequestJacksonTester;

	@Test
	public void testCreateUserAndLogin() throws Exception {
		createUser("usr", "pwd1234");
		authorizeUser("usr", "pwd1234");
	}

	@Test
	public void testGetItemsRequiresAuthorization() throws Exception {
		mvc.perform(get(new URI("/api/item"))
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().is4xxClientError());

		createUser("usr", "pwd1234");
		String token = authorizeUser("usr", "pwd1234");

		mvc.perform(get(new URI("/api/item"))
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk());
	}

	@Test
	public void testItemAddToCart() throws Exception {
		createUser("usr", "pwd1234");
		String token = authorizeUser("usr", "pwd1234");

		ModifyCartRequest request = new ModifyCartRequest();
		request.setUsername("usr");
		request.setItemId(1);
		request.setQuantity(1);

		MvcResult result = mvc.perform(post(new URI("/api/cart/addToCart"))
				.content(modifyCartRequestJacksonTester.write(request).getJson())
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk()).andReturn();

		byte[] content = result.getResponse().getContentAsByteArray();

		Cart cart = new ObjectMapper().readValue(content, Cart.class);
		assertEquals(1, cart.getItems().size());
	}

	@Test
	public void testBadRequestWhenShortPassword() {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername("usr");
		request.setPassword("pwd");
		request.setConfirmPassword("pwd");

		ResponseEntity<User> response = userController.createUser(request);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	private void createUser(String username, String password) throws Exception {
		CreateUserRequest request = new CreateUserRequest();
		request.setUsername(username);
		request.setPassword(password);
		request.setConfirmPassword(password);

		ResponseEntity<User> response = userController.createUser(request);
		assertEquals(username, response.getBody().getUsername());
	}

	private String authorizeUser(String username, String password) throws Exception {
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);

		MvcResult loginResult = mvc.perform(post(new URI("/login"))
				.content(userJacksonTester.write(user).getJson())
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk())
				.andExpect(header().exists("Authorization"))
				.andReturn();

		return loginResult.getResponse().getHeader("Authorization");
	}
}
