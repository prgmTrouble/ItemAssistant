package com.prgmTrouble.item_assistant.util;

import com.prgmTrouble.item_assistant.mixin.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.text.NbtText.BlockNbtText;
import net.minecraft.text.NbtText.EntityNbtText;
import net.minecraft.text.NbtText.StorageNbtText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

import static com.prgmTrouble.item_assistant.util.TextUtil.hoverClick;
import static com.prgmTrouble.item_assistant.util.TextUtil.text;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.minecraft.nbt.NbtElement.*;

public final class InventoryInspector
{
    private InventoryInspector() {}
    
    public static MinecraftServer SERVER;
    
    private static final Charset CHARSET = UTF_8;
    private static final File  FILE_DIR = new File("./inventory_inspector/"),
                              TEXT_FILE = new File("./inventory_inspector/text.tmp"),
                               NBT_FILE = new File("./inventory_inspector/nbt.tmp"),
                              ITEM_FILE = new File("./inventory_inspector/items.tmp"),
                              MODS_FILE = new File("./inventory_inspector/modifications.tmp");
    static
    {
        try
        {
            if(FILE_DIR.exists())
            {
                final File[] contents = FILE_DIR.listFiles();
                if(contents != null)
                    for(final File f : contents)
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
            }
            else if(!FILE_DIR.mkdirs())
                throw new UncheckedIOException(new IOException("Could not create required files for inventory inspector"));
            //noinspection ResultOfMethodCallIgnored
            TEXT_FILE.createNewFile();
            TEXT_FILE.deleteOnExit();
            //noinspection ResultOfMethodCallIgnored
            NBT_FILE.createNewFile();
            NBT_FILE.deleteOnExit();
            //noinspection ResultOfMethodCallIgnored
            ITEM_FILE.createNewFile();
            ITEM_FILE.deleteOnExit();
            //noinspection ResultOfMethodCallIgnored
            MODS_FILE.createNewFile();
            MODS_FILE.deleteOnExit();
        }
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    private static final class Input
    {
        private final RandomAccessFile r;
        private final FileInputStream fin;
        private DataInputStream i;
        long offset = 0;
        Input(final File f) throws IOException
        {
            i = new DataInputStream
            (
                new BufferedInputStream
                (
                    fin = new FileInputStream
                    (
                        (r = new RandomAccessFile(f,"r")).getFD()
                    )
                )
            );
        }
        boolean readBoolean() throws IOException
        {
            ++offset;
            return i.readBoolean();
        }
        byte readByte() throws IOException
        {
            ++offset;
            return i.readByte();
        }
        short readShort() throws IOException
        {
            offset += 2L;
            return i.readShort();
        }
        int readInt() throws IOException
        {
            offset += 4L;
            return i.readInt();
        }
        long readLong() throws IOException
        {
            offset += 8L;
            return i.readLong();
        }
        float readFloat() throws IOException
        {
            offset += 4L;
            return i.readFloat();
        }
        double readDouble() throws IOException
        {
            offset += 8L;
            return i.readDouble();
        }
        byte[] readNBytes(final int n) throws IOException
        {
            offset += n;
            return i.readNBytes(n);
        }
        void seek(final long address) throws IOException
        {
            offset = address;
            r.seek(address);
            i = new DataInputStream(new BufferedInputStream(fin));
        }
        void close() throws IOException {r.close();}
    }
    private static final class Output
    {
        //TODO implement moving window
        private final RandomAccessFile r;
        private final DataOutputStream o;
        long offset = 0;
        Output(final File f) throws IOException
        {
            o = new DataOutputStream
            (
                new BufferedOutputStream
                (
                    new FileOutputStream
                    (
                        (r = new RandomAccessFile(f,"rw")).getFD()
                    )
                )
            );
        }
        void write(final int b) throws IOException
        {
            ++offset;
            o.write(b);
        }
        void writeByte(final byte b) throws IOException
        {
            ++offset;
            o.writeByte(b);
        }
        void writeShort(final short s) throws IOException
        {
            offset += 2L;
            o.writeShort(s);
        }
        void writeInt(final int i) throws IOException
        {
            offset += 4L;
            o.writeInt(i);
        }
        void writeLong(final long l) throws IOException
        {
            offset += 8L;
            o.writeLong(l);
        }
        void writeBytes(final byte...b) throws IOException
        {
            offset += b.length;
            o.write(b);
        }
        void flush() throws IOException {o.flush();}
        void close() throws IOException {r.close();}
    }
    private static final Input TEXT_IN,NBT_IN,ITEM_IN,MODS_IN;
    private static final Output TEXT_OUT,NBT_OUT,ITEM_OUT,MODS_OUT;
    static
    {
        try
        {
            TEXT_IN = new Input(TEXT_FILE);
             NBT_IN = new Input( NBT_FILE);
            ITEM_IN = new Input(ITEM_FILE);
            MODS_IN = new Input(MODS_FILE);
            
            TEXT_OUT = new Output(TEXT_FILE);
             NBT_OUT = new Output( NBT_FILE);
            ITEM_OUT = new Output(ITEM_FILE);
            MODS_OUT = new Output(MODS_FILE);
        }
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    private static final record ConversionResult(byte[] arr,boolean synced) {}
    private static final record SyncResult(long addr,boolean synced) {}
    private static final record Cache<T>(Map<T,Long> a,Map<Long,T> b)
    {
        Cache() {this(new WeakHashMap<>(),new WeakHashMap<>());}
        void put(final T k,final long v) {a.put(k,v);}
        void put(final long v,final T k) {b.put(v,k);}
        Long get(final T k) {return a.get(k);}
        T get(final long v) {return b.get(v);}
    }
    private interface ToByteArray<T> {ConversionResult apply(T element) throws IOException;}
    
    private static <T> long seekImpl(final T entry,final Cache<T> cache,final byte[] data,
                                     final Input in,final Output out,final int hash) throws IOException
    {
        // See if the entry was already cached.
        {
            final Long fi = cache.get(entry);
            if(fi != null) return fi;
        }
        // Search the file to see if the entry already exists.
        {
            in.seek(0L);
            final long end = out.offset;
            final ByteArrayInputStream d = new ByteArrayInputStream(data);
            final DataInputStream dIn = new DataInputStream(d);
            while(in.offset < end)
            {
                dIn.reset();
                final long start = in.offset;
                final int nhash = in.readInt();
                final long next = in.readLong();
                if(nhash == hash)
                {
                    // Check for match.
                    //noinspection StatementWithEmptyBody
                    while(in.offset != next && dIn.readByte() == in.readByte());
                    if(in.offset == next && dIn.read() == -1)
                        return start;
                }
                in.seek(next);
            }
        }
        // No text found.
        return -1L;
    }
    private static long write(final byte[] data,final int hash,final Output out) throws IOException
    {
        final long addr = out.offset;
        out.writeInt(hash);
        out.writeLong(addr+12L+data.length);
        out.writeBytes(data);
        out.flush();
        return addr;
    }
    private static <T> SyncResult syncImpl(final T entry,final Cache<T> cache,final ToByteArray<T> toByteArr,
                                           final Input in,final Output out) throws IOException
    {
        {
            final Long cached = cache.get(entry);
            if(cached != null) return new SyncResult(cached,false);
        }
        final byte[] data;
        final boolean flag;
        {
            final ConversionResult cr = toByteArr.apply(entry);
            flag = cr.synced;
            data = cr.arr;
        }
        final int hash = entry.hashCode();
        if(!flag) // (flag == true) implies that the entry MUST be new, since some elements were generated by toByteArr.
        {
            final long seek = seekImpl(entry,cache,data,in,out,hash);
            if(seek != -1L)
            {
                cache.put(entry,seek);
                return new SyncResult(seek,false);
            }
        }
        final long l = write(data,hash,out);
        cache.put(entry,l);
        return new SyncResult(l,true);
    }
    private static <T> boolean syncNullableImpl(final T entry,final Cache<T> cache,final ToByteArray<T> toByteArr,
                                                final Input in,final Output out,final DataOutputStream o) throws IOException
    {
        if(entry == null) {o.writeLong(-1L); return false;}
        final SyncResult sr = syncImpl(entry,cache,toByteArr,in,out);
        o.writeLong(sr.addr);
        return sr.synced;
    }
    private static void writeBArray(final byte[] in,final DataOutputStream out) throws IOException
    {
        out.writeInt(in.length);
        out.write(in);
    }
    private static void writeIArray(final int[] in,final DataOutputStream out) throws IOException
    {
        out.writeInt(in.length);
        final ByteBuffer bb = ByteBuffer.allocate(4 * in.length);
        bb.asIntBuffer().put(in);
        out.write(bb.array());
    }
    private static void writeLArray(final long[] in,final DataOutputStream out) throws IOException
    {
        out.writeInt(in.length);
        final ByteBuffer bb = ByteBuffer.allocate(8 * in.length);
        bb.asLongBuffer().put(in);
        out.write(bb.array());
    }
    private static void writeNNString(final String in,final DataOutputStream out) throws IOException
    {
        writeBArray(in.getBytes(CHARSET),out);
    }
    private static void writeNullableString(final String in,final DataOutputStream out) throws IOException
    {
        if(in == null) out.writeInt(-1);
        else writeNNString(in,out);
    }
    private static byte[] readBArray(final Input in) throws IOException {return in.readNBytes(in.readInt());}
    private static int[] readIArray(final Input in) throws IOException
    {
        final int size = in.readInt();
        final int[] out = new int[size];
        ByteBuffer.wrap(in.readNBytes(4 * size)).asIntBuffer().get(out);
        return out;
    }
    private static long[] readLArray(final Input in) throws IOException
    {
        final int size = in.readInt();
        final long[] out = new long[size];
        ByteBuffer.wrap(in.readNBytes(8 * size)).asLongBuffer().get(out);
        return out;
    }
    private static String readNNString(final Input in) throws IOException
    {
        return new String(readBArray(in),CHARSET);
    }
    private static String readNullableString(final Input in) throws IOException
    {
        final int size = in.readInt();
        return size == -1? null : new String(in.readNBytes(size),CHARSET);
    }
    
    private static final Cache<NbtCompound> cachedNBT = new Cache<>();
    /*
    File structure: [(str) -> (32) size, (?) value] [(T[]) -> (32) size, (?T...) values]
    - NbtCompound
        - (32) hash
        - (64) next addr
        - (32) # entries
        - Entry
            - (str) key
            - NbtElement
                - (8) type
                - (?) NbtByte
                    - (8) value
                - (?) NbtByteArray
                    - (8[]) value
                - (?) NbtCompound
                    - (64) addr
                - (?) NbtDouble
                    - (64) value
                - (?) NbtFloat
                    - (32) value
                - (?) NbtInt
                    - (32) value
                - (?) NbtIntArray
                    - (32[]) value
                - (?) NbtList
                    - (8) subtype
                    - (?[]) value
                - (?) NbtLong
                    - (64) value
                - (?) NbtLongArray
                    - (64[]) value
                - (?) NbtNull
                    - N/A
                - (?) NbtShort
                    - (16) value
                - (?) NbtString
                    - (str) value
    */
    private static boolean putNbt(final NbtElement v,final DataOutputStream out,final boolean writeType) throws IOException
    {
        boolean synced = false;
        if(writeType) out.write(v.getType());
        if(v instanceof final NbtByte b)
            out.write(b.byteValue());
        else if(v instanceof final NbtByteArray b)
            writeBArray(b.getByteArray(),out);
        else if(v instanceof final NbtCompound c)
        {
            final SyncResult sr = sync(c);
            synced = sr.synced;
            out.writeLong(sr.addr);
        }
        else if(v instanceof final NbtDouble d)
            out.writeDouble(d.doubleValue());
        else if(v instanceof final NbtFloat f)
            out.writeFloat(f.floatValue());
        else if(v instanceof final NbtInt i)
            out.writeInt(i.intValue());
        else if(v instanceof final NbtIntArray i)
            writeIArray(i.getIntArray(),out);
        else if(v instanceof final NbtList l)
        {
            final byte type = l.getHeldType();
            out.write(type);
            out.writeInt(l.size());
            for(final NbtElement e : ((NbtListMixin)l).getValue())
                synced |= putNbt(e,out,false);
        }
        else if(v instanceof final NbtLong l)
            out.writeLong(l.longValue());
        else if(v instanceof final NbtLongArray l)
            writeLArray(l.getLongArray(),out);
        else if(v instanceof final NbtShort s)
            out.writeShort(s.shortValue());
        else if(v instanceof final NbtString s)
            writeNNString(s.asString(),out);
        return synced;
    }
    private static ConversionResult toByteArr(final NbtCompound nbt)
    {
        final Set<Entry<String,NbtElement>> entries = new TreeSet<>(Entry.comparingByKey());
        entries.addAll(((NbtCompoundMixin)nbt).getEntries().entrySet());
        boolean synced = false;
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
        final DataOutputStream out = new DataOutputStream(buf);
        try
        {
            out.writeInt(entries.size());
            for(final Entry<String,NbtElement> e : entries)
            {
                writeNNString(e.getKey(),out);
                synced |= putNbt(e.getValue(),out,true);
            }
            return new ConversionResult(buf.toByteArray(),synced);
        }
        catch(final IOException impossible) {throw new UncheckedIOException(impossible);}
    }
    private static SyncResult sync(final NbtCompound nbt) throws IOException
    {
        return syncImpl(nbt,cachedNBT,InventoryInspector::toByteArr,NBT_IN,NBT_OUT);
    }
    private static boolean sync(final NbtCompound nbt,final DataOutputStream out) throws IOException
    {
        final SyncResult sr = sync(nbt);
        out.writeLong(sr.addr);
        return sr.synced;
    }
    private static boolean syncNullable(final NbtCompound nbt,final DataOutputStream out) throws IOException
    {
        return syncNullableImpl(nbt,cachedNBT,InventoryInspector::toByteArr,NBT_IN,NBT_OUT,out);
    }
    private static NbtElement readValue(final byte type) throws IOException
    {
        return switch(type)
        {
            case BYTE_TYPE       -> NbtByte.of(NBT_IN.readByte());
            case BYTE_ARRAY_TYPE -> new NbtByteArray(readBArray(NBT_IN));
            case COMPOUND_TYPE   ->
            {
                final long addr = NBT_IN.readLong(),back = NBT_IN.offset;
                final NbtCompound v = readNBT(addr);
                NBT_IN.seek(back);
                yield v;
            }
            case DOUBLE_TYPE     -> NbtDouble.of(NBT_IN.readDouble());
            case FLOAT_TYPE      -> NbtFloat .of(NBT_IN.readFloat ());
            case INT_TYPE        -> NbtInt   .of(NBT_IN.readInt   ());
            case INT_ARRAY_TYPE  -> new NbtIntArray(readIArray(NBT_IN));
            case LIST_TYPE       ->
            {
                final byte t = NBT_IN.readByte();
                final NbtElement[] v = new NbtElement[NBT_IN.readInt()];
                for(int i = 0;i < v.length;++i) v[i] = readValue(t);
                yield NbtListMixin.constructor(Arrays.asList(v),t);
            }
            case LONG_TYPE       -> NbtLong.of(NBT_IN.readLong());
            case LONG_ARRAY_TYPE -> new NbtLongArray(readLArray(NBT_IN));
            case NULL_TYPE       -> NbtNull.INSTANCE;
            case SHORT_TYPE      -> NbtShort.of(NBT_IN.readShort());
            case STRING_TYPE     -> NbtString.of(readNNString(NBT_IN));
            default -> null;
        };
    }
    private static Optional<NbtCompound> readOptionalNBT(final long address) throws IOException
    {
        if(address == -1L) return Optional.empty();
        return Optional.of(readNBT(address));
    }
    private static NbtCompound readNBT(final long address) throws IOException
    {
        {
            final NbtCompound nbt = cachedNBT.get(address);
            if(nbt != null) return nbt;
        }
        NBT_IN.seek(address+12L);
        int size = NBT_IN.readInt();
        final Map<String,NbtElement> m = new HashMap<>(size);
        while(size-- != 0) m.put(readNNString(NBT_IN),readValue(NBT_IN.readByte()));
        final NbtCompound nbt = NbtCompoundMixin.constructor(m);
        cachedNBT.put(address,nbt);
        return nbt;
    }
    
    private static final Cache<Text> cachedText = new Cache<>();
    /*
    File structure: [(str) -> (32) size, (?) value] [(T[]) -> (32) size, (?T...) values]
    - Text
        - (32) hash
        - (64) next addr
        - (64[]) extra
        - (3) type
        - Style
            - (1) bold
            - (1) italic
            - (1) underlined
            - (1) strikethrough
            - (1) obfuscated
            - (32) color
            - Hover
                - (8) action
                - (?) show text
                    - (64) text addr
                - (?) show entity
                    - (8) type
                    - (32) name
                    - (128) uuid
                - (?) show item
                    - (16) item id
                    - (8) count
                    - (64) NBT
            - Click
                - (8) action
                - (str) value
            - (str) insertion
            - (str) font
        - Type-Specific
            - LiteralText
                - (str) text
            - TranslatableText
                - (str) key
                - (?[]) Arguments
                    - (8) arg type
                    - Argument
                        - (?64) text addr
                        - (?str) str
            - ScoreText
                - (str) name
                - (str) objective
            - SelectorText
                - (str) pattern
                - (?64) separator addr
            - KeybindText
                - (str) keybind
            - NbtText
                - (str) nbt path
                - (?64) separator addr
                - (8) interpret
                - (?) BlockNbtText
                    - (str) pos
                - (?) EntityNbtText
                    - (str) selector
                - (?) StorageNbtText
                    - (str) id
    */
    private static ConversionResult toByteArr(final Text text)
    {
        boolean synced = false;
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(256);
        final DataOutputStream out = new DataOutputStream(buf);
        try
        {
            {
                final List<Text> siblings = text.getSiblings();
                out.writeInt(siblings.size());
                for(final Text s : siblings)
                    // No null checks are made in vanilla code, so I assume that 's' will never be null.
                    synced |= sync(s,out);
            }
            final byte type;
            {
                final Style style = text.getStyle();
                final short style_bools = (short)
                (
                    (style.isBold         ()? 0b00010000 : 0) |
                    (style.isItalic       ()? 0b00001000 : 0) |
                    (style.isUnderlined   ()? 0b00000100 : 0) |
                    (style.isStrikethrough()? 0b00000010 : 0) |
                    (style.isObfuscated   ()? 0b00000001 : 0)
                );
                if(text instanceof LiteralText) type = 0;
                else if(text instanceof TranslatableText) type = 1;
                else if(text instanceof ScoreText) type = 2;
                else if(text instanceof SelectorText) type = 3;
                else if(text instanceof KeybindText) type = 4;
                else if(text instanceof BlockNbtText) type = 5;
                else if(text instanceof EntityNbtText) type = 6;
                else
                {
                    assert text instanceof StorageNbtText;
                    type = 7;
                }
                out.writeShort((type << 5) | style_bools);
                {
                    final TextColor c = style.getColor();
                    final boolean hasColor = c != null;
                    out.writeBoolean(hasColor);
                    if(hasColor) out.writeInt(c.getRgb());
                }
                {
                    final HoverEvent he = style.getHoverEvent();
                    if(he != null)
                    {
                        final HoverEvent.Action<?> hea = he.getAction();
                        if(hea == null) out.write(-1);
                        else if(hea == HoverEvent.Action.SHOW_TEXT)
                        {
                            out.write(0);
                            synced |= syncNullable(he.getValue(HoverEvent.Action.SHOW_TEXT),out);
                        }
                        else if(hea == HoverEvent.Action.SHOW_ENTITY)
                        {
                            out.write(1);
                            final HoverEvent.EntityContent ec = he.getValue(HoverEvent.Action.SHOW_ENTITY);
                            if(ec != null)
                            {
                                out.write(Registry.ENTITY_TYPE.getRawId(ec.entityType));
                                synced |= syncNullable(ec.name,out);
                                {
                                    final UUID u = ec.uuid;
                                    out.writeLong(u.getMostSignificantBits());
                                    out.writeLong(u.getLeastSignificantBits());
                                }
                            }
                            else out.write(-1);
                        }
                        else
                        {
                            assert hea == HoverEvent.Action.SHOW_ITEM;
                            out.write(2);
                            final HoverEvent.ItemStackContent ic = he.getValue(HoverEvent.Action.SHOW_ITEM);
                            if(ic != null)
                            {
                                final ItemStack stk = ic.asStack();
                                out.write(stk.getCount());
                                out.writeShort(Registry.ITEM.getRawId(stk.getItem()));
                                synced |= syncNullable(stk.getNbt(),out);
                            }
                            else out.write(-1);
                        }
                    }
                    else out.write(-2);
                }
                {
                    final ClickEvent ce = style.getClickEvent();
                    if(ce != null)
                    {
                        out.write(ce.getAction().ordinal());
                        writeNNString(ce.getValue(),out);
                    }
                    else out.write(-1);
                }
                writeNullableString(style.getInsertion(),out);
                {
                    final Identifier fnt = style.getFont();
                    if(fnt.equals(Style.DEFAULT_FONT_ID)) out.writeInt(-1);
                    else writeNNString(fnt.toString(),out);
                }
            }
            switch(type)
            {
                case 0 -> writeNNString(((LiteralText)text).getRawString(),out);
                case 1 ->
                {
                    final TranslatableText t = (TranslatableText)text;
                    writeNNString(t.getKey(),out);
                    final Object[] args = t.getArgs();
                    out.writeInt(args.length);
                    for(final Object arg : args)
                        if(arg instanceof final Text txt)
                        {
                            out.writeBoolean(false);
                            synced |= sync(txt,out);
                        }
                        else
                        {
                            out.writeBoolean(true);
                            writeNNString(String.valueOf(arg),out);
                        }
                }
                case 2 ->
                {
                    final ScoreText s = (ScoreText)text;
                    writeNNString(s.getName(),out);
                    writeNNString(s.getObjective(),out);
                }
                case 3 ->
                {
                    final SelectorText s = (SelectorText)text;
                    writeNNString(s.getPattern(),out);
                    synced |= syncNullable(s.getSeparator().orElse(null),out);
                }
                case 4 -> writeNNString(((KeybindText)text).getKey(),out);
                default ->
                {
                    final NbtText n = (NbtText)text;
                    writeNNString(n.getPath(),out);
                    synced |= syncNullable(((NbtTextMixin)n).getSeparator().orElse(null),out);
                    out.writeBoolean(n.shouldInterpret());
                    writeNullableString
                    (
                        switch(type)
                        {
                            case 5  -> ((BlockNbtText)n).getPos();
                            case 6  -> ((EntityNbtText)n).getSelector();
                            default -> ((StorageNbtText)n).getId().toString();
                        },
                        out
                    );
                }
            }
            return new ConversionResult(buf.toByteArray(),synced);
        }
        catch(final IOException impossible) {throw new UncheckedIOException(impossible);}
    }
    private static boolean syncNullable(final Text text,final DataOutputStream out) throws IOException
    {
        return syncNullableImpl(text,cachedText,InventoryInspector::toByteArr,TEXT_IN,TEXT_OUT,out);
    }
    private static SyncResult sync(final Text text) throws IOException
    {
        return syncImpl(text,cachedText,InventoryInspector::toByteArr,TEXT_IN,TEXT_OUT);
    }
    private static boolean sync(final Text text,final DataOutputStream out) throws IOException
    {
        final SyncResult sr = sync(text);
        out.writeLong(sr.addr);
        return sr.synced;
    }
    private static Text readNullableText() throws IOException
    {
        final long addr = TEXT_IN.readLong();
        if(addr == -1L) return null;
        final long back = TEXT_IN.offset;
        final Text ret = readText(addr);
        TEXT_IN.seek(back);
        return ret;
    }
    private static Optional<Text> readOptionalText() throws IOException
    {
        final long addr = TEXT_IN.readLong();
        if(addr == -1L) return Optional.empty();
        final long back = TEXT_IN.offset;
        final Text ret = readText(addr);
        TEXT_IN.seek(back);
        return Optional.of(ret);
    }
    private static Text readText(final long address) throws IOException
    {
        {
            final Text t = cachedText.get(address);
            if(t != null) return t;
        }
        TEXT_IN.seek(address+12L);
        final Text[] extra;
        {
            final long[] addr = readLArray(TEXT_IN);
            final long back = TEXT_IN.offset;
            extra = new Text[addr.length];
            int i = 0;
            for(final long a : addr) extra[i++] = readText(a);
            TEXT_IN.seek(back);
        }
        final Style style;
        final byte type;
        {
            final short typeAndBools = TEXT_IN.readShort();
            type = (byte)(typeAndBools >>> 5);
            final TextColor color = TEXT_IN.readBoolean()? TextColor.fromRgb(TEXT_IN.readInt()) : null;
            final HoverEvent he = switch(TEXT_IN.readByte())
            {
                case 0  -> new HoverEvent(HoverEvent.Action.SHOW_TEXT,readNullableText());
                case 1  ->
                {
                    final byte et = TEXT_IN.readByte();
                    if(et == -1) yield new HoverEvent(HoverEvent.Action.SHOW_ENTITY,null);
                    final Text n = readNullableText();
                    yield new HoverEvent
                    (
                        HoverEvent.Action.SHOW_ENTITY,
                        new HoverEvent.EntityContent
                        (
                            Registry.ENTITY_TYPE.get(et),
                            new UUID(TEXT_IN.readLong(),TEXT_IN.readLong()),
                            n
                        )
                    );
                }
                case 2  ->
                {
                    final byte count = TEXT_IN.readByte();
                    if(count == -1) yield new HoverEvent(HoverEvent.Action.SHOW_ITEM,null);
                    final Item item = Registry.ITEM.get(TEXT_IN.readShort());
                    final Optional<NbtCompound> nbt;
                    {
                        final long addr = TEXT_IN.readLong();
                        if(addr == -1L) nbt = Optional.empty();
                        else
                        {
                            final long back = TEXT_IN.offset;
                            nbt = Optional.of(readNBT(addr));
                            TEXT_IN.seek(back);
                        }
                    }
                    yield new HoverEvent
                    (
                        HoverEvent.Action.SHOW_ITEM,
                        new HoverEvent.ItemStackContent
                        (
                            ItemStackMixin.constructor(item,count,nbt)
                        )
                    );
                }
                case -1 -> new HoverEvent(null,null);
                default -> null;
            };
            final ClickEvent ce;
            {
                final byte b = TEXT_IN.readByte();
                ce = b != -1
                    ? new ClickEvent
                      (
                          ClickEvent.Action.values()[b],
                          readNNString(TEXT_IN)
                      )
                    : null;
            }
            final String insertion = readNullableString(TEXT_IN);
            final Identifier font;
            {
                final String id = readNullableString(TEXT_IN);
                font = id == null? Style.DEFAULT_FONT_ID : new Identifier(id);
            }
            style = StyleMixin.constructor
            (
                color,
                (typeAndBools & 0b00010000) != 0,
                (typeAndBools & 0b00001000) != 0,
                (typeAndBools & 0b00000100) != 0,
                (typeAndBools & 0b00000010) != 0,
                (typeAndBools & 0b00000001) != 0,
                ce,he,insertion,font
            );
        }
        final MutableText mt = switch(type)
        {
            case 0  -> new LiteralText(readNNString(TEXT_IN));
            case 1  ->
            {
                final String k = readNNString(TEXT_IN);
                final Object[] args = new Object[TEXT_IN.readInt()];
                final long[] addrs = new long[args.length];
                for(int i = 0;i < args.length;++i)
                    if(TEXT_IN.readBoolean())
                    {
                        addrs[i] = -1L;
                        args[i] = readNNString(TEXT_IN);
                    }
                    else
                        addrs[i] = TEXT_IN.readLong();
                {
                    final long back = TEXT_IN.offset;
                    boolean flag = false;
                    int i = 0;
                    for(final long a : addrs)
                    {
                        if(a != -1L)
                        {
                            flag = true;
                            args[i] = readText(a);
                        }
                        ++i;
                    }
                    if(flag) TEXT_IN.seek(back);
                }
                yield new TranslatableText(k,args);
            }
            case 2  -> new ScoreText(readNNString(TEXT_IN),readNNString(TEXT_IN));
            case 3  -> new SelectorText(readNNString(TEXT_IN),readOptionalText());
            case 4  -> new KeybindText(readNNString(TEXT_IN));
            default ->
            {
                final String nbt = readNNString(TEXT_IN);
                final Optional<Text> sep = readOptionalText();
                final boolean interpret = TEXT_IN.readBoolean();
                final String str = readNullableString(TEXT_IN);
                yield switch(type)
                {
                    case 5  -> new BlockNbtText(nbt,interpret,str,sep);
                    case 6  -> new EntityNbtText(nbt,interpret,str,sep);
                    default ->
                    {
                        assert str != null;
                        yield new StorageNbtText(nbt,interpret,new Identifier(str),sep);
                    }
                };
            }
        };
        for(final Text t : extra) mt.append(t);
        final Text out = mt.setStyle(style);
        cachedText.put(address,out);
        return out;
    }
    
    private static final Cache<ItemStack> cachedItems = new Cache<>();
    /*
    File Structure:
    - Items
        - Item
            - (32) hash
            - (64) next addr
            - (16) id
            - (64) name ref
            - (?64) nbt ref
    */
    private static ConversionResult toByteArr(final ItemStack item) throws IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(144);
        final DataOutputStream out = new DataOutputStream(buf);
        out.writeShort(Registry.ITEM.getRawId(item.getItem()));
        boolean synced = false;
        if(item.hasCustomName()) synced = sync(item.getName(),out);
        else out.writeLong(-1L);
        if(item.hasNbt()) synced |= sync(item.getNbt(),out);
        else out.writeLong(-1L);
        return new ConversionResult(buf.toByteArray(),synced);
    }
    private static SyncResult sync(final ItemStack item) throws IOException
    {
        return syncImpl(item,cachedItems,InventoryInspector::toByteArr,ITEM_IN,ITEM_OUT);
    }
    private static ItemStack readItem(final long address) throws IOException
    {
        {
            final ItemStack is = cachedItems.get(address);
            if(is != null) return is;
        }
        ITEM_IN.seek(address+12L);
        final Item item = Registry.ITEM.get(ITEM_IN.readShort());
        final Text name;
        {
            final long addr = ITEM_IN.readLong();
            name = addr == -1L? null : readText(addr);
        }
        final Optional<NbtCompound> nbt = readOptionalNBT(ITEM_IN.readLong());
        final ItemStack stk = ItemStackMixin.constructor(item,1,nbt);
        if(name != null) stk.setCustomName(name);
        cachedItems.put(address,stk);
        return stk;
    }
    
    private static final byte SET = 0,
                           REMOVE = (byte)(1<<6),
                             FILL = (byte)(2<<6),
                            CLEAR = (byte)(3<<6);
    public static final record Modification(byte type_slot,byte[] count,long[] itemAddr) {}
    public static Modification set(final byte slot,final ItemStack item)
    {
        try {return new Modification((byte)(SET|slot),new byte[] {(byte)item.getCount()},new long[] {sync(item).addr});}
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    public static Modification remove(final byte slot,final byte amount)
    {
        return new Modification((byte)(REMOVE|slot),new byte[] {amount},new long[] {-1L});
    }
    private static Modification fill(final Iterator<ItemStack> items,final byte size)
    {
        final byte[] count = new byte[size];
        final long[] addr = new long[size];
        try
        {
            for(int i = 0;i < size;++i)
            {
                final ItemStack is = items.next();
                if(is == ItemStack.EMPTY)
                    addr[i] = count[i] = -1;
                else
                {
                    addr[i] = sync(is).addr;
                    count[i] = (byte)is.getCount();
                }
            }
        }
        catch(final IOException e) {throw new UncheckedIOException(e);}
        return new Modification(FILL,count,addr);
    }
    public static Modification fill(final Collection<ItemStack> inv) {return fill(inv.iterator(),(byte)inv.size());}
    public static Modification fill(final Inventory inv)
    {
        return fill
        (
            new Iterator<>()
            {
                int i = 0;
                @Override public boolean hasNext() {return true;}
                @Override public ItemStack next() {return inv.getStack(i++);}
            },
            (byte)inv.size()
        );
    }
    private static final Modification CLR = new Modification(CLEAR,new byte[] {(byte)-1},new long[] {-1L});
    public static Modification clear() {return CLR;}
    
    private static final Cache<Modification> cachedMods = new Cache<>();
    /*
    - Modifications
        - Modification
            - (32) hash
            - (64) next
            - (2) type
            - (6) slot
            - (8|8[]) count
            - (64|64[]) item ref
    */
    private static ConversionResult toByteArr(final Modification mod)
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(10);
        final DataOutputStream out = new DataOutputStream(buf);
        try
        {
            out.writeByte(mod.type_slot);
            if(mod.type_slot == FILL)
            {
                out.writeByte(mod.count.length);
                out.write(mod.count);
                out.writeByte(mod.itemAddr.length);
                final ByteBuffer bb = ByteBuffer.allocate(8*mod.itemAddr.length);
                bb.asLongBuffer().put(mod.itemAddr);
                out.write(bb.array());
            }
            else
            {
                out.writeByte(mod.count[0]);
                out.writeLong(mod.itemAddr[0]);
            }
        }
        catch(final IOException impossible) {throw new UncheckedIOException(impossible);}
        return new ConversionResult(buf.toByteArray(),false);
    }
    private static long sync(final Modification mod) throws IOException
    {
        return mod == CLR? -1L : syncImpl(mod,cachedMods,InventoryInspector::toByteArr,MODS_IN,MODS_OUT).addr;
    }
    private static Modification readMod(final long address) throws IOException
    {
        if(address == -1L) return CLR;
        MODS_IN.seek(address+12);
        final byte type_slot = MODS_IN.readByte();
        final byte[] count;
        final long[] addr;
        if(type_slot == FILL)
        {
            count = MODS_IN.readNBytes(MODS_IN.readByte());
            {
                final byte size = MODS_IN.readByte();
                ByteBuffer.wrap(MODS_IN.readNBytes(8*size)).asLongBuffer().get(addr = new long[size]);
            }
        }
        else
        {
            count = new byte[] {MODS_IN.readByte()};
            addr = new long[] {MODS_IN.readLong()};
        }
        return new Modification(type_slot,count,addr);
    }
    
    private static final Style TEXT = Style.EMPTY.withColor(Formatting.GRAY),
                                NUM = Style.EMPTY.withColor(Formatting.GOLD);
    private static MutableText fromPos(final Pos p) {return text(new int[] {p.x,p.y,p.z},TEXT,NUM);}
    private static MutableText fromEntity(final Entity e)
    {
        return e.getName().copy().fillStyle
        (
            Style.EMPTY.withHoverEvent
            (
                new HoverEvent
                (
                    HoverEvent.Action.SHOW_ENTITY,
                    new HoverEvent.EntityContent(e.getType(),e.getUuid(),e.getCustomName())
                )
            ).
            withClickEvent
            (
                new ClickEvent
                (
                    ClickEvent.Action.SUGGEST_COMMAND,
                    e.getUuidAsString()
                )
            )
        );
    }
    private static MutableText fromUUID(final UUID u)
    {
        return text(u.toString(),TEXT).fillStyle
        (
            Style.EMPTY.withHoverEvent
            (
                new HoverEvent
                (
                    HoverEvent.Action.SHOW_ENTITY,
                    new HoverEvent.EntityContent(EntityType.HOPPER_MINECART,u,null)
                )
            ).
            withClickEvent
            (
                new ClickEvent
                (
                    ClickEvent.Action.SUGGEST_COMMAND,
                    u.toString()
                )
            )
        );
    }
    private static final Text CLICK_REMOVE = text("Click to remove",TEXT),
                              CLICK_ADD = text("Click to add",TEXT),
                              CLICK_JUMP = text("Click to jump to event",TEXT),
                              CLICK_BACK = text("Click to go back",TEXT),
                              CLICK_TELEPORT = text("Click to teleport",TEXT),
                              NO_MONITORS = text("No monitors exist",TEXT),
                              NO_REPLAY_SELECTED = text("No monitors selected for replay",TEXT),
                              NO_EVENTS = text("No more recorded events",TEXT),
                              RESET = text("All monitors removed",TEXT);
    
    private static final record Monitor(File f,Input in,Output out) {}
    private static final record Pos(int x,int y,int z) {Pos(final BlockPos p) {this(p.getX(),p.getY(),p.getZ());}}
    private static final record MonitorKey(Pos pos,UUID uuid)
    {
        MonitorKey(final Pos p) {this(p,null);}
        MonitorKey(final Entity e) {this(null,e.getUuid());}
        MonitorKey(final UUID u) {this(null,u);}
        
        boolean isBlock() {return pos != null;}
        @Override public String toString() {return isBlock()? pos.x+" "+pos.y+" "+pos.z : uuid.toString();}
        Text name(final ServerWorld world)
        {
            if(pos != null) return fromPos(pos);
            final Entity e = world.getEntity(uuid);
            return e == null? fromUUID(uuid) : fromEntity(e);
        }
    }
    private static final Map<Pos,MonitorKey> blockKeyCache = new WeakHashMap<>();
    private static final Map<UUID,MonitorKey> entityKeyCache = new WeakHashMap<>();
    private static MonitorKey key(final BlockPos pos)
    {
        final Pos p = new Pos(pos);
        MonitorKey k = blockKeyCache.get(p);
        if(k == null) blockKeyCache.put(p,k = new MonitorKey(p));
        return k;
    }
    private static MonitorKey key(final UUID uuid)
    {
        MonitorKey k = entityKeyCache.get(uuid);
        if(k == null) entityKeyCache.put(uuid,k = new MonitorKey(uuid));
        return k;
    }
    private static MonitorKey key(final Entity entity)
    {
        final UUID uuid = entity.getUuid();
        MonitorKey k = entityKeyCache.get(uuid);
        if(k == null) entityKeyCache.put(uuid,k = new MonitorKey(entity));
        return k;
    }
    private static final Map<MonitorKey,Monitor> monitor = new HashMap<>();
    private static int currentTick = 0;
    private static short order = 0;
    private static final class Replay
    {
        final Input file;
        final ItemStack[] inventory;
        final Pos display;
        int prevTick = -1,nextTick,mod;
        short prevOrder = -1,nextOrder;
        Modification latest = null;
        Replay(final File f,final Pos display) throws IOException
        {
            file = new Input(f);
            Arrays.fill(inventory = new ItemStack[27],ItemStack.EMPTY);
            this.display = display;
            reset();
        }
        private boolean reset() throws IOException
        {
            file.seek(0L);
            nextTick = -1;
            mod = 0;
            Arrays.fill(inventory,ItemStack.EMPTY);
            return nextTick();
        }
        private boolean nextTick() throws IOException
        {
            prevTick = nextTick;
            prevOrder = nextOrder;
            try {nextTick = file.readInt();}
            catch(final EOFException e) {nextTick = -1; nextOrder = -1; return false;}
            nextOrder = file.readShort();
            return true;
        }
        boolean next() throws IOException
        {
            if(nextTick == -1) return false;
            ++mod;
            latest = readMod(file.readLong());
            final byte slot = (byte)(latest.type_slot & ((1<<6)-1));
            switch(latest.type_slot & -(1<<6))
            {
                case SET -> (inventory[slot] = readItem(latest.itemAddr[0])).setCount(latest.count[0]);
                case REMOVE ->
                {
                    final ItemStack is = inventory[slot];
                    final byte nc = (byte)(is.getCount() - latest.count[0]);
                    if(nc > 0) is.setCount(nc);
                    else inventory[slot] = ItemStack.EMPTY;
                }
                case FILL ->
                {
                    byte i = 0;
                    for(final byte ct : latest.count)
                    {
                        final long addr = latest.itemAddr[i];
                        final ItemStack is;
                        if(addr == -1L) is = ItemStack.EMPTY;
                        else (is = readItem(addr)).setCount(ct);
                        inventory[i++] = is;
                    }
                }
                default -> Arrays.fill(inventory,ItemStack.EMPTY);
            }
            return nextTick();
        }
        private boolean next(int count) throws IOException
        {
            if(nextTick != -1) while(count > 0 && next()) --count;
            return count == 0;
        }
        @SuppressWarnings("unused") @Deprecated // This method is provided in the event that it is needed in the future.
        private boolean prev(final int count) throws IOException
        {
            final int oldM = mod;
            return (prevTick == -1 || count < 1)? count == 0 : (reset() && next(Math.max(0,oldM - count)));
        }
        boolean prev() throws IOException
        {
            //return prev(1);
            //TODO maybe reset to last fill/clear instead of beginning?
            if(prevTick == -1) return false;
            final int oldM = mod;
            return reset() && next(oldM-1);
        }
        void setTick(final int tick,final short order) throws IOException
        {
            if((tick < prevTick || (tick == prevTick && order < prevOrder)) && !reset()) return;
            //noinspection StatementWithEmptyBody
            while((tick > nextTick || (tick == nextTick && nextOrder < order)) && next());
        }
        void sync(final Inventory inv)
        {
            byte i = 0;
            for(final ItemStack is : inventory)
            {
                inv.setStack(i,is);
                if(++i >= inv.size()) return;
            }
        }
        void close() throws IOException {file.close();}
    }
    private static final Map<MonitorKey,Replay> replay = new HashMap<>();
    private static int replayTick = 0;
    private static short replayOrder = 0;
    
    /*
    File structure:
    - Monitor
        - Modifications
            - (112...) Modification
                - (32) tick
                - (16) order
                - (64) modification ref
    */
    private static void modify(final Monitor m,final Replay rp,final Modification mod)
    {
        final int tick = SERVER.getTicks();
        try
        {
            if(tick != currentTick)
            {
                currentTick = tick;
                order = 0;
            }
            m.out.writeInt(tick);
            m.out.writeShort(order);
            m.out.writeLong(sync(mod));
            m.out.flush();
            if(rp != null && rp.nextTick == -1)
            {
                rp.nextTick = tick;
                rp.nextOrder = order;
                rp.file.readInt();
                rp.file.readShort();
            }
            ++order;
        }
        catch(final IOException e) {throw new UncheckedIOException(e);}
    }
    
    private static Text addMonitor(final ServerWorld world,final Inventory inv,final MonitorKey k,final String file)
    {
        try
        {
            final File f = File.createTempFile(file+"_",null,FILE_DIR);
            f.deleteOnExit();
            final Monitor m = new Monitor(f,new Input(f),new Output(f));
            {
                final Monitor m2 = monitor.put(k,m);
                if(m2 != null)
                {
                    m2.in.close();
                    m2.out.close();
                    //noinspection ResultOfMethodCallIgnored
                    m2.f.delete();
                }
            }
            if(inv != null && !inv.isEmpty())
                modify(m,null,fill(inv));
        }
        catch(final IOException e) {throw new UncheckedIOException(e);}
        return hoverClick
        (
            text("Monitor added for "+(k.isBlock()? "position ":"entity "),TEXT).append(k.name(world)),
            CLICK_REMOVE,
            "/inventoryInspector monitor "+k+" remove"
        );
    }
    public static Text addMonitor(final ServerWorld world,final BlockPos pos)
    {
        return addMonitor
        (
            world,world.getBlockEntity(pos) instanceof final Inventory b? b : null,
            key(pos),pos.getX()+"_"+pos.getY()+"_"+pos.getZ()
        );
    }
    public static Text addMonitor(final Entity entity)
    {
        return addMonitor
        (
            (ServerWorld)entity.world,entity instanceof final Inventory i? i : null,
            key(entity),entity.getUuidAsString()
        );
    }
    public static Text[] addMonitors(final Collection<? extends Entity> entities)
    {
        final List<Text> out = new ArrayList<>(entities.size());
        for(final Entity e : entities)
        {
            if(!(e instanceof StorageMinecartEntity))
                out.add
                (
                    text("Warning: entity [",TEXT).
                        append
                        (
                            e.getName().copy().fillStyle
                            (
                                Style.EMPTY.withHoverEvent
                                (
                                    new HoverEvent
                                    (
                                        HoverEvent.Action.SHOW_ENTITY,
                                        new HoverEvent.EntityContent(e.getType(),e.getUuid(),e.getName())
                                    )
                                )
                            )
                        ).
                        append(text("] does not have a supported inventory. It's UUID will still be monitored regardless.",TEXT))
                );
            out.add(addMonitor(e));
        }
        return out.toArray(Text[]::new);
    }
    public static Text addMonitor(final ServerWorld world,final UUID uuid) {return addMonitor(world,null,key(uuid),uuid.toString());}
    
    private static final Text DOES_NOT_EXIST = text(" does not exist",TEXT),
                              WAS_REMOVED = text(" was removed",TEXT);
    private static Text removeMonitor(final ServerWorld world,final MonitorKey k,final String type,final Text res)
    {
        return hoverClick
        (
            text("Monitor for "+type+' ',TEXT).
                append(k.name(world)).
                append(res),
            CLICK_ADD,
            "/inventoryInspector monitor "+k
        );
    }
    private static Text removeMonitor(final ServerWorld world,final MonitorKey k)
    {
        final Text res;
        {
            final Monitor m = monitor.remove(k);
            if(m == null) res = DOES_NOT_EXIST;
            else
            {
                res = WAS_REMOVED;
                try
                {
                    m.in.close();
                    m.out.close();
                    //noinspection ResultOfMethodCallIgnored
                    m.f.delete();
                }
                catch(final IOException e) {throw new UncheckedIOException(e);}
            }
        }
        return removeMonitor(world,k,k.isBlock()? "position":"entity",res);
    }
    public static Text removeMonitor(final ServerWorld world,final BlockPos pos) {return removeMonitor(world,key(pos));}
    public static Text[] removeMonitors(final Collection<? extends Entity> entities)
    {
        final List<Text> out = new ArrayList<>(entities.size());
        for(final Entity e : entities)
        {
            final MonitorKey k = key(e);
            final Monitor m = monitor.remove(k);
            if(m != null)
            {
                try
                {
                    m.in.close();
                    m.out.close();
                    //noinspection ResultOfMethodCallIgnored
                    m.f.delete();
                }
                catch(final IOException ioe) {throw new UncheckedIOException(ioe);}
                out.add(removeMonitor((ServerWorld)e.world,k,"entity",WAS_REMOVED));
            }
        }
        return out.toArray(Text[]::new);
    }
    public static Text removeMonitor(final ServerWorld world,final UUID uuid) {return removeMonitor(world,key(uuid));}
    
    public static Text[] listMonitors(final ServerWorld world)
    {
        if(monitor.isEmpty()) return new Text[] {NO_MONITORS};
        final Text[] out = new Text[monitor.size()+1];
        out[0] = text("Active Monitors:",TEXT);
        int i = 0;
        for(final Entry<MonitorKey,Monitor> e : monitor.entrySet())
        {
            final MonitorKey k = e.getKey();
            out[++i] = hoverClick
            (
                text("    ",TEXT).append(k.name(world)),CLICK_REMOVE,
                "/inventoryInspector monitor "+k+" remove"
            );
        }
        return out;
    }
    
    private static void modify(final MonitorKey k,final Modification mod)
    {
        final Monitor m = monitor.get(k);
        if(m != null) modify(m,replay.get(k),mod);
    }
    public static void modify(final BlockPos pos,final Modification mod) {modify(key(pos),mod);}
    public static void modify(final Entity entity,final Modification mod) {modify(key(entity),mod);}
    
    private static void removeInventory(final MonitorKey k) {modify(k,CLR);}
    public static void removeInventory(final BlockPos pos) {removeInventory(key(pos));}
    public static void removeInventory(final Entity entity) {removeInventory(key(entity));}
    
    private static void placeInventory(final MonitorKey k,final Inventory inv)
    {
        final Monitor m = monitor.get(k);
        if(m != null && !inv.isEmpty())
            modify(m,replay.get(k),fill(inv));
    }
    public static void placeInventory(final BlockPos pos,final Inventory inv) {placeInventory(key(pos),inv);}
    
    // I could've made inventory modifications check for replay inventories, but I don't care enough.
    // I re-wrote all the dumb serialization code, so give me a break!
    private static Inventory ensureContainer(final ServerWorld world,final MonitorKey k,final Object raw,final BlockPos display)
    {
        final Inventory inv;
        {
            if(world.getBlockEntity(display) instanceof final Inventory i) inv = i;
            else
            {
                world.setBlockState
                (
                    display,
                    raw instanceof final BlockPos p
                        ? (
                              world.getBlockEntity(p) instanceof Inventory
                                  ? world.getBlockState(p)
                                  : Blocks.WHITE_SHULKER_BOX.getDefaultState()
                          )
                        : (
                              (raw instanceof final UUID u? world.getEntity(u) : raw) instanceof final Inventory i
                                  ? switch(i.size())
                                    {
                                        case  5 -> Blocks.HOPPER;
                                        case 27 -> Blocks.CHEST;
                                        default -> Blocks.WHITE_SHULKER_BOX;
                                    }
                                  : Blocks.WHITE_SHULKER_BOX
                          ).getDefaultState(),
                    Block.SKIP_LIGHTING_UPDATES | Block.NOTIFY_LISTENERS
                );
                inv = (Inventory)world.getBlockEntity(display);
                assert inv != null;
            }
        }
        replay.get(k).sync(inv);
        return inv;
    }
    private static Text startReplay(final ServerWorld world,final MonitorKey k,final Object raw,final BlockPos d,final Monitor m)
    {
        final Pos display = new Pos(d);
        final Replay rp;
        try {rp = new Replay(m.f,display); rp.setTick(replayTick,replayOrder);}
        catch(final IOException e) {throw new UncheckedIOException(e);}
        {
            final Replay r = replay.put(k,rp);
            if(r != null)
                try {r.close();}
                catch(final IOException e) {throw new UncheckedIOException(e);}
        }
        rp.sync(ensureContainer(world,k,raw,d));
        return hoverClick
        (
            text("Replay started at ",TEXT).
                append(fromPos(display)).
                append(text(" for monitor ",TEXT)).
                append(k.name(world)),
            CLICK_REMOVE,
            "/inventoryInspector replay "+k+" remove"
        );
    }
    private static Text startReplay(final ServerWorld world,final MonitorKey k,final Object raw,final BlockPos d)
    {
        final Monitor m = monitor.get(k);
        if(m == null)
            return hoverClick
            (
                text("No monitor was set for "+(k.isBlock()? "position ":"entity "),TEXT).append(k.name(world)),
                CLICK_ADD,
                "/inventoryInspector monitor "+k
            );
        return startReplay(world,k,raw,d,m);
    }
    public static Text startReplay(final ServerWorld world,final BlockPos monitor,final BlockPos display)
    {
        return startReplay(world,key(monitor),monitor,display);
    }
    public static Text startReplay(final Entity monitor,final BlockPos display)
    {
        return startReplay((ServerWorld)monitor.world,key(monitor),monitor,display);
    }
    public static Text startReplay(final ServerWorld world,final UUID uuid,final BlockPos display)
    {
        return startReplay(world,key(uuid),uuid,display);
    }
    
    private static Text stopReplay(final ServerWorld world,final MonitorKey k,final Text res,final String disp)
    {
        return hoverClick
        (
            text("Replay of monitor ",TEXT).
                append(k.name(world)).
                append(res),
            CLICK_ADD,
            "/inventoryInspector replay "+k+" "+disp
        );
    }
    private static Text stopReplay(final ServerWorld world,final MonitorKey k)
    {
        final Text option;
        final String disp;
        {
            final Replay r = replay.remove(k);
            if(r == null)
            {
                option = DOES_NOT_EXIST;
                disp = "~ ~ ~";
            }
            else
            {
                option = WAS_REMOVED;
                try {r.close();} catch(final IOException e) {throw new UncheckedIOException(e);}
                final Pos d = r.display;
                disp = d.x+" "+d.y+" "+d.z;
            }
        }
        return stopReplay(world,k,option,disp);
    }
    public static Text stopReplay(final ServerWorld world,final BlockPos monitor) {return stopReplay(world,key(monitor));}
    public static Text[] stopReplays(final Collection<? extends Entity> entities)
    {
        final List<Text> out = new ArrayList<>(entities.size());
        for(final Entity e : entities)
        {
            final MonitorKey k = key(e);
            final Replay r = replay.remove(k);
            if(r != null)
                out.add
                (
                    stopReplay
                    (
                        (ServerWorld)e.world,k,WAS_REMOVED,
                        r.display.x+" "+r.display.y+" "+r.display.z
                    )
                );
        }
        return out.toArray(Text[]::new);
    }
    public static Text stopReplay(final ServerWorld world,final UUID uuid) {return stopReplay(world,key(uuid));}
    
    public static Text[] listReplays(final ServerWorld world)
    {
        if(replay.isEmpty()) return new Text[] {NO_REPLAY_SELECTED};
        final Text[] out = new Text[replay.size()+1];
        out[0] = text("Active Replays:",TEXT);
        int i = 0;
        for(final Entry<MonitorKey,Replay> e : replay.entrySet())
        {
            final MonitorKey k = e.getKey();
            out[++i] = hoverClick
            (
                text("    ",TEXT).append(k.name(world)).append(" -> ").append(fromPos(e.getValue().display)),
                CLICK_REMOVE,"/inventoryInspector replay "+k+" remove"
            );
        }
        return out;
    }
    
    private static Text showReplay(final Pos d)
    {
        return hoverClick
        (
            text("Replay Location: ",TEXT).append(fromPos(d)),
            CLICK_TELEPORT,
            "/tp "+d.x+" "+d.y+" "+d.z
        );
    }
    private static Text showReplay(final ServerWorld w,final MonitorKey k)
    {
        final Replay r = replay.get(k);
        if(r == null)
        {
            final String text,command,disp;
            if(monitor.containsKey(k))
            {
                text = "No monitor exists for "+(k.isBlock()? "block ":"entity ");
                command = "monitor";
                disp = "";
            }
            else
            {
                text = "No replay was started for monitor ";
                command = "replay";
                disp = " ~ ~ ~";
            }
            return hoverClick
            (
                text(text,TEXT).append(k.name(w)),
                CLICK_ADD,
                "/inventoryInspector "+command+" "+k+disp
            );
        }
        return showReplay(r.display);
    }
    public static Text showReplay(final ServerWorld world,final BlockPos pos) {return showReplay(world,key(pos));}
    public static Text[] showReplays(final Collection<? extends Entity> entities)
    {
        final List<Text> out = new ArrayList<>(entities.size());
        for(final Entity e : entities)
        {
            final Replay r = replay.get(key(e));
            if(r != null) out.add(showReplay(r.display));
        }
        return out.toArray(Text[]::new);
    }
    public static Text showReplay(final ServerWorld world,final UUID uuid) {return showReplay(world,key(uuid));}
    
    private static Text fromItem(final ItemStack is)
    {
        final String nbt;
        {
            if(is.hasNbt())
            {
                final NbtCompound n = is.getNbt();
                assert n != null;
                nbt = n.toString();
            }
            else nbt = "";
        }
        return is.getName().shallowCopy().fillStyle
        (
            Style.EMPTY.withHoverEvent
            (
                new HoverEvent
                (
                    HoverEvent.Action.SHOW_ITEM,
                    new HoverEvent.ItemStackContent(is)
                )
            ).
            withClickEvent
            (
                new ClickEvent
                (
                    ClickEvent.Action.RUN_COMMAND,
                    "/give @s "+is.getItem().toString()+nbt+" "+is.getCount()
                )
            )
        );
    }
    private static Text mod(final Modification mod)
    {
        final byte[] count = mod.count;
        final byte slot = (byte)(mod.type_slot&((1<<6)-1));
        return switch(mod.type_slot&-(1<<6))
        {
            case SET ->
            {
                final ItemStack stk;
                try {stk = readItem(mod.itemAddr[0]);}
                catch(final IOException e) {throw new UncheckedIOException(e);}
                yield
                    text("set slot ",TEXT).
                        append(text(slot,NUM)).
                        append(text(" to [",TEXT)).
                        append(fromItem(stk)).
                        append(text(" x",TEXT)).
                        append(text(count[0],NUM)).
                        append(text(']',TEXT));
            }
            case REMOVE ->
                text("removed ",TEXT).
                    append(text(count[0],NUM)).
                    append(text(" item"+(count[0] == 1? "":"s")+" from slot ",TEXT)).
                    append(text(slot,NUM));
            case FILL ->
            {
                final MutableText out = text("initialized inventory to:",TEXT);
                {
                    byte i = 0;
                    for(final byte ct : count)
                    {
                        final long addr = mod.itemAddr[i];
                        if(addr != -1L)
                        {
                            final ItemStack is;
                            try {is = readItem(addr);}
                            catch(final IOException e) {throw new UncheckedIOException(e);}
                            is.setCount(ct);
                            out.append(" [slot:"+i+",").
                                append(fromItem(is)).
                                append(text(" x",TEXT)).
                                append(text(ct,NUM)).
                                append(text(']',TEXT));
                        }
                        ++i;
                    }
                }
                yield out;
            }
            default -> // CLEAR
                text("cleared the inventory",TEXT);
        };
    }
    private static Text event(final ServerWorld world,final int tick,final short order,final MonitorKey k,final Modification mod)
    {
        return hoverClick
        (
            text("Tick: ",TEXT).
                append(text(tick,NUM)).
                append(text(", Event: ",TEXT)).
                append(text(order,NUM)).
                append(text(", Monitor: ",TEXT)).
                append(k.name(world)).
                append(text(", Modification: ",TEXT)),
            CLICK_JUMP,
            "/inventoryInspector goto "+tick+" "+order
        ).append(mod(mod));
    }
    public static Text nextEvent(final ServerWorld world)
    {
        if(replay.isEmpty()) return NO_REPLAY_SELECTED;
        Entry<MonitorKey,Replay> p;
        Replay p1p;
        {
            final Iterator<Entry<MonitorKey,Replay>> itr = replay.entrySet().iterator();
            do p1p = (p = itr.next()).getValue(); while(itr.hasNext() && p1p.nextTick == -1);
            if(p1p.nextTick == -1) return NO_EVENTS;
            while(itr.hasNext())
            {
                final Entry<MonitorKey,Replay> p2 = itr.next();
                final Replay p2p = p2.getValue();
                if(p2p.nextTick != -1 && (p2p.nextTick < p1p.nextTick || (p2p.nextTick == p1p.nextTick && p2p.nextOrder < p1p.nextOrder)))
                {
                    p = p2;
                    p1p = p2p;
                }
            }
        }
        final short order = p1p.nextOrder;
        replayTick = p1p.nextTick;
        replayOrder = (short)(order+1);
        try {p1p.next();} catch(final IOException e) {throw new UncheckedIOException(e);}
        final MonitorKey k = p.getKey();
        p1p.sync
        (
            ensureContainer
            (
                world,k,
                k.isBlock()
                    ? new BlockPos(k.pos.x,k.pos.y,k.pos.z)
                    : world.getEntity(k.uuid),
                new BlockPos(p1p.display.x,p1p.display.y,p1p.display.z)
            )
        );
        return event(world,replayTick,order,k,p1p.latest);
    }
    public static Text[] nextTick(final ServerWorld world)
    {
        if(replay.isEmpty()) return new Text[] {NO_REPLAY_SELECTED};
        final List<Entry<MonitorKey,Replay>> list = new ArrayList<>(replay.size());
        final int tick;
        {
            final Iterator<Entry<MonitorKey,Replay>> itr = replay.entrySet().iterator();
            int t;
            do
            {
                final Entry<MonitorKey,Replay> e = itr.next();
                if((t = e.getValue().nextTick) != -1)
                {
                    list.add(e);
                    break;
                }
            }
            while(itr.hasNext());
            if(t == -1) return new Text[] {NO_EVENTS};
            while(itr.hasNext())
            {
                final Entry<MonitorKey,Replay> e = itr.next();
                final int nt = e.getValue().nextTick;
                if(nt == -1 || nt > t) continue;
                if(nt < t)
                {
                    t = nt;
                    list.clear();
                }
                list.add(e);
            }
            tick = t;
        }
        // 'list' now has all the entries with the lowest nextTick.
        @SuppressWarnings("unchecked") final Entry<MonitorKey,Replay>[] list2 = list.toArray(Entry[]::new);
        final List<Text> out = new ArrayList<>(list2.length);
        while(!list.isEmpty())
        {
            int idx;
            Entry<MonitorKey,Replay> p;
            {
                final ListIterator<Entry<MonitorKey,Replay>> itr = list.listIterator();
                idx = 0;
                short order = (p = itr.next()).getValue().nextOrder;
                while(itr.hasNext())
                {
                    final Entry<MonitorKey,Replay> p2 = itr.next();
                    final short o = p2.getValue().nextOrder;
                    if(o < order)
                    {
                        order = o;
                        p = p2;
                        idx = itr.previousIndex();
                    }
                }
            }
            // 'p' is now the replay with the least ordinal modification.
            final Replay rp = p.getValue();
            final short order = rp.nextOrder;
            final boolean b;
            try {b = rp.next();} catch(final IOException e) {throw new UncheckedIOException(e);}
            out.add(event(world,tick,order,p.getKey(),rp.latest));
            if(!b || rp.nextTick != tick) list.remove(idx);
        }
        for(final Entry<MonitorKey,Replay> e : list2)
        {
            final Replay r = e.getValue();
            final MonitorKey k = e.getKey();
            r.sync
            (
                ensureContainer
                (
                    world,k,
                    k.isBlock()
                        ? new BlockPos(k.pos.x,k.pos.y,k.pos.z)
                        : world.getEntity(k.uuid),
                    new BlockPos(r.display.x,r.display.y,r.display.z)
                )
            );
        }
        replayTick = tick+1;
        replayOrder = 0;
        return out.toArray(Text[]::new);
    }
    public static Text setTick(final ServerWorld world,final int tick,final short order)
    {
        final int prevTick = replayTick;
        final short prevOrder = replayOrder;
        replayTick = tick;
        replayOrder = order;
        for(final Entry<MonitorKey,Replay> e : replay.entrySet())
        {
            final Replay r = e.getValue();
            try
            {
                r.setTick(tick,order);
                final MonitorKey k = e.getKey();
                r.sync
                (
                    ensureContainer
                    (
                        world,k,
                        k.isBlock()
                            ? new BlockPos(k.pos.x,k.pos.y,k.pos.z)
                            : world.getEntity(k.uuid),
                        new BlockPos(r.display.x,r.display.y,r.display.z)
                    )
                );
            }
            catch(final IOException ioe) {throw new UncheckedIOException(ioe);}
        }
        return hoverClick
        (
            text("Set replay to tick ",TEXT).
                append(text(tick,NUM)).
                append(text(", event ",TEXT)).
                append(text(order,NUM)),
            CLICK_BACK,
            "/inventoryInspector goto "+prevTick+" "+prevOrder
        );
    }
    public static Text previousEvent(final ServerWorld world)
    {
        if(replay.isEmpty()) return NO_REPLAY_SELECTED;
        Entry<MonitorKey,Replay> p;
        Replay p1p;
        {
            final Iterator<Entry<MonitorKey,Replay>> itr = replay.entrySet().iterator();
            do p1p = (p = itr.next()).getValue(); while(itr.hasNext() && p1p.prevTick == -1);
            if(p1p.prevTick == -1) return NO_EVENTS;
            while(itr.hasNext())
            {
                final Entry<MonitorKey,Replay> p2 = itr.next();
                final Replay p2p = p2.getValue();
                if(p2p.prevTick != -1 && (p2p.prevTick > p1p.prevTick || (p2p.prevTick == p1p.prevTick && p2p.prevOrder > p1p.prevOrder)))
                {
                    p = p2;
                    p1p = p2p;
                }
            }
        }
        replayTick = p1p.prevTick;
        replayOrder = p1p.prevOrder;
        try {p1p.prev();} catch(final IOException e) {throw new UncheckedIOException(e);}
        final MonitorKey k = p.getKey();
        p1p.sync
        (
            ensureContainer
            (
                world,k,
                k.isBlock()
                    ? new BlockPos(k.pos.x,k.pos.y,k.pos.z)
                    : world.getEntity(k.uuid),
                new BlockPos(p1p.display.x,p1p.display.y,p1p.display.z)
            )
        );
        return event(world,replayTick,replayOrder,k,p1p.latest);
    }
    public static Text[] prevTick(final ServerWorld world)
    {
        if(replay.isEmpty()) return new Text[] {NO_REPLAY_SELECTED};
        final List<Entry<MonitorKey,Replay>> list = new ArrayList<>(replay.size());
        final int tick;
        {
            final Iterator<Entry<MonitorKey,Replay>> itr = replay.entrySet().iterator();
            int t;
            do
            {
                final Entry<MonitorKey,Replay> e = itr.next();
                if((t = e.getValue().prevTick) != -1)
                {
                    list.add(e);
                    break;
                }
            }
            while(itr.hasNext());
            if(t == -1) return new Text[] {NO_EVENTS};
            while(itr.hasNext())
            {
                final Entry<MonitorKey,Replay> e = itr.next();
                final int nt = e.getValue().prevTick;
                if(nt == -1 || nt < t) continue;
                if(nt > t)
                {
                    t = nt;
                    list.clear();
                }
                list.add(e);
            }
            tick = t;
        }
        // 'list' now has all the entries with the highest prevTick.
        @SuppressWarnings("unchecked") final Entry<MonitorKey,Replay>[] list2 = list.toArray(Entry[]::new);
        final List<Text> out = new ArrayList<>(list2.length);
        while(!list.isEmpty())
        {
            int idx;
            Entry<MonitorKey,Replay> p;
            {
                final ListIterator<Entry<MonitorKey,Replay>> itr = list.listIterator();
                idx = 0;
                short order = (p = itr.next()).getValue().prevOrder;
                while(itr.hasNext())
                {
                    final Entry<MonitorKey,Replay> p2 = itr.next();
                    final short o = p2.getValue().prevOrder;
                    if(o > order)
                    {
                        order = o;
                        p = p2;
                        idx = itr.previousIndex();
                    }
                }
            }
            // 'p' is now the replay with the least ordinal modification.
            final Replay rp = p.getValue();
            final short order = rp.prevOrder;
            final MonitorKey k = p.getKey();
            final boolean b;
            try {b = rp.prev();} catch(final IOException e) {throw new UncheckedIOException(e);}
            out.add(event(world,tick,order,k,rp.latest));
            if(!b || rp.prevTick != tick) list.remove(idx);
        }
        for(final Entry<MonitorKey,Replay> e : list2)
        {
            final Replay r = e.getValue();
            final MonitorKey k = e.getKey();
            r.sync
            (
                ensureContainer
                (
                    world,k,
                    k.isBlock()
                        ? new BlockPos(k.pos.x,k.pos.y,k.pos.z)
                        : world.getEntity(k.uuid),
                    new BlockPos(r.display.x,r.display.y,r.display.z)
                )
            );
        }
        replayTick = tick+1;
        replayOrder = 0;
        return out.toArray(Text[]::new);
    }
    
    public static Text reset()
    {
        if(monitor.isEmpty()) return NO_MONITORS;
        for(final Iterator<Entry<MonitorKey,Monitor>> itr = monitor.entrySet().iterator();
            itr.hasNext();)
        {
            final Entry<MonitorKey,Monitor> e = itr.next();
            final MonitorKey k = e.getKey();
            {
                final Replay r = replay.remove(k);
                if(r != null) try {r.close();} catch(final IOException ioe) {throw new UncheckedIOException(ioe);}
            }
            {
                final Monitor m = e.getValue();
                try
                {
                    m.in.close();
                    m.out.close();
                    //noinspection ResultOfMethodCallIgnored
                    m.f.delete();
                }
                catch(final IOException ioe) {throw new UncheckedIOException(ioe);}
            }
            itr.remove();
        }
        return RESET;
    }
}