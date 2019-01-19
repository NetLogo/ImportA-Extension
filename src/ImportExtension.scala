package org.nlogo.extension.importa

import java.io.{ ByteArrayInputStream, InputStreamReader }
import java.util.Base64
import javax.imageio.ImageIO

import org.nlogo.agent.{ ImportPatchColors, World }
import org.nlogo.api.{ Argument, Command, Context, DefaultClassManager, PrimitiveManager }
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

  private object DrawingPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val bais = new ByteArrayInputStream(asBytes(args(0)))
      context.workspace match {
        case workspace: HeadlessWorkspace => workspace.importDrawing(bais)
        case workspace: GUIWorkspace      => workspace.importDrawing(bais)
        case _                            => // No-op
      }
    }
  }

  private object PcolorsPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val image = ImageIO.read(new ByteArrayInputStream(asBytes(args(0))))
      val world = context.workspace.world.asInstanceOf[World]
      PColorsImporter(world, true, image)
    }
  }

  private object PcolorsRGBPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      val image = ImageIO.read(new ByteArrayInputStream(asBytes(args(0))))
      val world = context.workspace.world.asInstanceOf[World]
      PColorsImporter(world, false, image)
    }
  }

  private object WorldPrim extends Command {
    override def getSyntax = Syntax.commandSyntax(right = List(Syntax.StringType))
    override def perform(args: Array[Argument], context: Context): Unit = {
      context.workspace.importWorld(new InputStreamReader(new ByteArrayInputStream(args(0).getString.getBytes)))
    }
  }

  private def asBytes(arg: Argument): Array[Byte] =
    Base64.getDecoder.decode(arg.getString.split(",")(1))

}
