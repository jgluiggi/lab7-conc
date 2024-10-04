import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.*;

public class Cliente implements Runnable {
    private final int clienteId;
    private final AtomicInteger pedidoIdGenerator;
    private final BlockingQueue<Pedido> filaDePedidos;

    public Cliente(int clienteId, AtomicInteger pedidoIdGenerator, BlockingQueue<Pedido> filaDePedidos) {
        this.clienteId = clienteId;
        this.pedidoIdGenerator = pedidoIdGenerator;
        this.filaDePedidos = filaDePedidos;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int pedidoId = pedidoIdGenerator.incrementAndGet();
                
                // System.out.println("Cliente " + clienteId + " fazendo pedido ID: " + pedidoId);

                Map<String, Integer> produtos = new ConcurrentHashMap<>();
                produtos.put("ProdutoA", random.nextInt(5) + 1);
                produtos.put("ProdutoB", random.nextInt(3) + 1);
                produtos.put("ProdutoC", random.nextInt(4) + 1);

                filaDePedidos.put(new Pedido(pedidoId, clienteId, produtos));

                Thread.sleep(random.nextInt(2000) + 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}