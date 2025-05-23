package org.nlogo.extension.importa

import java.awt.Color
import java.awt.image.BufferedImage

import scala.collection.mutable.{ Map => MMap }

import org.nlogo.agent.World
import org.nlogo.api.{ Color => NLColor }

object PColorsImporter {

  private case class Update(color: Color, x: Int, y: Int)

  private val colorCache: MMap[Color, Double] = MMap()

  def apply(world: World, isNetLogoColorspace: Boolean, image: BufferedImage): Unit = {

    val height = image.getHeight
    val width  = image.getWidth
    val pixels = for (y <- 0 until height; x <- 0 until width) yield new Color(image.getRGB(x, y), true)

    val updates = genUpdates(world, pixels, width, height)

    val updateColor: (Update) => Unit =
      if (isNetLogoColorspace)
        {
         case Update(color, x, y) =>
           world.fastGetPatchAt(x, y).pcolor(Double.box(lookupColor(color)))
        }
      else
        {
         case Update(color, x, y) =>
           world.fastGetPatchAt(x, y).pcolor(NLColor.getRGBListByARGB(color.getRGB))
        }

    updates.foreach(updateColor)

  }

  private def lookupColor(color: Color): Double =
    colorCache.getOrElse(color, {
      val out = NLColor.getClosestColorNumberByARGB(color.getRGB)
      colorCache += color -> out
      out
    })

  private def genUpdates(world: World, pixels: Seq[Color], imageWidth: Int, imageHeight: Int): Seq[Update] = {

    import world.{ minPxcor, minPycor, patchSize, worldWidth, worldHeight }

    def genCoords(patchSize: Double, ratio: Double, worldDim: Int, imageDim: Int): (Int, Int, Map[Int, Int]) = {

      val worldPixelDim  = patchSize * worldDim
      val scaledImageDim = imageDim * ratio

      // Even with Jason's floating point voodoo for the ratios, we can still end up with
      // a scaled image dimenstion slightly larger than the world dimension.  This is
      // incorrect, so we just limit it at 0.  -Jeremy B April 2025
      val patchOffset = StrictMath.max(0, worldPixelDim  - scaledImageDim) / patchSize / 2
      val startPatch  = StrictMath.floor(patchOffset).toInt
      val endPatch    = worldDim - StrictMath.ceil(patchOffset).toInt
      val dimRatio    = imageDim.toDouble / (endPatch - startPatch)

      val startPixels =
        (startPatch until endPatch).map {
          patchNum => patchNum -> StrictMath.floor((patchNum - startPatch) * dimRatio).toInt
        }.toMap

      (startPatch, endPatch, startPixels)

    }

    // Sorry, but I'm probably going to Hell for this.
    // Patch size values with a large number of significant digits could
    // trigger IEEE floating point chicanery here.  My solution is to pump
    // the numbers up, to give us more leeway before the IEEE floating point
    // assassins emerge from the shadows and stab us in the back.
    // --Jason B. (5/9/22)
    val xRatio = patchSize * 1e10 * worldWidth  / imageWidth  / 1e10
    val yRatio = patchSize * 1e10 * worldHeight / imageHeight / 1e10
    val ratio  = StrictMath.min(xRatio, yRatio)

    val (xStart, xEnd, xStarts) = genCoords(patchSize, ratio, worldWidth , imageWidth )
    val (yStart, yEnd, yStarts) = genCoords(patchSize, ratio, worldHeight, imageHeight)

    val updates =
      for (xcor <- xStart until xEnd; ycor <- yStart until yEnd) yield {

        val minX = xStarts(xcor)
        val minY = yStarts(ycor)
        val maxX = StrictMath.max(minX + 1, xStarts.getOrElse(xcor + 1, imageWidth ))
        val maxY = StrictMath.max(minY + 1, yStarts.getOrElse(ycor + 1, imageHeight))

        // This here is essentially the whole reason I'm not using the standard NetLogo
        // code for `import-pcolors`.  Instead, I just use the color of the center pixel
        // of the bitmap section.  Averaging the colors in the section is tempting, but
        // just tends to lead to lots of grays.  Normal `import-pcolors` does all this
        // silly jank with scaling the image down, using AWT.  How does AWT decide which
        // color to use when several pixels are collapsed into one?  Who knows.  I haven't
        // managed to figure out the logic.  It doesn't seem to be averaging nor picking
        // the center pixel.  Whatever. --JAB (11/19/18)
        val x     = StrictMath.floor((minX + maxX) / 2).toInt
        val y     = StrictMath.floor((minY + maxY) / 2).toInt
        val pixel = pixels(x + (y * imageWidth))

        Update(pixel, minPxcor + xcor, minPycor + ((worldHeight - 1) - ycor))

      }

    updates.filter { case Update(p, x, y) => p.getAlpha != 0 }

  }

}
