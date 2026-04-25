package GUI;




import blockchain.utils.MerkleTree;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Classe que representa graficamente uma Merkle Tree (árvore de Merkle).
 * Esta classe é um componente gráfico que exibe a estrutura de uma Merkle Tree
 * e permite verificar visualmente se um determinado elemento possui a prova (proof) necessária.
 * 
 * @author Catarina - Miguel
 */
public class MerkleGraphics extends javax.swing.JPanel {

    // A árvore de Merkle a ser exibida
    MerkleTree tree;

    // O elemento específico da MerkleTree para o qual a proof é exibida
    Object element;
    
    // A proof (prova) do elemento, representada como uma lista de hashes
    List<String> proof;

    /**
     * Verifica se os dados fornecidos estão contidos na proof (prova) do elemento.
     * 
     * @param data o hash a ser verificado na proof
     * @return true se o hash fornecido está contido na proof, false caso contrário
     */
    public boolean containsProof(String data) {
        if (proof == null) {
            return false;
        }
        return proof.contains(data);
    }

    /**
     * Construtor que inicializa os componentes gráficos da Merkle Tree.
     */
    public MerkleGraphics() {
        initComponents();
    }

    /**
     * Define a Merkle Tree a ser exibida no painel.
     * 
     * @param tree a MerkleTree a ser associada e exibida
     */
    public void setMerkle(MerkleTree tree) {
        this.tree = tree;
        repaint();
    }

    /**
     * Define a proof (prova) de um elemento específico da Merkle Tree para exibição.
     * 
     * @param element o elemento da MerkleTree para o qual a proof é associada
     * @param proof a proof (lista de hashes) que comprova a inclusão do elemento na árvore
     */
    public void setProof(Object element, List<String> proof) {
        this.element = element;
        this.proof = proof;
        repaint();
    }
    

     /**
     * Substitui o método paintComponent para desenhar a estrutura da Merkle Tree.
     * Este método desenha os elementos e ligações da árvore, indicando visualmente os nós 
     * que fazem parte da proof (comprovação de inclusão).
     * 
     * @param gr o contexto gráfico fornecido pelo Swing
     */
    @Override
    public void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        // Verifica se a árvore foi definida
        if (tree != null) {
            // Desenha as linhas de conexão entre os nós da árvore
            drawLines(gr);
            // Altura total da árvore em níveis (hashes e elementos)
            int height = tree.getHashTree().size() + 1;
            int sizeY = this.getHeight() / (height + 1);

            // Obtém a estrutura de hashes da Merkle Tree
            List<List<String>> hashTree = tree.getHashTree();
            for (int i = 0; i < hashTree.size(); i++) {

                int blocks = (int) Math.pow(2, i); // número de blocos no nível atual
                int size = this.getWidth() / blocks;

                // Desenha cada hash no nível atual
                for (int j = 0; j < hashTree.get(i).size(); j++) {
                    Color back = new Color(250, 200, 200);
                    if (containsProof(hashTree.get(i).get(j))) {
                        back = new Color(200, 255, 200);
                    }
                    
                    // Desenha o hash centralizado no retângulo designado
                    drawCenteredString(gr, hashTree.get(i).get(j),
                            new Rectangle(j * size, sizeY * i, size, sizeY),
                            Color.BLACK, back);
                }
            }
            
            // Desenha os elementos finais da árvore
            int blocks = (int) Math.pow(2, hashTree.size() - 1);
            int size = this.getWidth() / blocks;
            for (int j = 0; j < tree.getElements().size(); j++) {
                Color back = new Color(200, 200, 255);
                if (tree.getElements().get(j).equals(element)) {
                    back = new Color(200, 255, 200);
                }

                // Desenha o elemento final da árvore
                drawCenteredString(gr, tree.getElements().get(j).toString(),
                        new Rectangle(j * size, sizeY * hashTree.size(), size, sizeY),
                        Color.BLACK, back);
            }

        }

    }

    /**
     * Desenha as linhas de conexão entre os nós da Merkle Tree.
     * Este método liga cada hash aos nós filhos, construindo visualmente a estrutura da árvore.
     * 
     * @param gr o contexto gráfico fornecido pelo Swing
     */
    public void drawLines(Graphics gr) {
        gr.setColor(Color.BLACK); // Define a cor das linhas
       List<List<String>> hashTree = tree.getHashTree();
        for (int y = 1; y < hashTree.size(); y++) {
            for (int x = 0; x < hashTree.get(y).size(); x++) {
                // Determina o ponto central para cada hash, ligando-o ao nó pai
                Point p1 = getCenter(y - 1, x / 2);
                Point p2 = getCenter(y, x);
                gr.drawLine(p1.x, p1.y, p2.x, p2.y); // Desenha a linha de conexão
            }
        }
        // Desenha as linhas que ligam os últimos nós aos elementos da árvore
        int height = tree.getHashTree().size() + 1;
        int sizeY = this.getHeight() / (height + 1);
        for (int x = 0; x < tree.getElements().size(); x++) {
            Point p1 = getCenter(hashTree.size() - 1, x);
            gr.drawLine(p1.x, p1.y, p1.x, p1.y + sizeY); // Linha vertical para o último nível
        }

    }

    /**
     * Calcula o ponto central de um retângulo representando um nó da Merkle Tree.
     * 
     * @param y o nível do nó
     * @param x a posição do nó no nível
     * @return um ponto que representa o centro do retângulo onde o nó será desenhado
     */
    private Point getCenter(int y, int x) {
        int height = tree.getHashTree().size() + 1;
        int sizeY = this.getHeight() / (height + 1);
        int blocks = (int) Math.pow(2, y); // número de blocos no nível atual
        int size = this.getWidth() / blocks; // largura de cada bloco
        return new Point(x * size + size / 2, sizeY * y + sizeY / 2); // centro do bloco
    }

    /**
     * Desenha uma string centralizada no meio de um retângulo com uma cor de fundo e uma borda.
     * 
     * @param g o contexto gráfico
     * @param text o texto a ser desenhado
     * @param rect o retângulo onde o texto será centralizado
     * @param lineColor a cor da borda do retângulo
     * @param backColor a cor de fundo do retângulo
     */
    public void drawCenteredString(Graphics g, String text, Rectangle rect, Color lineColor, Color backColor) {

        Font font = new Font("Courier New", Font.BOLD, 14);
        // Obtém as métricas da fonte
        FontMetrics metrics = g.getFontMetrics(font);
        
        // Calcula as coordenadas X e Y para centralizar o texto
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        
        // Define a fonte e desenha o fundo e a borda do retângulo
        g.setFont(font);
        int step = 2;
        g.setColor(backColor);
        g.fillRect(x - step, y - metrics.getHeight(), metrics.stringWidth(text) + 2 * step, metrics.getHeight() * 2);
        g.setColor(lineColor);
        g.drawRect(x - step, y - metrics.getHeight(), metrics.stringWidth(text) + 2 * step, metrics.getHeight() * 2);

        // Desenha a string centralizada
        g.setColor(Color.BLACK);
        g.drawString(text, x, y);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
