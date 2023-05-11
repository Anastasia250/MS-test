package com.itm.space.backendresources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.controller.RestExceptionHandler;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.mock.StubClass;
import com.itm.space.backendresources.util.JsonUtil;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends BaseIntegrationTest {
    @MockBean
    private StubClass stubClass;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "ROLE_MODERATOR")
    @DisplayName("тест на создание User")
    void testCreateUser() throws Exception {
        UserRequest userRequest = new UserRequest(
                "Test",
                "test@mail.ru",
                "1234",
                "test",
                "test2");

        doReturn("userId").when(stubClass).createUser(any());
        mvc.perform(post("http://localhost:9191/api/users")
                        .content(asJsonString(userRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("тесе метода контроллера")
    @WithMockUser(username = "user", roles = {"MODERATOR"})
    void testGetUserByIdOk() throws Exception {
        //give
        UUID id = UUID.randomUUID();
        String firstName = "John";
        String lastName = "Doe";
        String roleName = "ROLE_USER";
        String email = "johndoe@gmail.com";
        String groupName = "GROUP1";


        // stubs for IdentityWorker
        UserRepresentation userRepresentation = createUserRepresentation();
        List<GroupRepresentation> groupsRepresentation = createGroupsRepresentation(groupName);
        List<RoleRepresentation> roleRepresentation = createRolesRepresentation(roleName);

        doReturn(roleRepresentation).when(stubClass).getUserRoles(id);
        doReturn(groupsRepresentation).when(stubClass).getUserGroups(id);
        doReturn(userRepresentation).when(stubClass).getUserById(id);


        //then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{id}", id))

                //assert
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.firstName").value(firstName))
                .andExpect(jsonPath("$.lastName").value(lastName))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value(roleName))
                .andExpect(jsonPath("$.groups[0]").value(groupName));

    }

    @Test
    @DisplayName("тест на возврат пользователя по идентификатору")
    public void testGetUserByIdWithValidId() throws Exception {
        String validId = "valid-id";
        mockMvc.perform(get("/api/users/{id}", validId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    @DisplayName("тест на проверку доступа к API - эндпоинту")
    @WithMockUser(username = "test", roles = "USER")
    public void testGetUserByIdUnauthorizedRoles() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/users" + "/" + randomId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("тест на создание пользователя без авторизации")
    void testCreateUserUnauthorized() throws Exception {
        UserRequest userRequest = new UserRequest("test",
                "test@example.com",
                "password",
                "John",
                "Doe");
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userRequest));
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    @DisplayName("тест на правильность обработки исключения")
    public void handleBackendResourcesExceptionTest() {
        BackendResourcesException exception = new BackendResourcesException("Test Error", HttpStatus.INTERNAL_SERVER_ERROR);
        RestExceptionHandler controller = new RestExceptionHandler();
        ResponseEntity<String> responseEntity = controller.handleException(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals("Test Error", responseEntity.getBody());
    }


    @SneakyThrows
    private UserRepresentation createUserRepresentation() {
        return JsonUtil.getObjectFromJson("json/userRepresentation_correct.json", UserRepresentation.class);
    }

    private List<GroupRepresentation> createGroupsRepresentation(String nameOfGroup) {
        List<GroupRepresentation> groupsRepresentation = new ArrayList<>();
        var group = new GroupRepresentation();
        group.setName(nameOfGroup);
        groupsRepresentation.add(group);
        return groupsRepresentation;
    }

    private List<RoleRepresentation> createRolesRepresentation(String nameOfRole) {
        List<RoleRepresentation> rolesRepresentation = new ArrayList<>();
        var role = new RoleRepresentation();
        role.setName(nameOfRole);
        rolesRepresentation.add(role);
        return rolesRepresentation;
    }
}

