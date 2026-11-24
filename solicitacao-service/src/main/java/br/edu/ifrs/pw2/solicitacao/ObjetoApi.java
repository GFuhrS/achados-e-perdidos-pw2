package br.edu.ifrs.pw2.solicitacao;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/objetos")
@RegisterRestClient(configKey = "objetos-api")
@RegisterClientHeaders
public interface ObjetoApi {
    record Objeto(Long id, String status, Long solicitacaoAprovadaId) {}
    record AlterarStatus(String status, Long solicitacaoId) {}

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Objeto buscar(@PathParam("id") Long id);

    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Objeto alterarStatus(@PathParam("id") Long id, AlterarStatus input);
}
