package com.ducks.common.image

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductImageNormalizerTest {

    @Test
    fun `холст всегда одного размера`() {
        val result = decode(ProductImageNormalizer.normalize(circle(diameter = 120, canvas = 400)))

        assertEquals(ProductImageNormalizer.CANVAS_SIZE, result.width)
        assertEquals(ProductImageNormalizer.CANVAS_SIZE, result.height)
    }

    @Test
    fun `один и тот же объект, снятый крупно и мелко, приводится к одному масштабу`() {
        val big = coverage(ProductImageNormalizer.normalize(circle(diameter = 900, canvas = 1000)))
        val small = coverage(ProductImageNormalizer.normalize(circle(diameter = 90, canvas = 1000)))

        assertTrue(
            abs(big - small) < 0.01,
            "Масштаб разъехался: крупный кадр $big, мелкий $small",
        )
    }

    @Test
    fun `поворот объекта в кадре не меняет масштаб`() {
        val straight = coverage(ProductImageNormalizer.normalize(bar(rotationDegrees = 0.0)))
        val rotated = coverage(ProductImageNormalizer.normalize(bar(rotationDegrees = 45.0)))

        assertTrue(
            abs(straight - rotated) < 0.02,
            "Поворот сбил масштаб: ровно $straight, под 45° $rotated",
        )
    }

    @Test
    fun `объект не вылезает за поля холста`() {
        val result = decode(ProductImageNormalizer.normalize(bar(rotationDegrees = 0.0)))
        val padding = (ProductImageNormalizer.CANVAS_SIZE * 0.06).toInt()

        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val opaque = (result.getRGB(x, y) ushr 24) > 12
                val insideFields = x >= padding && y >= padding &&
                        x < result.width - padding && y < result.height - padding

                assertTrue(!opaque || insideFields, "Пиксель ($x, $y) залез в поля")
            }
        }
    }

    @Test
    fun `повторный прогон ничего не меняет`() {
        val once = ProductImageNormalizer.normalize(circle(diameter = 300, canvas = 800))
        val twice = ProductImageNormalizer.normalize(once)

        assertTrue(
            abs(coverage(once) - coverage(twice)) < 0.01,
            "Второй прогон сдвинул масштаб: ${coverage(once)} против ${coverage(twice)}",
        )
    }

    private fun circle(diameter: Int, canvas: Int): ByteArray = draw(canvas) { graphics ->
        graphics.color = Color.RED
        // Со сдвигом от центра — Photoroom кропает по объекту, но исходники тестов
        // должны проверять и то, что мы сами находим силуэт.
        graphics.fill(Ellipse2D.Double(10.0, 20.0, diameter.toDouble(), diameter.toDouble()))
    }

    private fun bar(rotationDegrees: Double): ByteArray = draw(canvas = 1000) { graphics ->
        graphics.color = Color.RED
        graphics.transform = AffineTransform.getRotateInstance(
            Math.toRadians(rotationDegrees),
            500.0,
            500.0,
        )
        graphics.fill(Rectangle2D.Double(200.0, 430.0, 600.0, 140.0))
    }

    private fun draw(canvas: Int, block: (java.awt.Graphics2D) -> Unit): ByteArray {
        val image = BufferedImage(canvas, canvas, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.stroke = BasicStroke(1f)
        block(graphics)
        graphics.dispose()

        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    private fun decode(png: ByteArray): BufferedImage = ImageIO.read(ByteArrayInputStream(png))

    /** Доля холста, занятая непрозрачными пикселями. */
    private fun coverage(png: ByteArray): Double {
        val image = decode(png)
        var opaque = 0L

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if ((image.getRGB(x, y) ushr 24) > 12) opaque++
            }
        }

        return opaque.toDouble() / (image.width * image.height)
    }
}
