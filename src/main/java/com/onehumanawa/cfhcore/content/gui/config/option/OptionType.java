package com.onehumanawa.cfhcore.content.gui.config.option;

public enum OptionType {
   BOOLEAN,
   INT,
   DOUBLE,
   CHOICE,
   STRING,
   COLOR,
   LIST,
   KEYBIND,
   ACTION,
   INFO;

   public boolean snapshotable() {
      return this != ACTION && this != INFO;
   }
}
