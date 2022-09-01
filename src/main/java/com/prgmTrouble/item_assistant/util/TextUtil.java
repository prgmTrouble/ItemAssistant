package com.prgmTrouble.item_assistant.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.math.BlockPos;

public final class TextUtil
{
    private TextUtil() {}
    
    public static MutableText text(final String str,final Style style)
    {
        final MutableText txt = new LiteralText(str);
        txt.setStyle(style);
        return txt;
    }
    public static MutableText text(final char chr,final Style style) {return text(Character.toString(chr),style);}
    public static MutableText text(final int i,final Style style) {return text(Integer.toString(i),style);}
    public static MutableText text(final double d,final Style style) {return text(Double.toString(d),style);}
    public static MutableText text(final int[] vec,final Style text,final Style num)
    {
        final MutableText t = text('[',text);
        boolean first = true;
        for(final int i : vec)
        {
            if(first) first = false;
            else t.append(text(',',text));
            t.append(text(i,num));
        }
        return t.append(text(']',text));
    }
    public static MutableText text(final BlockPos p,final Style text,final Style num)
    {
        return
            text('[',text).
            append(text(p.getX(),num)).
            append(text(',',text)).
            append(text(p.getY(),num)).
            append(text(',',text)).
            append(text(p.getZ(),num)).
            append(text(']',text));
    }
    public static MutableText text(final double[] vec,final Style text,final Style num)
    {
        final MutableText t = text('[',text);
        boolean first = true;
        for(final double i : vec)
        {
            if(first) first = false;
            else t.append(text(',',text));
            t.append(text(i,num));
        }
        return t.append(text(']',text));
    }
    public static HoverEvent hover(final Text hover) {return new HoverEvent(HoverEvent.Action.SHOW_TEXT,hover);}
    public static ClickEvent click(final String str) {return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,str);}
    public static MutableText hoverClick(final MutableText txt,final Text hover,final String click)
    {
        return txt.fillStyle(Style.EMPTY.withHoverEvent(hover(hover)).withClickEvent(click(click)));
    }
    public static void send(final ServerCommandSource s,final Text...msg)
    {
        for(final Text t : msg) s.sendFeedback(t,false);
    }
    public static void broadcast(final MinecraftServer s,final Text...msg)
    {
        for(final ServerPlayerEntity p : s.getPlayerManager().getPlayerList())
            for(final Text t : msg)
                p.sendMessage(t,false);
    }
}