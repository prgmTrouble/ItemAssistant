package com.prgmTrouble.item_assistant.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.prgmTrouble.item_assistant.mixin.WorldChunkAccessor;
import com.prgmTrouble.item_assistant.util.TextUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraft.command.argument.BlockStateArgumentType.blockState;
import static net.minecraft.command.argument.BlockStateArgumentType.getBlockState;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ChunkFill
{
    private static final Text DONE = TextUtil.text("done",Style.EMPTY.withColor(Formatting.GRAY));
    private static void fill(final ServerWorld world,final int fromX,final int fromZ,final int toX,final int toZ,final BlockState state)
    {
        final int fx = min(fromX,toX),tx = max(fromX,toX),
                  fz = min(fromZ,toZ),tz = max(fromZ,toZ);
        final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for(int x = fx;x <= tx;++x)
            for(int z = fz;z <= tz;++z)
            {
                final WorldChunk wc = world.getChunk(x,z);
                final ChunkSection[] s = wc.getSectionArray();
                for(byte i = 0;i < 16;++i)
                {
                    final PalettedContainer<BlockState> container = (s[i] == null? s[i] = new ChunkSection(i) : s[i]).getContainer();
                    exec.submit
                    (
                        () ->
                        {
                            for(byte x1 = 0;x1 < 16;++x1)
                                for(byte y1 = 0;y1 < 16;++y1)
                                    for(byte z1 = 0;z1 < 16;++z1)
                                        container.set(x1,y1,z1,state);
                        }
                    );
                }
                wc.setShouldSave(true);
                wc.getBlockEntities().clear();
                wc.getBlockEntityPositions().clear();
                wc.getStructureStarts().clear();
                wc.getStructureReferences().clear();
                Arrays.fill(wc.getPostProcessingLists(),null);
                final WorldChunkAccessor wca = (WorldChunkAccessor)wc;
                wca.getPendingBlockEntityNbts().clear();
                wca.getBlockEntityTickers().clear();
                
            }
        exec.shutdown();
        try
        {
            long delay = 1;
            while(!exec.awaitTermination(delay,TimeUnit.SECONDS))
                delay <<= 1;
        }
        catch(final InterruptedException e) {e.printStackTrace();}
    }
    private static int fillAir(final CommandContext<ServerCommandSource> c)
    {
        fill(c.getSource().getWorld(),getInteger(c,"fromX"),getInteger(c,"fromZ"),getInteger(c,"toX"),getInteger(c,"toZ"),Blocks.AIR.getDefaultState());
        c.getSource().sendFeedback(DONE,false);
        return 1;
    }
    private static int fill(final CommandContext<ServerCommandSource> c)
    {
        fill(c.getSource().getWorld(),getInteger(c,"fromX"),getInteger(c,"fromZ"),getInteger(c,"toX"),getInteger(c,"toZ"),getBlockState(c,"block").getBlockState());
        c.getSource().sendFeedback(DONE,false);
        return 1;
    }
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register
        (
            literal("chunkFill").
                then
                (
                    argument("fromX",integer()).
                        then
                        (
                            argument("fromZ",integer()).
                                then
                                (
                                    argument("toX",integer()).
                                        then
                                        (
                                            argument("toZ",integer()).
                                                executes(ChunkFill::fillAir).
                                                then
                                                (
                                                    argument("block",blockState()).
                                                        executes(ChunkFill::fill)
                                                )
                                        )
                                )
                        )
                )
        );
    }
}