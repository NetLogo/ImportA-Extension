package org.nlogo.extension.importa

import java.awt.image.BufferedImage
import java.io.{ ByteArrayInputStream, InputStreamReader }
import java.util.Base64
import javax.imageio.ImageIO

import org.nlogo.agent.World
import org.nlogo.api.{ Argument, Command, Context, DefaultClassManager, ExtensionException, PrimitiveManager }
import org.nlogo.core.Syntax
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.window.GUIWorkspace

class ImportExtension extends DefaultClassManager {

  override def load(manager: PrimitiveManager): Unit = {
    manager.addPrimitive("drawing"    , DrawingPrim)
    manager.addPrimitive("pcolors"    , PcolorsPrim)
    manager.addPrimitive("pcolors-rgb", PcolorsRGBPrim)
    manager.addPrimitive("world"      , WorldPrim)
  }

  private val pcMsg = "This primitive only accepts input that is base64-encoded."

  private object DrawingPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      context.workspace match {
        case workspace: HeadlessWorkspace => workspace.importDrawingBase64(args(0).getString)
        case workspace: GUIWorkspace      => workspace.importDrawingBase64(args(0).getString)
        case _                            => // No-op
      }
    }
  }

  private object PcolorsPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val image = asImage(args(0))
      val world = context.workspace.world.asInstanceOf[World]
      PColorsImporter(world, true, image)
    }
  }

  private object PcolorsRGBPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val image = asImage(args(0))
      val world = context.workspace.world.asInstanceOf[World]
      PColorsImporter(world, false, image)
    }
  }

  private object WorldPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val bais = new ByteArrayInputStream(args(0).getString.getBytes)
      context.workspace.importWorld(new InputStreamReader(bais))
    }
  }

  private def asImage(arg: Argument): BufferedImage = {
    val image = ImageIO.read(new ByteArrayInputStream(asBytes(arg)))
    if (image != null)
      image
    else
      throw new ExtensionException(pcMsg)
  }

  private def asBytes(arg: Argument): Array[Byte] = {

    val arr = arg.getString.split(",")

    val base64 =
      if (arr.length == 1) {
        arr(0)
      } else if (arr.length == 2) {
        arr(1)
      } else {
        throw new ExtensionException(pcMsg)
      }

    try Base64.getDecoder.decode(base64)
    catch {
      case _: IllegalArgumentException =>
        throw new ExtensionException(pcMsg)
    }

  }

}
