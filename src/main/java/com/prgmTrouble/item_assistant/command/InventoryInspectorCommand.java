package com.prgmTrouble.item_assistant.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.prgmTrouble.item_assistant.util.InventoryInspector.*;
import static com.prgmTrouble.item_assistant.util.TextUtil.text;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.command.argument.UuidArgumentType.getUuid;
import static net.minecraft.command.argument.UuidArgumentType.uuid;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class InventoryInspectorCommand
{
    private InventoryInspectorCommand() {}
    
    private static final Style TEXT = Style.EMPTY.withColor(Formatting.GRAY);
    private static final Text NO_ENTITIES = text("No entities found",TEXT);
    private static void printList(final Text[] out,final ServerCommandSource src)
    {
        if(out.length == 0) src.sendFeedback(NO_ENTITIES,false);
        else for(final Text t : out) src.sendFeedback(t,false);
    }
    
    private static int monitorList(final CommandContext<ServerCommandSource> c)
    {
        final ServerCommandSource src = c.getSource();
        for(final Text t : listMonitors(src.getWorld())) src.sendFeedback(t,false);
        return 1;
    }
    
    private static int monitorAddBlock(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(addMonitor(src.getWorld(),getBlockPos(c,"block")),false);
        return 1;
    }
    private static int monitorAddEntities(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        printList(addMonitors(getEntities(c,"entities")),c.getSource());
        return 1;
    }
    private static int monitorAddUUID(final CommandContext<ServerCommandSource> c)
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(addMonitor(src.getWorld(),getUuid(c,"UUID")),false);
        return 1;
    }
    
    private static int monitorRemoveBlock(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(removeMonitor(src.getWorld(),getBlockPos(c,"block")),false);
        return 1;
    }
    private static int monitorRemoveEntities(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        printList(removeMonitors(getEntities(c,"entities")),c.getSource());
        return 1;
    }
    private static int monitorRemoveUUID(final CommandContext<ServerCommandSource> c)
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(removeMonitor(src.getWorld(),getUuid(c,"UUID")),false);
        return 1;
    }
    
    private static int replayList(final CommandContext<ServerCommandSource> c)
    {
        final ServerCommandSource src = c.getSource();
        for(final Text t : listReplays(src.getWorld())) src.sendFeedback(t,false);
        return 1;
    }
    
    private static int replayShowBlock(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(showReplay(src.getWorld(),getBlockPos(c,"block")),false);
        return 1;
    }
    private static int replayShowEntities(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        printList(showReplays(getEntities(c,"entities")),c.getSource());
        return 1;
    }
    private static int replayShowUUID(final CommandContext<ServerCommandSource> c)
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(showReplay(src.getWorld(),getUuid(c,"UUID")),false);
        return 1;
    }
    
    private static int replayStartBlock(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(startReplay(src.getWorld(),getBlockPos(c,"block"),getBlockPos(c,"display")),false);
        return 1;
    }
    private static int replayStartEntity(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        c.getSource().sendFeedback(startReplay(getEntity(c,"entity"),getBlockPos(c,"display")),false);
        return 1;
    }
    private static int replayStartUUID(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(startReplay(src.getWorld(),getUuid(c,"UUID"),getBlockPos(c,"display")),false);
        return 1;
    }
    
    private static int replayStopBlock(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(stopReplay(src.getWorld(),getBlockPos(c,"block")),false);
        return 1;
    }
    private static int replayStopEntities(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        printList(stopReplays(getEntities(c,"entities")),c.getSource());
        return 1;
    }
    private static int replayStopUUID(final CommandContext<ServerCommandSource> c)
    {
        final ServerCommandSource src = c.getSource();
        src.sendFeedback(stopReplay(src.getWorld(),getUuid(c,"UUID")),false);
        return 1;
    }
    
    private static int eventNext(final CommandContext<ServerCommandSource> c)
    {
        c.getSource().sendFeedback(nextEvent(c.getSource().getWorld()),false);
        return 1;
    }
    private static int eventPrev(final CommandContext<ServerCommandSource> c)
    {
        c.getSource().sendFeedback(previousEvent(c.getSource().getWorld()),false);
        return 1;
    }
    
    private static int tickNext(final CommandContext<ServerCommandSource> c)
    {
        for(final Text t : nextTick(c.getSource().getWorld()))
            c.getSource().sendFeedback(t,false);
        return 1;
    }
    private static int tickPrev(final CommandContext<ServerCommandSource> c)
    {
        for(final Text t : prevTick(c.getSource().getWorld()))
            c.getSource().sendFeedback(t,false);
        return 1;
    }
    
    private static int tickSet(final CommandContext<ServerCommandSource> c)
    {
        c.getSource().sendFeedback(setTick(c.getSource().getWorld(),getInteger(c,"tick"),(short)0),false);
        return 1;
    }
    private static int tickSetOrder(final CommandContext<ServerCommandSource> c)
    {
        c.getSource().sendFeedback(setTick(c.getSource().getWorld(),getInteger(c,"tick"),(short)getInteger(c,"event")),false);
        return 1;
    }
    
    private static int resetAll(final CommandContext<ServerCommandSource> c)
    {
        c.getSource().sendFeedback(reset(),false);
        return 1;
    }
    
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register
        (
            literal("inventoryInspector").
                then
                (
                    literal("monitor").
                        executes(InventoryInspectorCommand::monitorList).
                        then
                        (
                            argument("block",blockPos()).
                                executes(InventoryInspectorCommand::monitorAddBlock).
                                then
                                (
                                    literal("remove").
                                        executes(InventoryInspectorCommand::monitorRemoveBlock)
                                )
                        ).
                        then
                        (
                            argument("entities",entities()).
                                executes(InventoryInspectorCommand::monitorAddEntities).
                                then
                                (
                                    literal("remove").
                                        executes(InventoryInspectorCommand::monitorRemoveEntities)
                                )
                        ).
                        then
                        (
                            argument("UUID",uuid()).
                                executes(InventoryInspectorCommand::monitorAddUUID).
                                then
                                (
                                    literal("remove").
                                        executes(InventoryInspectorCommand::monitorRemoveUUID)
                                )
                        )
                ).
                then
                (
                    literal("replay").
                        executes(InventoryInspectorCommand::replayList).
                        then
                        (
                            argument("block",blockPos()).
                                executes(InventoryInspectorCommand::replayShowBlock).
                                then
                                (
                                    argument("display",blockPos()).
                                        executes(InventoryInspectorCommand::replayStartBlock)
                                ).
                                then
                                (
                                    literal("remove").
                                        executes(InventoryInspectorCommand::replayStopBlock)
                                )
                        ).
                        then
                        (
                            argument("entity",entity()).
                                then
                                (
                                    argument("display",blockPos()).
                                        executes(InventoryInspectorCommand::replayStartEntity)
                                )
                        ).
                        then
                        (
                            argument("entities",entities()).
                                executes(InventoryInspectorCommand::replayShowEntities).
                                then
                                (
                                    literal("remove").
                                        executes(InventoryInspectorCommand::replayStopEntities)
                                )
                        ).
                        then
                        (
                            argument("UUID",uuid()).
                                executes(InventoryInspectorCommand::replayShowUUID).
                                then
                                (
                                    argument("display",blockPos()).
                                        executes(InventoryInspectorCommand::replayStartUUID)
                                ).
                                then
                                (
                                    literal("remove").
                                        executes(InventoryInspectorCommand::replayStopUUID)
                                )
                        )
                ).
                then
                (
                    literal("next").
                        then
                        (
                            literal("event").
                                executes(InventoryInspectorCommand::eventNext)
                        ).
                        then
                        (
                            literal("tick").
                                executes(InventoryInspectorCommand::tickNext)
                        )
                ).
                then
                (
                    literal("previous").
                        then
                        (
                            literal("event").
                                executes(InventoryInspectorCommand::eventPrev)
                        ).
                        then
                        (
                            literal("tick").
                                executes(InventoryInspectorCommand::tickPrev)
                        )
                ).
                then
                (
                    literal("goto").
                        then
                        (
                            argument("tick",integer(0)).
                                executes(InventoryInspectorCommand::tickSet).
                                then
                                (
                                    argument("event",integer(0,(1<<16)-1)).
                                        executes(InventoryInspectorCommand::tickSetOrder)
                                )
                        )
                ).
                then
                (
                    literal("reset").
                        executes(InventoryInspectorCommand::resetAll)
                )
        );
    }
}