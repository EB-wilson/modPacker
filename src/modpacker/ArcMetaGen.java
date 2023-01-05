package modpacker;

import arc.files.Fi;
import arc.util.serialization.Jval;
import modpacker.utils.MetaGenerator;
import modpacker.utils.ModInfo;
import modpacker.utils.PackModel;

import java.io.File;
import java.util.Map;

public class ArcMetaGen implements MetaGenerator {
  @Override
  public String genMeta(PackModel model) {
    Jval res = Jval.newObject();
    res.add("packer_version", ModPacker.selfMeta.version);
    res.add("name", model.name);
    res.add("displayName", model.displayName);
    res.add("description", model.description);
    res.add("version", model.version);
    res.add("author", model.author);
    res.add("minGameVersion", model.minGameVersion);

    if (model.installMessage != null) res.add("installMessage", model.installMessage);

    res.add("deleteAtExit", Jval.valueOf(model.deleteAtExit));
    res.add("disableOther", Jval.valueOf(model.disableOther));
    res.add("shouldBackupData", Jval.valueOf(model.shouldBackupData));
    res.add("shouldClearOldData", Jval.valueOf(model.shouldClearOldData));

    res.add("hidden", Jval.valueOf(true));

    Jval modList = Jval.newArray();
    for (ModInfo mod : model.listMods()) {
      Jval modInfo = Jval.newObject();
      modInfo.add("name", mod.name);
      modInfo.add("version", mod.version);
      modInfo.add("author", mod.author);
      modInfo.add("file", mod.file.getName());
      modList.add(modInfo);
    }
    res.add("mods", modList);

    Jval files = Jval.newArray();
    for (PackModel.Entry entry : model.listFiles()) {
      Jval fileEntry = Jval.newObject();
      fileEntry.add("file", model.getDest(entry.fi));
      fileEntry.add("to", entry.to.endsWith("/")? entry.to + entry.fi.getName(): entry.to);
      files.add(fileEntry);
    }
    res.add("files", files);

    res.add("main", "main.Main");

    return res.toString(Jval.Jformat.formatted);
  }
}
