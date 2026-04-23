package com.inventario_hardware.inventario.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.awt.image.BufferedImage;

/**
 * Utilidad para generar códigos QR como {@link BufferedImage} usando ZXing.
 * <p>
 * Uso típico:
 * <pre>
 *   BufferedImage img = QRGenerator.generateQRImage("{\"id\":1}", 250, 250);
 * </pre>
 */
public class QRGenerator {

    /**
     * Genera un QR en memoria a partir de un texto.
     *
     * @param text   contenido a codificar (JSON, URL, etc.)
     * @param width  ancho en píxeles (recomendado ≥ 200)
     * @param height alto en píxeles (recomendado ≥ 200)
     * @return imagen del QR como {@link BufferedImage}
     * @throws Exception si falla la codificación
     */
    public static BufferedImage generateQRImage(String text, int width, int height) throws Exception {
        // Codifica el texto a un BitMatrix (matriz de puntos blancos/negros)
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);

        // Convierte la matriz a imagen PNG en memoria
        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
