package com.onehumanawa.cfhcore.content.gui.core.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jetbrains.annotations.Nullable;

public final class JsonWire {
   public static final Gson GSON = (new GsonBuilder()).disableHtmlEscaping().create();
   public static final int MAX_UNPACKED = 8388608;

   public static byte[] encode(Object value) {
      return encode(GSON, value);
   }

   public static byte[] encode(Gson gson, Object value) {
      ByteArrayOutputStream out = new ByteArrayOutputStream(512);

      try {
         Writer w = new OutputStreamWriter(new GZIPOutputStream(out), StandardCharsets.UTF_8);

         try {
            gson.toJson(value, w);
         } catch (Throwable var7) {
            try {
               w.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         w.close();
      } catch (IOException e) {
         throw new IllegalStateException("cannot pack " + value.getClass().getSimpleName(), e);
      }

      return out.toByteArray();
   }

   public static <T> @Nullable T decode(@Nullable byte[] bytes, Class<T> type) {
      return (T)decode(GSON, bytes, type);
   }

   public static <T> @Nullable T decode(Gson gson, @Nullable byte[] bytes, Class<T> type) {
      if (bytes != null && bytes.length != 0) {
         try {
            Reader r = new InputStreamReader(new Capped(new GZIPInputStream(new ByteArrayInputStream(bytes)), 8388608L), StandardCharsets.UTF_8);

            Object var4;
            try {
               var4 = gson.fromJson(r, type);
            } catch (Throwable var7) {
               try {
                  r.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            r.close();
            return (T)var4;
         } catch (JsonParseException | IOException var8) {
            return null;
         }
      } else {
         return null;
      }
   }

   private JsonWire() {
   }

   private static final class Capped extends FilterInputStream {
      private long left;

      Capped(InputStream in, long limit) {
         super(in);
         this.left = limit;
      }

      public int read() throws IOException {
         int b = super.read();
         if (b >= 0 && --this.left < 0L) {
            throw new IOException("payload unpacks to more than the limit");
         } else {
            return b;
         }
      }

      public int read(byte[] buffer, int off, int len) throws IOException {
         int n = super.read(buffer, off, len);
         if (n > 0 && (this.left -= (long)n) < 0L) {
            throw new IOException("payload unpacks to more than the limit");
         } else {
            return n;
         }
      }
   }
}
