package com.prgmTrouble.item_assistant.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;

import static com.prgmTrouble.item_assistant.util.TextUtil.text;
import static net.minecraft.command.argument.BlockPosArgumentType.blockPos;
import static net.minecraft.command.argument.BlockPosArgumentType.getBlockPos;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class NameCleanseCommand
{
    private static final Style TEXT = Style.EMPTY.withColor(Formatting.GRAY),
                               NUM  = Style.EMPTY.withColor(Formatting.GOLD);
    private static Text res(final int count) {return text("Cleansed ",TEXT).append(text(count,NUM)).append(text(" items",TEXT));}
    private static int cleanse(final Iterator<ItemStack> items)
    {
        int count = 0;
        while(items.hasNext())
        {
            final ItemStack is = items.next();
            if(!is.isEmpty())
            {
                if(is.hasCustomName())
                {
                    ++count;
                    is.setCustomName(null);
                }
                NbtCompound nbt = is.getNbt();
                if(nbt != null && nbt.contains("BlockEntityTag",10) && (nbt = nbt.getCompound("BlockEntityTag")).contains("Items",9))
                {
                    final Map<Byte,ItemStack> m;
                    {
                        final NbtList inv = nbt.getList("Items",10);
                        m = new HashMap<>(inv.size());
                        for(final NbtElement e : inv)
                        {
                            final NbtCompound c = (NbtCompound)e;
                            final ItemStack i = ItemStack.fromNbt(c);
                            if(!i.isEmpty()) m.put(c.getByte("Slot"),i);
                        }
                    }
                    count += cleanse(m.values().iterator());
                    final NbtList inv = new NbtList();
                    for(final Entry<Byte,ItemStack> e : m.entrySet())
                        inv.add(e.getKey(),e.getValue().writeNbt(new NbtCompound()));
                    nbt.put("Items",inv);
                }
            }
        }
        return count;
    }
    private static Iterator<ItemStack> fromInventory(final Inventory inv)
    {
        return new Iterator<>()
        {
            int slot = 0;
            @Override public boolean hasNext() {return slot != inv.size();}
            @Override public ItemStack next() {return inv.getStack(slot++);}
        };
    }
    private static int cleansePlayer(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        final Inventory inv;
        {
            final Entity e = src.getEntityOrThrow();
            if(!(e instanceof final Inventory i))
                throw new SimpleCommandExceptionType(text("Entity does not have a supported inventory",TEXT)).create();
            inv = i;
        }
        src.sendFeedback(res(cleanse(fromInventory(inv))),false);
        return 1;
    }
    private static int cleanseBlock(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final ServerCommandSource src = c.getSource();
        if(!(src.getWorld().getBlockEntity(getBlockPos(c,"block")) instanceof final Inventory i))
            throw new SimpleCommandExceptionType(text("Specified block does not have an inventory",TEXT)).create();
        src.sendFeedback(res(cleanse(fromInventory(i))),false);
        return 1;
    }
    private static int cleanseBlocks(final CommandContext<ServerCommandSource> c) throws CommandSyntaxException
    {
        final int minX,minY,minZ,maxX,maxY,maxZ;
        {
            final BlockPos f = getBlockPos(c,"from"),t = getBlockPos(c,"to");
            minX = Math.min(f.getX(),t.getX());
            minY = Math.min(f.getY(),t.getY());
            minZ = Math.min(f.getZ(),t.getZ());
            maxX = Math.max(f.getX(),t.getX());
            maxY = Math.max(f.getY(),t.getY());
            maxZ = Math.max(f.getZ(),t.getZ());
        }
        final ServerCommandSource src = c.getSource();
        final ServerWorld w = src.getWorld();
        final Iterator<ItemStack> items;
        {
            final List<Iterator<ItemStack>> list = new ArrayList<>();
            for(int x = minX;x < maxX;++x)
                for(int y = minY;y < maxY;++y)
                    for(int z = minZ;z < maxZ;++z)
                        if(w.getBlockEntity(new BlockPos(x,y,z)) instanceof final Inventory i && !i.isEmpty())
                            list.add(fromInventory(i));
            if(list.isEmpty())
            {
                src.sendFeedback(res(0),false);
                return 1;
            }
            items = new Iterator<>()
            {
                final Iterator<Iterator<ItemStack>> itr = list.iterator();
                Iterator<ItemStack> current = itr.next();
                @Override public boolean hasNext() {return itr.hasNext() || current.hasNext();}
                @Override public ItemStack next() {return (current.hasNext()? current : (current = itr.next())).next();}
            };
        }
        src.sendFeedback(res(cleanse(items)),false);
        return 1;
    }
    public static void register(final CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register
        (
            literal("nameCleanse").
                executes(NameCleanseCommand::cleansePlayer).
                then
                (
                    argument("block",blockPos()).
                        executes(NameCleanseCommand::cleanseBlock)
                ).
                then
                (
                    argument("from",blockPos()).
                        then
                        (
                            argument("to",blockPos()).
                                executes(NameCleanseCommand::cleanseBlocks)
                        )
                )
        );
    }
}