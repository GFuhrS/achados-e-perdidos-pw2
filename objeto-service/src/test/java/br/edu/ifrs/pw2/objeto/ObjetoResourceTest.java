package br.edu.ifrs.pw2.objeto;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ObjetoResourceTest {
    private static final String NOVO = """
            {"titulo":"Chave preta","descricao":"Encontrada perto da biblioteca",
             "categoria":"ACESSORIO","localEncontrado":"Biblioteca",
             "dataEncontrado":"2024-05-10"}
            """;

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void administraOsCincoEndpointsEValidaTransicoes() {
        int id = given().contentType(ContentType.JSON).body(NOVO)
                .when().post("/objetos")
                .then().statusCode(201).body("status", equalTo("DISPONIVEL"))
                .extract().path("id");

        given().when().get("/objetos/{id}", id)
                .then().statusCode(200).body("titulo", equalTo("Chave preta"));
        given().queryParam("categoria", "ACESSORIO").when().get("/objetos")
                .then().statusCode(200).body("size()", greaterThanOrEqualTo(1));
        given().contentType(ContentType.JSON).body(NOVO.replace("Chave preta", "Chave azul"))
                .when().put("/objetos/{id}", id)
                .then().statusCode(200).body("titulo", equalTo("Chave azul"));
        given().contentType(ContentType.JSON).body("{\"status\":\"DEVOLVIDO\"}")
                .when().put("/objetos/{id}/status", id).then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"status\":\"DEVOLVIDO\",\"solicitacaoId\":7}")
                .when().put("/objetos/{id}/status", id)
                .then().statusCode(200).body("status", equalTo("DEVOLVIDO"));
        // Mesmo ID permite repetir a chamada após perda de resposta HTTP.
        given().contentType(ContentType.JSON).body("{\"status\":\"DEVOLVIDO\",\"solicitacaoId\":7}")
                .when().put("/objetos/{id}/status", id).then().statusCode(200);
        given().contentType(ContentType.JSON).body("{\"status\":\"DESCARTADO\"}")
                .when().put("/objetos/{id}/status", id).then().statusCode(409);
        given().when().get("/objetos/999999999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "user", roles = "User")
    void usuarioApenasConsulta() {
        given().when().get("/objetos").then().statusCode(200);
        given().contentType(ContentType.JSON).body(NOVO)
                .when().post("/objetos").then().statusCode(403);
    }

    @Test
    void exigeAutenticacao() {
        given().when().get("/objetos").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = "Admin")
    void rejeitaCadastroInvalido() {
        given().contentType(ContentType.JSON).body(NOVO.replace("Chave preta", ""))
                .when().post("/objetos").then().statusCode(400);
    }

    @Test
    void publicaHealthEMetricas() {
        given().when().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
        given().when().get("/q/metrics").then().statusCode(200);
    }
}
