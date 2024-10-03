import java.util.*;

public class Pedido {
    private final int id;
    private final Map<String, Integer> produtos;

    public Pedido(int id, Map<String, Integer> produtos) {
        this.id = id;
        this.produtos = produtos;
    }

    public int getId() {
        return id;
    }

    public Map<String, Integer> getProdutos() {
        return produtos;
    }
}
