package com.ducks.common.image

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Приводит вырезанные Photoroom картинки товаров к единому масштабу.
 *
 * Photoroom с `crop=true` отдаёт PNG, обрезанный ровно по границам объекта, поэтому у
 * каждого товара свои размеры и своё соотношение сторон. Клиент вписывает такую картинку
 * в карточку по своим правилам — и один и тот же по смыслу товар выглядит то крупнее, то
 * мельче соседнего. Здесь объект кладётся на общий квадратный холст с общим правилом
 * масштабирования, так что дальше клиенту достаточно растянуть картинку на всю карточку.
 *
 * Съёмка строго сверху, значит объект в кадре может быть повёрнут как угодно, и правило
 * масштабирования обязано этого не замечать. Поэтому силуэт вписывается в круг
 * фиксированного диаметра вокруг своего центра масс: круг не зависит от поворота, тогда
 * как прямоугольные габариты зависят — у повёрнутого на 45° круассана bbox почти вдвое
 * больше, чем у того же круассана, снятого ровно, и подгонка по габаритам ужала бы ровный
 * кадр относительно косого.
 */
object ProductImageNormalizer {

    /** Сторона холста. 1024 хватает карточке на экране с тройной плотностью. */
    const val CANVAS_SIZE = 1024

    /**
     * Ревизия правила нормализации. Попадает в имя файла, и по ней видно, приведена
     * картинка к текущему масштабу или досталась от прошлой версии правила.
     *
     * Поменяли CANVAS_SIZE, поля или само правило вписывания — увеличьте ревизию, и на
     * ближайшем старте приложения весь каталог перезальётся сам. Ничего включать руками
     * не нужно.
     */
    const val REVISION = 1

    /**
     * Поле с каждой стороны холста: объект не липнет к краю карточки и её скруглениям.
     * Заодно это единственная ручка масштаба — больше поле, мельче все товары разом.
     */
    private const val PADDING_RATIO = 0.06

    /**
     * Полупрозрачная кайма после despill — это ореол от фона, а не объект. Считать её
     * силуэтом значит подарить каждому товару свой лишний отступ и свой масштаб.
     */
    private const val ALPHA_THRESHOLD = 12

    fun normalize(png: ByteArray): ByteArray {
        val source = ImageIO.read(ByteArrayInputStream(png))
            ?: error("Не удалось декодировать изображение товара")

        val mask = maskOf(source) ?: return png // Полностью прозрачная — делить не на что.

        val padding = CANVAS_SIZE * PADDING_RATIO
        val contentBox = CANVAS_SIZE - 2 * padding

        // Диаметр силуэта = сторона холста за вычетом полей. Всё, что дальше центра масс,
        // чем radius, у объекта отсутствует, поэтому объект гарантированно внутри полей
        // при любом повороте.
        val scale = contentBox / (2 * mask.radius)

        val targetWidth = max(1, (mask.width * scale).roundToInt())
        val targetHeight = max(1, (mask.height * scale).roundToInt())

        val scaled = scaleSmooth(
            source.getSubimage(mask.x, mask.y, mask.width, mask.height),
            targetWidth,
            targetHeight,
        )

        // Центруем по центру масс, а не по центру bbox: у кружки сверху ручка утягивает
        // bbox вбок, и по нему кружок кофе вставал бы в карточке заметно левее центра.
        // Центр масс — ещё и центр того самого круга, в который вписан силуэт.
        val offsetX = centeredOffset((mask.centerX - mask.x) * scale, targetWidth)
        val offsetY = centeredOffset((mask.centerY - mask.y) * scale, targetHeight)

        val canvas = BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        graphics.drawImage(scaled, offsetX, offsetY, null)
        graphics.dispose()

        return ByteArrayOutputStream().use { out ->
            ImageIO.write(canvas, "png", out)
            out.toByteArray()
        }
    }

    private class Mask(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val centerX: Double,
        val centerY: Double,
        /** Расстояние от центра масс до самой дальней точки силуэта. */
        val radius: Double,
    )

    private fun maskOf(image: BufferedImage): Mask? {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var area = 0L
        var sumX = 0L
        var sumY = 0L

        val row = IntArray(image.width)

        for (y in 0 until image.height) {
            image.getRGB(0, y, image.width, 1, row, 0, image.width)

            for (x in row.indices) {
                if ((row[x] ushr 24) <= ALPHA_THRESHOLD) continue

                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                area++
                sumX += x
                sumY += y
            }
        }

        if (area == 0L) return null

        val centerX = sumX.toDouble() / area
        val centerY = sumY.toDouble() / area

        return Mask(
            x = minX,
            y = minY,
            width = maxX - minX + 1,
            height = maxY - minY + 1,
            centerX = centerX,
            centerY = centerY,
            // Радиус ищем вторым проходом: без готового центра масс его не посчитать.
            // Проход по пикселям стоит единицы миллисекунд даже на 4 МП.
            radius = max(1.0, radiusOf(image, centerX, centerY)),
        )
    }

    private fun radiusOf(image: BufferedImage, centerX: Double, centerY: Double): Double {
        var maxSquared = 0.0
        val row = IntArray(image.width)

        for (y in 0 until image.height) {
            image.getRGB(0, y, image.width, 1, row, 0, image.width)
            val dy = y - centerY

            for (x in row.indices) {
                if ((row[x] ushr 24) <= ALPHA_THRESHOLD) continue

                val dx = x - centerX
                val squared = dx * dx + dy * dy
                if (squared > maxSquared) maxSquared = squared
            }
        }

        return sqrt(maxSquared)
    }

    private fun centeredOffset(center: Double, size: Int): Int {
        val offset = (CANVAS_SIZE / 2.0 - center).roundToInt()
        // Масштаб уже держит объект внутри полей; это страховка от округления вплотную
        // к краю, чтобы drawImage ничего не обрезал.
        return offset.coerceIn(0, max(0, CANVAS_SIZE - size))
    }

    /**
     * Уменьшение в несколько раз за один drawImage даёт рваный край у вырезанного объекта,
     * поэтому сначала ужимаем половинками, а к цели подходим последним шагом.
     */
    private fun scaleSmooth(source: BufferedImage, width: Int, height: Int): BufferedImage {
        var current = source

        while (current.width / 2 > width && current.height / 2 > height) {
            current = redraw(current, current.width / 2, current.height / 2)
        }

        return redraw(current, width, height)
    }

    private fun redraw(source: BufferedImage, width: Int, height: Int): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(source, 0, 0, width, height, null)
        graphics.dispose()
        return result
    }
}
