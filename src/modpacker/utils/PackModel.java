package modpacker.utils;


import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class PackModel {
  public String name = "";
  public String displayName = "";
  public String description = "";
  public String version = "";
  public String author = "";
  public int minGameVersion = -1;

  public File icon;
  public String installMessage;

  public boolean deleteAtExit;
  public boolean disableOther;
  public boolean shouldBackupData;
  public boolean shouldClearOldData;

  HashSet<ModInfo> mods = new HashSet<>();
  HashMap<File, String> additionalFile = new HashMap<>();
  HashMap<File, String> nameEntry = new HashMap<>();

  public void addFile(File targetFile, String resultTo){
    if (additionalFile.put(targetFile, resultTo.trim()) == null){
      nameEntry.put(targetFile, nameEntry.size() + ".file");
    }
  }

  public void removeFile(File file){
    additionalFile.remove(file);
  }

  public boolean selected(ModInfo mod){
    return mods.contains(mod);
  }

  public void addMod(ModInfo mod){
    mods.add(mod);
  }

  public void removeMod(ModInfo mod){
    mods.remove(mod);
  }

  public Set<ModInfo> listMods(){
    return mods;
  }

  public List<Entry> listFiles() {
    ArrayList<Entry> res = new ArrayList<>();

    for (Map.Entry<File, String> entry : additionalFile.entrySet()) {
      res.add(new Entry(){{
        fi = entry.getKey();
        to = entry.getValue();
      }});
    }

    return res;
  }

  public void clearFiles() {
    additionalFile.clear();
  }

  public String getDest(File fi) {
    return nameEntry.get(fi);
  }

  public static class Entry implements Comparable<Entry>{
    public File fi;
    public String to;

    @Override
    public int compareTo(Entry entry) {
      return fi.getName().compareTo(entry.fi.getName());
    }
  }

  public int check(){
    HashMap<String, Boolean> dependencies = new HashMap<>();
    for (ModInfo mod : mods) {
      for (String dependence : mod.dependencies) {
        dependencies.putIfAbsent(dependence, false);
      }
      dependencies.put(mod.name, true);
    }

    for (Boolean value : dependencies.values()) {
      if (!value) return 1;
    }

    for (File fi : additionalFile.keySet()) {
      try{
        new FileInputStream(fi).close();
      }catch (Throwable e){
        return 2;
      }
    }

    if (name.isEmpty() || version.isEmpty() || author.isEmpty() || displayName.isEmpty()) return 3;

    return 0;
  }

  public static String getStateMessage(int stateCode){
    return switch (stateCode) {
      case 1 -> "packer.requireDepend";
      case 2 -> "packer.fileError";
      case 3 -> "packer.metaMissing";
      default -> null;
    };
  }

  public String genMeta(MetaGenerator generator){
    return generator.genMeta(this);
  }
}
