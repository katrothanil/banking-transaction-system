package com.anil.banking.controller;

import com.anil.banking.support.DatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end checks of the REST layer: status codes and error bodies. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private DatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.clean();
    }

    @Test
    void fullFlowOverHttp() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Anil Katroth","email":"anil@example.com","phone":"9652336856"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":1,"accountType":"SAVINGS"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","phone":"12"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void negativeAmountIsRejected() throws Exception {
        mvc.perform(post("/api/accounts/100000000001/deposit").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":-50}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mvc.perform(get("/api/accounts/999999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account 999999999999 not found"));
    }
}
