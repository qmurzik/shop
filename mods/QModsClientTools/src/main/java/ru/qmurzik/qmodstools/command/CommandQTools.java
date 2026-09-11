package ru.qmurzik.qmodstools.command;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import ru.qmurzik.qmodstools.gui.GuiSettings;

public final class CommandQTools extends CommandBase {
    @Override public String getCommandName(){return "qtools";}
    @Override public String getCommandUsage(ICommandSender sender){return "/qtools";}
    @Override public int getRequiredPermissionLevel(){return 0;}
    @Override public void processCommand(ICommandSender sender,String[] args){Minecraft.getMinecraft().displayGuiScreen(new GuiSettings(null));}
}
