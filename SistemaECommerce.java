import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.locks.*;

public class SistemaECommerce {
    private static BlockingQueue<Pedido> filaDePedidos = new LinkedBlockingQueue<>(50);

    private static BlockingQueue<Pedido> filaDePedidosPendentes = new LinkedBlockingQueue<>();

    private static AtomicInteger pedidoIdGenerator = new AtomicInteger(0);


    private static ExecutorService executor = Executors.newFixedThreadPool(10); // Exemplo com 10 threads

    private static Map<String, Integer> estoque = new ConcurrentHashMap<>();
    private static Map<String, AtomicInteger> produtosVendidos = new ConcurrentHashMap<>();
    private static ReentrantReadWriteLock estoqueLock = new ReentrantReadWriteLock();


    private static AtomicInteger pedidosProcessados = new AtomicInteger(0);
    private static AtomicInteger valorTotalVendas = new AtomicInteger(0);
    private static AtomicInteger pedidosRejeitados = new AtomicInteger(0);

    public static void main(String[] args) {

        inicializarEstoque();


        for (int i = 0; i < 5; i++) {
            executor.submit(new Cliente(i + 1, pedidoIdGenerator, filaDePedidos));
        }


        for (int i = 0; i < 3; i++) {
            executor.submit(new Worker(valorTotalVendas, estoqueLock, i + 1, filaDePedidos, pedidosProcessados, pedidosRejeitados, filaDePedidosPendentes, estoque, produtosVendidos));
        }

        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10000);
                    reabastecerEstoque();
                    reprocessarPedidosPendentes();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });


        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30000);
                    exibirRelatorio();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static void inicializarEstoque() {
        estoque.put("ProdutoA", 100);
        estoque.put("ProdutoB", 150);
        estoque.put("ProdutoC", 200);
        produtosVendidos.put("ProdutoA", new AtomicInteger(0));
        produtosVendidos.put("ProdutoB", new AtomicInteger(0));
        produtosVendidos.put("ProdutoC", new AtomicInteger(0));
        System.out.println("Estoque inicializado com " + estoque.get("ProdutoA") + " itens de ProdutoA, " 
        + estoque.get("ProdutoB") + " itens de ProdutoB, e " 
        + estoque.get("ProdutoC") + " itens de ProdutoC.");
    }

    // private static void reabastecerEstoque() {
    //     estoqueLock.writeLock().lock();
    //     try {
    //         System.out.println("Reabastecendo o estoque...");
    //         estoque.put("ProdutoA", estoque.get("ProdutoA") + 50);
    //         estoque.put("ProdutoB", estoque.get("ProdutoB") + 50);
    //         estoque.put("ProdutoC", estoque.get("ProdutoC") + 50);
    //         System.out.println("Sistema reabastecido com 50 itens de ProdutoA, 50 itens de ProdutoB, 50 itens de ProdutoC.");
    //     } finally {
    //         estoqueLock.writeLock().unlock();
    //     }
    // }

    private static void reabastecerEstoque() {
        estoqueLock.writeLock().lock();
        try {
            System.out.println("Reabastecendo o estoque...");
            for (Map.Entry<String, AtomicInteger> produto : produtosVendidos.entrySet()) {
                String nomeProduto = produto.getKey();
                if (produto.getValue().get() > 50) { 
                    estoque.put(nomeProduto, estoque.get(nomeProduto) + 100);
                } else {
                    estoque.put(nomeProduto, estoque.get(nomeProduto) + 50);
                }
            }
        } finally {
            estoqueLock.writeLock().unlock();
        }
    }

    private static void reprocessarPedidosPendentes() {
        System.out.println("Reprocessando pedidos pendentes...");
        List<Pedido> reprocessar = new ArrayList<>();
        filaDePedidosPendentes.drainTo(reprocessar);
        for (Pedido pedido : reprocessar) {
            try {
                filaDePedidos.put(pedido);
                System.out.println("Pedido " + pedido.getId() + " do Cliente " + pedido.getClienteId() + " foi reprocessado.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void exibirRelatorio() {
        System.out.println("----------------RELATÓRIO----------------");
        System.out.println("Relatório de Vendas: ");
        System.out.println("Pedidos Processados: " + pedidosProcessados.get());
        System.out.println("Valor Total das Vendas: " + valorTotalVendas.get());
        System.out.println("Pedidos Rejeitados: " + pedidosRejeitados.get());
        System.out.println("----------------RELATÓRIO----------------");
    }
}