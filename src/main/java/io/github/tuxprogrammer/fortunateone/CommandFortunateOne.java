package io.github.tuxprogrammer.fortunateone;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

public class CommandFortunateOne extends CommandBase {

    @Override
    public String getCommandName() {
        return "fortunateone";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fortunateone reload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        try {
            FortunateOneConfig.reloadFromDisk();
        } catch (RuntimeException e) {
            FortunateOneMod.LOG.error("[Fortunate One] Failed to reload config from disk.", e);
            throw new CommandException("Failed to reload Fortunate One config: %s", e.getMessage());
        }

        sender.addChatMessage(
            new ChatComponentText(
                "[Fortunate One] Config reloaded from disk. Worldgen changes apply to newly generated chunks only."));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "reload");
        }
        return null;
    }
}
