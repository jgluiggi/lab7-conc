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
            executor.submit(new Cliente(i + 1));
        }

        // Iniciando threads de workers para processar os pedidos
        for (int i = 0; i < 3; i++) { // Exemplo com 3 workers
            executor.submit(new Worker(i + 1));
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
    }

    // Simula o reabastecimento do estoque
    private static void reabastecerEstoque() {
        estoqueLock.writeLock().lock();
        try {
            System.out.println("Reabastecendo o estoque...");
            estoque.put("ProdutoA", estoque.get("ProdutoA") + 50);
            estoque.put("ProdutoB", estoque.get("ProdutoB") + 50);
            estoque.put("ProdutoC", estoque.get("ProdutoC") + 50);
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Método para exibir o relatório de vendas
    private static void exibirRelatorio() {
        System.out.println("--------------------");
        System.out.println("Relatório de Vendas:");
        System.out.println("Pedidos Processados: " + pedidosProcessados.get());
        System.out.println("Valor Total das Vendas: " + valorTotalVendas.get());
        System.out.println("Pedidos Rejeitados: " + pedidosRejeitados.get());
        System.out.println("--------------------");
    }

    // Classe representando um Pedido
    static class Pedido {
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

    // Cliente que faz pedidos
    static class Cliente implements Runnable {
        private final int clienteId;

        public Cliente(int clienteId) {
            this.clienteId = clienteId;
        }

        @Override
        public void run() {
            Random random = new Random();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int pedidoId = pedidoIdGenerator.incrementAndGet();
                    System.out.println("Cliente " + clienteId + " fazendo pedido ID: " + pedidoId);

                    // Gera produtos aleatórios para o pedido
                    Map<String, Integer> produtos = new HashMap<>();
                    produtos.put("ProdutoA", random.nextInt(5) + 1);
                    produtos.put("ProdutoB", random.nextInt(3) + 1);
                    produtos.put("ProdutoC", random.nextInt(4) + 1);

                    filaDePedidos.put(new Pedido(pedidoId, produtos));

                    // Espera randômica entre pedidos
                    Thread.sleep(random.nextInt(2000) + 1000); // De 1 a 3 segundos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Worker que processa pedidos
    static class Worker implements Runnable {
        private final int workerId;

        public Worker(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Pedido pedido = filaDePedidos.take();
                    System.out.println("Worker " + workerId + " processando pedido ID: " + pedido.getId());

                    if (processarPedido(pedido)) {
                        pedidosProcessados.incrementAndGet();
                    } else {
                        pedidosRejeitados.incrementAndGet();
                        filaDePedidosPendentes.put(pedido); // Move o pedido para a fila de espera
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
}
