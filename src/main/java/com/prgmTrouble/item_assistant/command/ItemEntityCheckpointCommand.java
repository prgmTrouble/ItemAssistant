package com.prgmTrouble.item_assistant.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.prgmTrouble.item_assistant.util.ItemEntityDetector;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.prgmTrouble.item_assistant.util.ItemEntityDetector.*;
import static com.prgmTrouble.item_assistant.util.TextUtil.send;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.command.argument.TextArgumentType.getTextArgument;
import static net.minecraft.command.argument.TextArgumentType.text;
import static net.minecraft.item.ItemStack.EMPTY;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ItemEntityCheckpointCommand
{
    private ItemEntityCheckpointCommand() {}

    private static int printCheckpoints(final CommandContext<ServerCommandSource> c)
    {
        send(c.getSource(),ItemEntityDetector.printCheckpoints());
        return 1;
    }
    private static int addCheckpoint(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send(c.getSource(),ItemEntityDetector.addCheckpoint(getBlockPos(c,"pos")));
        return 1;
    }
    private static int removeCheckpoint(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send(c.getSource(),ItemEntityDetector.removeCheckpoint(getBlockPos(c,"pos")));
        return 1;
    }
    private static int removeVolume(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send(c.getSource(),removeCheckpointVolume(getBlockPos(c,"from"),getBlockPos(c,"to")));
        return 1;
    }
    private static int clearCheckpoints(final CommandContext<ServerCommandSource> c)
    {
        send(c.getSource(),clear());
        return 1;
    }
    private static Collection<Text> container(final BlockPos p,final World w)
    {
        if(w.getBlockEntity(p) instanceof final Inventory c && !c.isEmpty())
        {
            final Set<Text> items = new HashSet<>(c.size());
            for(int i = 0;i < c.size();++i)
            {
                final ItemStack is = c.getStack(i);
                if(is != EMPTY) items.add(is.getName());
            }
            return items;
        }
        return null;
    }
    private static int whitelistInPlace(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final BlockPos p = getBlockPos(c,"checkpoint");
        send(c.getSource(),whitelistAll(p,container(p,c.getSource().getWorld())));
        return 1;
    }
    private static int whitelistContainer(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send
        (
            c.getSource(),
            whitelistAll
            (
                getBlockPos(c,"checkpoint"),
                container(getBlockPos(c,"container"),c.getSource().getWorld())
            )
        );
        return 1;
    }
    private static int whitelistItem(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send(c.getSource(),whitelist(getBlockPos(c,"checkpoint"),getTextArgument(c,"item")));
        return 1;
    }
    private static int blacklistInPlace(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final BlockPos p = getBlockPos(c,"checkpoint");
        send(c.getSource(),blacklistAll(p,container(p,c.getSource().getWorld())));
        return 1;
    }
    private static int blacklistContainer(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send
        (
            c.getSource(),
            blacklistAll
            (
                getBlockPos(c,"checkpoint"),
                container(getBlockPos(c,"container"),c.getSource().getWorld())
            )
        );
        return 1;
    }
    private static int blacklistItem(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        send(c.getSource(),blacklist(getBlockPos(c,"checkpoint"),getTextArgument(c,"item")));
        return 1;
    }
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register
        (
            literal("itemEntityCheckpoint").
                executes(ItemEntityCheckpointCommand::printCheckpoints).
                then
                (
                    literal("add").
                        then
                        (
                            argument("pos",blockPos()).
                                executes(ItemEntityCheckpointCommand::addCheckpoint)
                        )
                ).
                then
                (
                    literal("remove").
                        then
                        (
                            argument("pos",blockPos()).
                                executes(ItemEntityCheckpointCommand::removeCheckpoint)
                        ).
                        then
                        (
                            argument("from",blockPos()).
                                then
                                (
                                    argument("to",blockPos()).
                                        executes(ItemEntityCheckpointCommand::removeVolume)
                                )
                        )
                ).
                then
                (
                    literal("clear").
                        executes(ItemEntityCheckpointCommand::clearCheckpoints)
                ).
                then
                (
                    argument("checkpoint",blockPos()).
                        then
                        (
                            literal("whitelist").
                                executes(ItemEntityCheckpointCommand::whitelistInPlace).
                                then
                                (
                                    argument("container",blockPos()).
                                        executes(ItemEntityCheckpointCommand::whitelistContainer)
                                ).
                                then
                                (
                                    argument("item",text()).
                                        executes(ItemEntityCheckpointCommand::whitelistItem)
                                )
                        ).
                        then
                        (
                            literal("blacklist").
                                executes(ItemEntityCheckpointCommand::blacklistInPlace).
                                then
                                (
                                    argument("container",blockPos()).
                                        executes(ItemEntityCheckpointCommand::blacklistContainer)
                                ).
                                then
                                (
                                    argument("item",text()).
                                        executes(ItemEntityCheckpointCommand::blacklistItem)
                                )
                        )
                )
        );
    }
}