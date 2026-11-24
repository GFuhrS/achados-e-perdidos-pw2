package br.edu.ifrs.pw2.solicitacao;

import java.time.LocalDateTime;
import java.util.List;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/solicitacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolicitacaoResource {
    private static final Logger LOG = Logger.getLogger(SolicitacaoResource.class);

    @Inject
    ObjetoGateway objetos;

    @Inject
    MeterRegistry metrics;

    public record SolicitacaoInput(@NotNull Long objetoId,
            @NotBlank @Size(max = 120) String nomeSolicitante,
            @NotBlank @Email @Size(max = 254) String emailSolicitante,
            @NotBlank @Size(max = 500) String comprovacao) {}

    public record Erro(String erro) {}
    public record SolicitacaoView(Long id, Long objetoId, String nomeSolicitante,
            String emailSolicitante, String comprovacao, SolicitacaoStatus status,
            LocalDateTime criadaEm, LocalDateTime analisadaEm) {
        static SolicitacaoView of(SolicitacaoDevolucao s) {
            return new SolicitacaoView(s.id, s.objetoId, s.nomeSolicitante,
                    s.emailSolicitante, s.comprovacao, s.status, s.criadaEm, s.analisadaEm);
        }
    }

    @POST
    @RolesAllowed({"User", "Admin"})
    @Transactional
    public Response criar(@Valid SolicitacaoInput input) {
        try {
            ObjetoApi.Objeto objeto = objetos.buscar(input.objetoId);
            if (!"DISPONIVEL".equals(objeto.status())) {
                return erro(Response.Status.CONFLICT, "Objeto não está disponível");
            }
            SolicitacaoDevolucao s = new SolicitacaoDevolucao();
            s.objetoId = input.objetoId;
            s.nomeSolicitante = input.nomeSolicitante.trim();
            s.emailSolicitante = input.emailSolicitante.trim();
            s.comprovacao = input.comprovacao.trim();
            s.status = SolicitacaoStatus.PENDENTE;
            s.criadaEm = LocalDateTime.now();
            s.persist();
            metrics.counter("solicitacoes_criadas").increment();
            LOG.infof("Solicitação criada: id=%d objeto=%d", s.id, s.objetoId);
            return Response.status(Response.Status.CREATED).entity(SolicitacaoView.of(s)).build();
        } catch (ObjetoIndisponivel e) {
            LOG.warn("Falha de comunicação ao criar solicitação", e);
            return erro(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (WebApplicationException e) {
            return erro(statusRemoto(e), "Objeto não disponível para solicitação");
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"User", "Admin"})
    public Response buscar(@PathParam("id") Long id) {
        SolicitacaoDevolucao s = SolicitacaoDevolucao.findById(id);
        return s == null ? erro(Response.Status.NOT_FOUND, "Solicitação não encontrada")
                : Response.ok(SolicitacaoView.of(s)).build();
    }

    @GET
    @RolesAllowed("Admin")
    public List<SolicitacaoView> listar(@QueryParam("status") SolicitacaoStatus status,
            @QueryParam("objetoId") Long objetoId, @QueryParam("email") String email) {
        return SolicitacaoDevolucao.<SolicitacaoDevolucao>listAll().stream()
                .filter(s -> status == null || s.status == status)
                .filter(s -> objetoId == null || s.objetoId.equals(objetoId))
                .filter(s -> email == null || s.emailSolicitante.equalsIgnoreCase(email))
                .map(SolicitacaoView::of).toList();
    }

    @PUT
    @Path("/{id}/aprovar")
    @RolesAllowed("Admin")
    @Transactional
    public Response aprovar(@PathParam("id") Long id) {
        SolicitacaoDevolucao s = SolicitacaoDevolucao.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (s == null) return erro(Response.Status.NOT_FOUND, "Solicitação não encontrada");
        if (s.status != SolicitacaoStatus.PENDENTE)
            return erro(Response.Status.CONFLICT, "Solicitação já analisada");
        try {
            objetos.devolver(s.objetoId, s.id);
            s.status = SolicitacaoStatus.APROVADA;
            s.analisadaEm = LocalDateTime.now();
            metrics.counter("solicitacoes_aprovadas").increment();
            LOG.infof("Solicitação aprovada: id=%d objeto=%d", s.id, s.objetoId);
            return Response.ok(SolicitacaoView.of(s)).build();
        } catch (ObjetoIndisponivel e) {
            LOG.warnf("Falha de comunicação ao aprovar solicitação id=%d", id);
            return erro(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        } catch (WebApplicationException e) {
            return erro(statusRemoto(e), "Objeto não pode ser devolvido");
        }
    }

    @PUT
    @Path("/{id}/rejeitar")
    @RolesAllowed("Admin")
    @Transactional
    public Response rejeitar(@PathParam("id") Long id) {
        SolicitacaoDevolucao s = SolicitacaoDevolucao.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (s == null) return erro(Response.Status.NOT_FOUND, "Solicitação não encontrada");
        if (s.status != SolicitacaoStatus.PENDENTE)
            return erro(Response.Status.CONFLICT, "Solicitação já analisada");
        s.status = SolicitacaoStatus.REJEITADA;
        s.analisadaEm = LocalDateTime.now();
        LOG.infof("Solicitação rejeitada: id=%d", id);
        return Response.ok(SolicitacaoView.of(s)).build();
    }

    private Response.Status statusRemoto(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        if (status == 404) return Response.Status.NOT_FOUND;
        if (status == 409) return Response.Status.CONFLICT;
        LOG.warnf("Resposta inesperada do Objeto Service: HTTP %d", status);
        return Response.Status.BAD_GATEWAY;
    }

    private Response erro(Response.Status status, String mensagem) {
        return Response.status(status).entity(new Erro(mensagem)).build();
    }
}
