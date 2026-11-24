package br.edu.ifrs.pw2.solicitacao;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ObjetoGateway {
    private static final Logger LOG = Logger.getLogger(ObjetoGateway.class);

    @Inject
    @RestClient
    ObjetoApi api;

    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, retryOn = {ProcessingException.class, TimeoutException.class})
    @Fallback(fallbackMethod = "buscarFallback", skipOn = WebApplicationException.class)
    public ObjetoApi.Objeto buscar(Long id) {
        return api.buscar(id);
    }

    public ObjetoApi.Objeto buscarFallback(Long id) {
        LOG.warnf("Fallback ao consultar objeto id=%d", id);
        throw new ObjetoIndisponivel();
    }

    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200, retryOn = {ProcessingException.class, TimeoutException.class})
    @Fallback(fallbackMethod = "devolverFallback", skipOn = WebApplicationException.class)
    public ObjetoApi.Objeto devolver(Long objetoId, Long solicitacaoId) {
        return api.alterarStatus(objetoId, new ObjetoApi.AlterarStatus("DEVOLVIDO", solicitacaoId));
    }

    public ObjetoApi.Objeto devolverFallback(Long objetoId, Long solicitacaoId) {
        LOG.warnf("Fallback ao devolver objeto id=%d solicitacao=%d", objetoId, solicitacaoId);
        throw new ObjetoIndisponivel();
    }
}
