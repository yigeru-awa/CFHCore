package com.onehumanawa.cfhcore.content.gui.core.event;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class Event<T> {
   private final String name;
   private final List<T> listeners = new CopyOnWriteArrayList();

   public Event(String name) {
      this.name = name;
   }

   public void register(T listener) {
      this.listeners.add(listener);
   }

   public void unregister(T listener) {
      this.listeners.remove(listener);
   }

   public List<T> listeners() {
      return this.listeners;
   }

   public void invoke(Consumer<T> call) {
      for(T l : this.listeners) {
         try {
            call.accept(l);
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] listener of {} threw", this.name, e);
         }
      }

   }

   public boolean invokeUntilConsumed(Predicate<T> call) {
      for(T l : this.listeners) {
         try {
            if (call.test(l)) {
               return true;
            }
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] listener of {} threw", this.name, e);
         }
      }

      return false;
   }
}
