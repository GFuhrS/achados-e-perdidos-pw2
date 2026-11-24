package br.edu.ifrs.pw2.objeto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/objetos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ObjetoResource {
    private static final Logger LOG = Logger.getLogger(ObjetoResource.class);

    @Inject
    MeterRegistry metrics;

    public record ObjetoInput(
            @NotBlank @Size(max = 120) String titulo,
            @NotBlank @Size(max = 500) String descricao,
            @NotNull Categoria categoria,
            @NotBlank @Size(max = 150) String localEncontrado,
            @NotNull @PastOrPresent LocalDate dataEncontrado) {}

    public record StatusInput(@NotNull ObjetoStatus status, Long solicitacaoId) {}
    public record Erro(String erro) {}
    public record ObjetoView(Long id, String titulo, String descricao, Categoria categoria,
            String localEncontrado, LocalDate dataEncontrado, ObjetoStatus status,
            LocalDateTime criadoEm, Long solicitacaoAprovadaId) {
        static ObjetoView of(ObjetoEncontrado o) {
            return new ObjetoView(o.id, o.titulo, o.descricao, o.categoria,
                    o.localEncontrado, o.dataEncontrado, o.status, o.criadoEm,
                    o.solicitacaoAprovadaId);
        }
    }

    @POST
    @RolesAllowed("Admin")
    @Transactional
    public Response criar(@Valid ObjetoInput input) {
        ObjetoEncontrado o = new ObjetoEncontrado();
        preencher(o, input);
        o.status = ObjetoStatus.DISPONIVEL;
        o.criadoEm = LocalDateTime.now();
        o.persist();
        metrics.counter("objetos_cadastrados").increment();
        LOG.infof("Objeto cadastrado: id=%d", o.id);
        return Response.status(Response.Status.CREATED).entity(ObjetoView.of(o)).build();
    }

    @GET
    @RolesAllowed({"User", "Admin"})
    public List<ObjetoView> listar(@QueryParam("status") ObjetoStatus status,
            @QueryParam("categoria") Categoria categoria, @QueryParam("titulo") String titulo) {
        String busca = titulo == null ? null : titulo.toLowerCase(Locale.ROOT);
        return ObjetoEncontrado.<ObjetoEncontrado>listAll().stream()
                .filter(o -> status == null || o.status == status)
                .filter(o -> categoria == null || o.categoria == categoria)
                .filter(o -> busca == null || o.titulo.toLowerCase(Locale.ROOT).contains(busca))
                .map(ObjetoView::of).toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"User", "Admin"})
    public Response buscar(@PathParam("id") Long id) {
        ObjetoEncontrado o = ObjetoEncontrado.findById(id);
        return o == null ? erro(Response.Status.NOT_FOUND, "Objeto não encontrado")
                : Response.ok(ObjetoView.of(o)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("Admin")
    @Transactional
    public Response atualizar(@PathParam("id") Long id, @Valid ObjetoInput input) {
        ObjetoEncontrado o = ObjetoEncontrado.findById(id);
        if (o == null) return erro(Response.Status.NOT_FOUND, "Objeto não encontrado");
        preencher(o, input);
        LOG.infof("Objeto atualizado: id=%d", id);
        return Response.ok(ObjetoView.of(o)).build();
    }

    @PUT
    @Path("/{id}/status")
    @RolesAllowed("Admin")
    @Transactional
    public Response alterarStatus(@PathParam("id") Long id, @Valid StatusInput input) {
        ObjetoEncontrado o = ObjetoEncontrado.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (o == null) return erro(Response.Status.NOT_FOUND, "Objeto não encontrado");
        if (input.status == ObjetoStatus.DISPONIVEL) {
            return erro(Response.Status.CONFLICT, "Não é possível retornar a DISPONIVEL");
        }
        if (input.status == ObjetoStatus.DEVOLVIDO && input.solicitacaoId == null) {
            return erro(Response.Status.BAD_REQUEST, "solicitacaoId é obrigatório para devolução");
        }
        if (o.status == ObjetoStatus.DEVOLVIDO && input.status == ObjetoStatus.DEVOLVIDO
                && input.solicitacaoId.equals(o.solicitacaoAprovadaId)) {
            return Response.ok(ObjetoView.of(o)).build();
        }
        if (o.status != ObjetoStatus.DISPONIVEL) {
            return erro(Response.Status.CONFLICT, "Transição de status inválida");
        }
        o.status = input.status;
        o.solicitacaoAprovadaId = input.status == ObjetoStatus.DEVOLVIDO ? input.solicitacaoId : null;
        LOG.infof("Status do objeto alterado: id=%d status=%s", id, o.status);
        return Response.ok(ObjetoView.of(o)).build();
    }

    private void preencher(ObjetoEncontrado o, ObjetoInput input) {
        o.titulo = input.titulo.trim();
        o.descricao = input.descricao.trim();
        o.categoria = input.categoria;
        o.localEncontrado = input.localEncontrado.trim();
        o.dataEncontrado = input.dataEncontrado;
    }

    private Response erro(Response.Status status, String mensagem) {
        return Response.status(status).entity(new Erro(mensagem)).build();
    }
}
