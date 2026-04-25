package utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/**
 * A classe ImageUtils fornece métodos utilitários para trabalhar com imagens.
 * Permite a conversão de imagens para e de arrays de bytes, o redimensionamento
 * de imagens e a leitura e gravação de imagens em arquivos.
 *
 * @author Catarina - Miguel
 */
public class ImageUtils {

    /**
     * Converte uma BufferedImage para um array de bytes.
     *
     * @param image A imagem a ser convertida para um array de bytes
     * @return Um array de bytes representando a imagem
     * @throws IOException Caso ocorra um erro ao escrever a imagem para o array
     * de bytes
     */
    public static byte[] imageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        return baos.toByteArray();
    }

    /**
     * Converte um array de bytes para uma BufferedImage.
     *
     * @param data O array de bytes representando a imagem
     * @return A BufferedImage gerada a partir do array de bytes
     * @throws IOException Caso ocorra um erro ao ler os bytes para uma imagem
     */
    public static BufferedImage byteArrayToImage(byte[] data) throws IOException {
        // Lê os dados do array de bytes e cria uma BufferedImage
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    /**
     * Salva uma BufferedImage em um ficheiro de imagem
     *
     * @param image A BufferedImage a ser salva
     * @param fileName O nome do ficheiro onde a imagem será salva
     * @throws IOException Caso ocorra um erro ao salvar a imagem
     */
    public static void saveImage(BufferedImage image, String fileName) throws IOException {
        ImageIO.write(image, "jpg", new File(fileName));
    }

    /**
     * Salva um array de bytes representando uma imagem em um ficheiro
     *
     * @param data O array de bytes da imagem
     * @param fileName O nome do ficheiro onde os bytes serão salvos
     * @throws IOException Caso ocorra um erro ao salvar os dados no ficheiro
     */
    public static void saveImage(byte[] data, String fileName) throws IOException {
        Files.write(Paths.get(fileName), data);
    }

    /**
     * Carrega uma imagem a partir de um ficheiro
     *
     * @param fileName O nome do ficheiro de imagem a ser carregado
     * @return A BufferedImage carregada do ficheiro
     * @throws IOException Caso ocorra um erro ao carregar a imagem
     */
    public static BufferedImage loadImage(String fileName) throws IOException {
        return ImageIO.read(new File(fileName));
    }

    /**
     * redimensiona uma imagem
     *
     * @param srcImg imagem original
     * @param w largura
     * @param h altura
     * @return imagem redimensioanda
     */
    public static Image getScaledImage(Image srcImg, int w, int h) {
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

}
