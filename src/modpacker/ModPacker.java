package modpacker;

import arc.Events;
import arc.files.Fi;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import modpacker.ui.ModPackerDialog;

public class ModPacker extends Mod {
  public static Mods.ModMeta selfMeta;

  ModPackerDialog mainMenu;

  public ModPacker(){
    Events.on(EventType.ClientLoadEvent.class, e -> {
      Time.runTask(1, () -> {
        selfMeta = Vars.mods.getMod(ModPacker.class).meta;

        Vars.ui.mods.buttons.button("Mod Packer", Icon.book, mainMenu::show);
      });
    });
  }

  @Override
  public void init() {
    mainMenu = new ModPackerDialog();

    Global.selfFile = Vars.mods.getMod(ModPacker.class).file.file();

    Global.init();
  }
}
