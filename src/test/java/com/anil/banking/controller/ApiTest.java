package com.anil.banking.controller;

import com.anil.banking.entity.AppUser;
import com.anil.banking.entity.Role;
import com.anil.banking.repository.AppUserRepository;
import com.anil.banking.support.DatabaseCleaner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end checks over HTTP: login, JWT, ownership rules, admin rules, validation. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private DatabaseCleaner cleaner;
    @Autowired private ObjectMapper json;
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cleaner.clean();
    }

    @Test
    void registerLoginAndUseOwnAccount() throws Exception {
        String token = register("anil", "anil@example.com");
        String account = firstAccount(token);

        mvc.perform(post("/api/accounts/" + account + "/deposit").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":5000}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/accounts/" + account).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5000.0));

        String loginToken = login("anil", "Secret123");
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(loginToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test anil"))
                .andExpect(jsonPath("$.accounts.length()").value(1));
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register("anil", "anil@example.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"anil\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void customerCannotTouchSomeoneElsesAccount() throws Exception {
        String anil = register("anil", "anil@example.com");
        String ravi = register("ravi", "ravi@example.com");
        String raviAccount = firstAccount(ravi);

        mvc.perform(get("/api/accounts/" + raviAccount).header("Authorization", bearer(anil)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/accounts/" + raviAccount + "/withdraw").header("Authorization", bearer(anil))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/transfers").header("Authorization", bearer(anil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountNumber\":\"" + raviAccount + "\",\"toAccountNumber\":\""
                                + firstAccount(anil) + "\",\"amount\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanSendToAnotherCustomer() throws Exception {
        String anil = register("anil", "anil@example.com");
        String ravi = register("ravi", "ravi@example.com");
        String anilAccount = firstAccount(anil);
        String raviAccount = firstAccount(ravi);

        mvc.perform(post("/api/accounts/" + anilAccount + "/deposit").header("Authorization", bearer(anil))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1000}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/accounts/" + raviAccount + "/holder").header("Authorization", bearer(anil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderName").value("Test r."));

        mvc.perform(post("/api/transfers").header("Authorization", bearer(anil))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountNumber\":\"" + anilAccount + "\",\"toAccountNumber\":\""
                                + raviAccount + "\",\"amount\":250}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/accounts/" + raviAccount).header("Authorization", bearer(ravi)))
                .andExpect(jsonPath("$.balance").value(250.0));
    }

    @Test
    void adminEndpointsNeedAdminRole() throws Exception {
        String customer = register("anil", "anil@example.com");
        mvc.perform(get("/api/admin/stats").header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        userRepository.save(new AppUser("boss", passwordEncoder.encode("Admin1234"), Role.ADMIN, null));
        String admin = login("boss", "Admin1234");

        mvc.perform(get("/api/admin/stats").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers").value(1));
        mvc.perform(get("/api/admin/customers").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("anil"));
        mvc.perform(patch("/api/accounts/" + firstAccount(customer) + "/status").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        register("anil", "anil@example.com");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("anil", "other@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidSignUpReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","phone":"12","username":"a","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    private String register(String username, String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private String firstAccount(String token) throws Exception {
        String body = mvc.perform(get("/api/me/accounts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode accounts = json.readTree(body);
        return accounts.get(0).get("accountNumber").asText();
    }

    private static String registerBody(String username, String email) {
        return "{\"fullName\":\"Test " + username + "\",\"email\":\"" + email + "\",\"phone\":\"9652336856\","
                + "\"username\":\"" + username + "\",\"password\":\"Secret123\"}";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
