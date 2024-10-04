import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.*;

public class Worker implements Runnable {
    private final int workerId;
    private final BlockingQueue<Pedido> filaDePedidos;
    private final AtomicInteger pedidosProcessados;
    private final AtomicInteger pedidosRejeitados;
    private final BlockingQueue<Pedido> filaDePedidosPendentes;
    private final Map<String, Integer> estoque;
    private final ReentrantReadWriteLock estoqueLock;
    private final AtomicInteger valorTotalVendas;

    public Worker(AtomicInteger valorTotalVendas, ReentrantReadWriteLock estoqueLock, int workerId, BlockingQueue<Pedido> filaDePedidos, AtomicInteger pedidosProcessados, AtomicInteger pedidosRejeitados, BlockingQueue<Pedido> filaDePedidosPendentes, Map<String, Integer> estoque){
        this.workerId = workerId;
        this.pedidosProcessados = pedidosProcessados;
        this.filaDePedidos = filaDePedidos;
        this.pedidosRejeitados = pedidosRejeitados;
        this.filaDePedidosPendentes = filaDePedidosPendentes;
        this.estoque = estoque;
        this.estoqueLock = estoqueLock;
        this.valorTotalVendas = valorTotalVendas;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Pedido pedido = filaDePedidos.take();
                String mensagem = "Pedido " + pedido.getId() + " do Cliente " + pedido.getClienteId();
                System.out.println("Worker " + workerId + " processando " + mensagem);

                if (processarPedido(pedido)) {
                    pedidosProcessados.incrementAndGet();
                    System.out.println("Pedido ID: " + pedido.getId() + " do Cliente " + pedido.getClienteId() + " processado pelo Worker " + workerId + ".");
                } else {
                    pedidosRejeitados.incrementAndGet();
                    filaDePedidosPendentes.put(pedido); // Move o pedido para a fila de espera
                    System.out.println("Pedido ID: " + pedido.getId() + " do Cliente " + pedido.getClienteId() + " movido para fila de espera.");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Método que processa um pedido
    private boolean processarPedido(Pedido pedido) {
        estoqueLock.readLock().lock();
        try {
            // Verifica se há produtos disponíveis no estoque
            for (Map.Entry<String, Integer> item : pedido.getProdutos().entrySet()) {
                if (estoque.getOrDefault(item.getKey(), 0) < item.getValue()) {
                    System.out.println("Pedido ID: " + pedido.getId() + " movido para fila de espera por estoque vazio.");
                    return false;
                }
            }
        } finally {
            estoqueLock.readLock().unlock();
        }

        // Desconta os produtos do estoque
        estoqueLock.writeLock().lock();
        try {
            for (Map.Entry<String, Integer> item : pedido.getProdutos().entrySet()) {
                estoque.put(item.getKey(), estoque.get(item.getKey()) - item.getValue());
            }
            valorTotalVendas.addAndGet(calcularValorTotal(pedido));
            System.out.println("Pedido ID: " + pedido.getId() + " processado com sucesso.");
        } finally {
            estoqueLock.writeLock().unlock();
        }

        return true;
    }

    // Calcula o valor total do pedido
    private int calcularValorTotal(Pedido pedido) {
        // Simula preços para os produtos
        int total = 0;
        total += pedido.getProdutos().getOrDefault("ProdutoA", 0) * 10;
        total += pedido.getProdutos().getOrDefault("ProdutoB", 0) * 20;
        total += pedido.getProdutos().getOrDefault("ProdutoC", 0) * 15;
        return total;
    }
}