package br.edu.ifrs.pw2.solicitacao;

public class ObjetoIndisponivel extends RuntimeException {
    public ObjetoIndisponivel() {
        super("Objeto Service indisponível");
    }
}
