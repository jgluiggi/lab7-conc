import java.util.*;

public class Pedido {
    private final int id;
    private final int clienteId;
    private final Map<String, Integer> produtos;

    public Pedido(int id, int clienteId, Map<String, Integer> produtos) {
        this.id = id;
        this.produtos = produtos;
        this.clienteId = clienteId;
    }

    public int getId() {
        return id;
    }
    public int getClienteId() {
        return clienteId;
    }

    public Map<String, Integer> getProdutos() {
        return produtos;
    }
}
