import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.concurrent.locks.*;

public class SistemaECommerce {

    // Fila de pedidos (com capacidade limitada)
    private static BlockingQueue<Pedido> filaDePedidos = new LinkedBlockingQueue<>(50);

    // Fila de pedidos pendentes
    private static BlockingQueue<Pedido> filaDePedidosPendentes = new LinkedBlockingQueue<>();

    // Gerador de ID de pedidos único
    private static AtomicInteger pedidoIdGenerator = new AtomicInteger(0);

    // Executor para gerenciar threads de clientes e workers
    private static ExecutorService executor = Executors.newFixedThreadPool(10); // Exemplo com 10 threads

    // Estoque compartilhado de produtos
    private static Map<String, Integer> estoque = new HashMap<>();
    private static ReentrantReadWriteLock estoqueLock = new ReentrantReadWriteLock();

    // Variáveis de controle de vendas e rejeições
    private static AtomicInteger pedidosProcessados = new AtomicInteger(0);
    private static AtomicInteger valorTotalVendas = new AtomicInteger(0);
    private static AtomicInteger pedidosRejeitados = new AtomicInteger(0);

    public static void main(String[] args) {
        // Inicializa estoque
        inicializarEstoque();

        // Iniciando threads de clientes para fazer pedidos
        for (int i = 0; i < 5; i++) { // Exemplo com 5 clientes
            executor.submit(new Cliente(i + 1, pedidoIdGenerator, filaDePedidos));
        }

        // Iniciando threads de workers para processar os pedidos
        for (int i = 0; i < 3; i++) { // Exemplo com 3 workers
            executor.submit(new Worker(valorTotalVendas, estoqueLock, i + 1, filaDePedidos, pedidosProcessados, pedidosRejeitados, filaDePedidosPendentes, estoque));
        }

        // Inicia uma thread para reabastecer o estoque periodicamente
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(10000); // A cada 10 segundos
                    reabastecerEstoque();
                    reprocessarPedidosPendentes(); // Reprocessa pedidos pendentes após o reabastecimento
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Inicia uma thread para exibir relatórios periodicamente a cada 30 segundos
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(30000); // A cada 30 segundos
                    exibirRelatorio();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // Inicializa o estoque de produtos
    private static void inicializarEstoque() {
        estoque.put("ProdutoA", 100);
        estoque.put("ProdutoB", 150);
        estoque.put("ProdutoC", 200);
        System.out.println("Estoque inicializado com " + estoque.get("ProdutoA") + " itens de ProdutoA, " 
        + estoque.get("ProdutoB") + " itens de ProdutoB, e " 
        + estoque.get("ProdutoC") + " itens de ProdutoC.");
    }

    // Simula o reabastecimento do estoque
    private static void reabastecerEstoque() {
        estoqueLock.writeLock().lock();
        try {
            System.out.println("Reabastecendo o estoque...");
            estoque.put("ProdutoA", estoque.get("ProdutoA") + 50);
            estoque.put("ProdutoB", estoque.get("ProdutoB") + 50);
            estoque.put("ProdutoC", estoque.get("ProdutoC") + 50);
            System.out.println("Sistema reabastecido com 50 itens de ProdutoA, 50 itens de ProdutoB, 50 itens de ProdutoC.");
        } finally {
            estoqueLock.writeLock().unlock();
        }
    }

    // Reprocessa os pedidos pendentes após o reabastecimento
    private static void reprocessarPedidosPendentes() {
        System.out.println("Reprocessando pedidos pendentes...");
        List<Pedido> reprocessar = new ArrayList<>();
        filaDePedidosPendentes.drainTo(reprocessar); // Move todos os pedidos pendentes para reprocessar
        for (Pedido pedido : reprocessar) {
            try {
                filaDePedidos.put(pedido); // Coloca os pedidos de volta na fila principal para processamento
                System.out.println("Pedido " + pedido.getId() + " do Cliente " + pedido.getClienteId() + " foi reprocessado.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Método para exibir o relatório de vendas
    private static void exibirRelatorio() {
        System.out.println("--------------------");
        System.out.println("Relatório de Vendas: ");
        System.out.println("Pedidos Processados: " + pedidosProcessados.get());
        System.out.println("Valor Total das Vendas: " + valorTotalVendas.get());
        System.out.println("Pedidos Rejeitados: " + pedidosRejeitados.get());
        System.out.println("--------------------");
    }
}