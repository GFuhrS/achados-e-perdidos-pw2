package br.edu.ifrs.pw2.solicitacao;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SolicitacaoResourceTest {
    private static final String NOVA = """
            {"objetoId":10,"nomeSolicitante":"Ana Silva",
             "emailSolicitante":"ana@example.com","comprovacao":"Minha chave tem fita verde"}
            """;

    @InjectMock
    ObjetoGateway objetos;

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void criaConsultaListaEAprova() {
        when(objetos.buscar(10L)).thenReturn(new ObjetoApi.Objeto(10L, "DISPONIVEL", null));
        when(objetos.devolver(eq(10L), any())).thenReturn(new ObjetoApi.Objeto(10L, "DEVOLVIDO", 1L));
        int id = given().contentType(ContentType.JSON).body(NOVA)
                .when().post("/solicitacoes")
                .then().statusCode(201).body("status", equalTo("PENDENTE"))
                .extract().path("id");
        given().when().get("/solicitacoes/{id}", id)
                .then().statusCode(200).body("nomeSolicitante", equalTo("Ana Silva"));
        given().queryParam("objetoId", 10).when().get("/solicitacoes")
                .then().statusCode(200).body("size()", greaterThanOrEqualTo(1));
        given().when().put("/solicitacoes/{id}/aprovar", id)
                .then().statusCode(200).body("status", equalTo("APROVADA"));
        verify(objetos).devolver(10L, (long) id);
        given().when().put("/solicitacoes/{id}/aprovar", id).then().statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void rejeitaESeguraFalhaRemota() {
        when(objetos.buscar(10L)).thenReturn(new ObjetoApi.Objeto(10L, "DISPONIVEL", null));
        int rejeitada = given().contentType(ContentType.JSON).body(NOVA)
                .when().post("/solicitacoes").then().statusCode(201).extract().path("id");
        given().when().put("/solicitacoes/{id}/rejeitar", rejeitada)
                .then().statusCode(200).body("status", equalTo("REJEITADA"));
        given().when().put("/solicitacoes/{id}/aprovar", rejeitada).then().statusCode(409);

        int pendente = given().contentType(ContentType.JSON).body(NOVA)
                .when().post("/solicitacoes").then().statusCode(201).extract().path("id");
        when(objetos.devolver(eq(10L), any())).thenThrow(new ObjetoIndisponivel());
        given().when().put("/solicitacoes/{id}/aprovar", pendente).then().statusCode(503);
        given().when().get("/solicitacoes/{id}", pendente)
                .then().statusCode(200).body("status", equalTo("PENDENTE"));
    }

    @Test
    @TestSecurity(user = "user", roles = "User")
    void usuarioCriaMasNaoAnalisa() {
        when(objetos.buscar(10L)).thenReturn(new ObjetoApi.Objeto(10L, "DISPONIVEL", null));
        int id = given().contentType(ContentType.JSON).body(NOVA)
                .when().post("/solicitacoes").then().statusCode(201).extract().path("id");
        given().when().get("/solicitacoes/{id}", id).then().statusCode(200);
        given().when().get("/solicitacoes").then().statusCode(403);
        given().when().put("/solicitacoes/{id}/aprovar", id).then().statusCode(403);
        given().when().put("/solicitacoes/{id}/rejeitar", id).then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void naoCriaQuandoObjetoIndisponivel() {
        when(objetos.buscar(10L)).thenThrow(new ObjetoIndisponivel());
        given().contentType(ContentType.JSON).body(NOVA)
                .when().post("/solicitacoes").then().statusCode(503);
    }

    @Test
    void exigeAutenticacao() {
        given().when().get("/solicitacoes").then().statusCode(401);
    }

    @Test
    void publicaHealthEMetricas() {
        given().when().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
        given().when().get("/q/metrics").then().statusCode(200);
    }
}
