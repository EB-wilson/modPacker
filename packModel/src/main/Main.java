package main;

import arc.Events;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

public class Main extends Mod {
  public static Fi selfFile;
  public static Fi modsDir;
  public static Fi filesDir;
  public static Mods.LoadedMod self;

  public Main(){
    Events.on(EventType.ClientLoadEvent.class, e -> {
      Time.run(1, () -> {
        self = Vars.mods.getMod(Main.class);

        selfFile = self.root;
        modsDir = selfFile.child("assets").child("mods");
        filesDir = selfFile.child("assets").child("files");

        ReleaseHandle handler = new ReleaseHandle();
        handler.load();

        new InstallHelperDialog().show();
      });
    });
  }
}
