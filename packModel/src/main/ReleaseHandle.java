package main;

import arc.Core;
import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Streams;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.ui.dialogs.ModsDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static arc.Core.settings;
import static mindustry.Vars.*;

public class ReleaseHandle {
  public static final Fi backupsDir = dataDirectory.child("backups");

  public PackMeta meta;

  public ObjectSet<ModInfo> selectedMod = new ObjectSet<>();
  public ObjectSet<Fi> selectFiles = new ObjectSet<>();

  public boolean deleteAtExit;
  public boolean disableOther;
  public boolean shouldBackupData;
  public boolean shouldClearOldData;

  public int repeatFileHandle = 0;

  boolean backup;

  public void release() {
    if (shouldBackupData && !backup){
      platform.showFileChooser(false, Core.bundle.get("package.selectBackupFile"), "zip", file -> {
        try{
          exportData(file);
          ui.showInfo("@data.exported");
          backup = true;
          release();
        }catch(Exception e){
          e.printStackTrace();
          ui.showException(e);
        }
      });

      return;
    }

    if (shouldClearOldData){
      ObjectMap<String, Object> map = new ObjectMap<>();
      for(String value : Core.settings.keys()){
        if(value.contains("usid") || value.contains("uuid")){
          map.put(value, Core.settings.get(value, null));
        }
      }
      Core.settings.clear();
      Core.settings.putAll(map);

      for(Fi file : dataDirectory.list()){
        if (file.equals(backupsDir)) continue;
        file.deleteDirectory();
      }
    }

    if (disableOther){
      for (Mods.LoadedMod mod : Vars.mods.list()) {
        Vars.mods.setEnabled(mod, false);
      }
    }

    for (ModInfo info : selectedMod) {
      try{
        Vars.mods.importMod(info.fi);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    for (Fi file : selectFiles) {
      Fi target = meta.additionalFiles.get(file);
      if (repeatFileHandle == 1 && target.exists()) continue;

      int c = 1;
      while (repeatFileHandle == 2 && target.exists()){
        target = target.parent().child(target.nameWithoutExtension() + "(" + c + ")." + target.extension());
        c++;
      }

      file.copyTo(target);
    }

    if (deleteAtExit && !shouldClearOldData){
      mods.removeMod(Main.self);
      Main.self.file.file().deleteOnExit();
      if (Main.self.file.exists()){
        mods.setEnabled(Main.self, false);
      }
    }
  }

  public void exportData(Fi file) throws IOException{
    Seq<Fi> files = new Seq<>();
    files.addAll(dataDirectory.list()).remove(e -> e.equals(backupsDir));
    String base = Core.settings.getDataDirectory().path();

    //add directories
    for(Fi other : files.copy()){
      Fi parent = other.parent();
      while(!files.contains(parent) && !parent.equals(settings.getDataDirectory())){
        files.add(parent);
      }
    }

    try(OutputStream fos = file.write(false, 2048); ZipOutputStream zos = new ZipOutputStream(fos)){
      for(Fi add : files){
        String path = add.path().substring(base.length());
        if(add.isDirectory()) path += "/";
        //fix trailing / in path
        path = path.startsWith("/") ? path.substring(1) : path;
        zos.putNextEntry(new ZipEntry(path));
        if(!add.isDirectory()){
          Streams.copy(add.read(), zos);
        }
        zos.closeEntry();
      }
    }
  }

  public void load(){
    meta = new PackMeta();
    try(Reader reader = Main.selfFile.child("mod.hjson").reader()) {
      Jval infos = Jval.read(reader);
      meta.load(infos);

      selectedMod.addAll(meta.mods.select(e -> {
        Mods.LoadedMod mod = mods.getMod(e.name);
        if (mod == null) return true;
        if (mod.hasSteamID()) return false;

        try{
          return ModInfo.compareVersion(e.version, mod.meta.version) >= 0;
        }catch (IllegalArgumentException err){
          return false;
        }
      }));

      selectFiles.addAll(meta.additionalFiles.keys().toSeq());

      deleteAtExit = infos.getBool("deleteAtExit", false);
      disableOther = infos.getBool("disableOther", false);
      shouldBackupData = infos.getBool("shouldBackupData", false);
      shouldClearOldData = infos.getBool("shouldClearOldData", false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
