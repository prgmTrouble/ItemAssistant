package com.prgmTrouble.item_assistant.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.*;

import static com.prgmTrouble.item_assistant.util.TextUtil.*;

public final class ItemEntityDetector
{
    private ItemEntityDetector() {}
    private static final Style TEXT = Style.EMPTY.withColor(Formatting.GRAY),
                               VAL = Style.EMPTY.withColor(Formatting.GOLD);
    private static final Text COMMA = text(',',TEXT);
    
    private static final record list(Set<Text> whitelist,Set<Text> blacklist)
    {
        list() {this(new HashSet<>(),new HashSet<>());}
    }
    private static final Map<BlockPos,list> checkpoints = new HashMap<>();
    private static MutableText itemList(final String label,final Collection<Text> s)
    {
        final MutableText t = text(label,TEXT);
        boolean first = false;
        for(final Text i : s.stream().sorted(Comparator.comparing(Text::asString)).toArray(Text[]::new))
        {
            if(first) t.append(COMMA);
            else first = true;
            t.append(i);
        }
        return t;
    }
    private static Text hover(final list l)
    {
        return itemList("whitelist: ",l.whitelist).append(itemList("\nblacklist: ",l.blacklist));
    }
    private static MutableText fmt(final BlockPos k,final list v)
    {
        final Text h = hover(v);
        return hoverClick(text(k,TEXT,VAL),h,h.getString());
    }
    private static MutableText fmt(final Map.Entry<BlockPos,list> e) {return fmt(e.getKey(),e.getValue());}
    
    public static Text addCheckpoint(final BlockPos p)
    {
        return hoverClick
        (
            text("Checkpoint ",TEXT).
            append(text(p,TEXT,VAL)).
            append(text((checkpoints.putIfAbsent(p,new list()) == null? "":" was already")+" added",TEXT)),
            text("Click to remove",TEXT),
            "/itemEntityCheckpoint remove "+p.getX()+" "+p.getY()+" "+p.getZ()
        );
    }
    public static Text removeCheckpoint(final BlockPos p)
    {
        return hoverClick
        (
            text("Checkpoint ",TEXT).
            append(text(p,TEXT,VAL)).
            append(text(checkpoints.remove(p) != null? " removed":" does not exist",TEXT)),
            text("Click to add",TEXT),
            "/itemEntityCheckpoint add "+p.getX()+" "+p.getY()+" "+p.getZ()
        );
    }
    public static Text removeCheckpointVolume(final BlockPos a,final BlockPos b)
    {
        final int fx = Math.min(a.getX(),b.getX()),fy = Math.min(a.getY(),b.getY()),fz = Math.min(a.getZ(),b.getZ()),
                  tx = Math.max(a.getX(),b.getX()),ty = Math.max(a.getY(),b.getY()),tz = Math.max(a.getZ(),b.getZ());
        final Map<BlockPos,list> l = new TreeMap<>();
        if
        (
            checkpoints.entrySet().removeIf(e ->
            {
                final BlockPos k = e.getKey();
                if(tx < k.getX() || k.getX() < fx ||
                   ty < k.getY() || k.getY() < fy ||
                   tz < k.getZ() || k.getZ() < fz)
                {
                    l.put(k,e.getValue());
                    return true;
                }
                return false;
            })
        )
        {
            final MutableText t = text("Removed checkpoints: ",TEXT);
            boolean flag = false;
            for(final Map.Entry<BlockPos,list> e : l.entrySet())
            {
                if(flag) t.append(COMMA);
                else flag = true;
                t.append(fmt(e));
            }
            return t;
        }
        return text("No checkpoints removed",TEXT);
    }
    public static Text clear()
    {
        checkpoints.clear();
        return text("Cleared all checkpoints",TEXT);
    }
    private static Text add(final boolean whitelist,final BlockPos pos,final Text item)
    {
        list l = checkpoints.get(pos);
        if(l == null) checkpoints.put(pos,l = new list());
        return
            text("Item ",TEXT).
            append(item).
            append
            (
                " was "+((whitelist? l.whitelist:l.blacklist).add(item)? "":"already ")+
                "added to "+(whitelist? "white":"black")+"list "
            ).
            append(fmt(pos,l));
    }
    public static Text whitelist(final BlockPos pos,final Text item) {return add( true,pos,item);}
    public static Text blacklist(final BlockPos pos,final Text item) {return add(false,pos,item);}
    private static Text addAll(final boolean whitelist,final BlockPos pos,final Collection<Text> items)
    {
        list l = checkpoints.get(pos);
        if(l == null) checkpoints.put(pos,l = new list());
        final Set<Text> s = whitelist? l.whitelist : l.blacklist;
        s.addAll(items);
        return
            text(items.size()+" item"+(items.size() == 1?"":"s")+" added to "+(whitelist?"white":"black")+"list ",TEXT).
            append(fmt(pos,l));
    }
    public static Text whitelistAll(final BlockPos pos,final Collection<Text> items) {return addAll(true,pos,items);}
    public static Text blacklistAll(final BlockPos pos,final Collection<Text> items) {return addAll(false,pos,items);}
    
    public static void testCheckpoint(final ServerWorld world,final BlockPos p,final Text item)
    {
        final list l = checkpoints.get(p);
        if(l != null)
        {
            final int x = p.getX(),y = p.getY(),z = p.getZ();
            if(!l.whitelist.isEmpty() && !l.whitelist.contains(item))
                broadcast
                (
                    world,
                    hoverClick
                    (
                        text("Un-whitelisted detected: ",TEXT).
                        append(item).
                        append(text(" at ",TEXT)).
                        append(text(p,TEXT,VAL)),
                        text("Click for teleport command",TEXT),
                        "/tp @s "+x+" "+y+" "+z
                    )
                );
            if(!l.blacklist.isEmpty() && l.blacklist.contains(item))
                broadcast
                (
                    world,
                    hoverClick
                    (
                        text("Blacklisted item detected: ",TEXT).
                        append(item).
                        append(text(" at ",TEXT)).
                        append(text(p,TEXT,VAL)),
                        text("Click for teleport command",TEXT),
                        "/tp @s "+x+" "+y+" "+z
                    )
                );
        }
    }
    public static Text[] printCheckpoints()
    {
        return
            checkpoints.entrySet().stream().
                                   sorted().
                                   parallel().
                                   map(ItemEntityDetector::fmt).
                                   toArray(Text[]::new);
    }
}