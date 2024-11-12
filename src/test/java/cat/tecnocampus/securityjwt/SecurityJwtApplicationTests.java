package cat.tecnocampus.securityjwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityJwtApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void helloWorld() throws Exception {
        mockMvc.perform(get("/helloWorld"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"));
    }

    @Test
    @WithMockUser(username = "maria", authorities = {"SCOPE_ADMIN", "SCOPE_USER"})
    void helloAdmin() throws Exception {
        mockMvc.perform(get("/helloAdmin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello Admin"));
       mockMvc.perform(get("/helloUser"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello User"));
    }

    @Test
    @WithMockUser(username = "maria", authorities = {"SCOPE_ADMIN"})
    void helloUserFromAdmin() throws Exception {
        mockMvc.perform(get("/helloUser"))
                .andExpect(status().isForbidden());
    }

}
