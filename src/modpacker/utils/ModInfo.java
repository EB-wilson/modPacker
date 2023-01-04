package modpacker.utils;

import mindustry.mod.Mods;

import java.io.File;
import java.util.*;

public class ModInfo {
  public String name;
  public String version;
  public String author;
  public int minGameVersion;
  public File file;
  public List<String> dependencies = Arrays.asList();

  private static HashMap<Mods.LoadedMod, ModInfo> map;

  public static ModInfo asLoaded(Mods.LoadedMod mod){
    if (map == null){
      map = new HashMap<>();
    }

    return map.computeIfAbsent(mod, m -> {
      ModInfo res = new ModInfo();
      res.name = m.name;
      res.version = m.meta.version;
      res.author = m.meta.author;
      res.dependencies = new ArrayList<>(m.meta.dependencies.list());

      res.minGameVersion = Integer.parseInt(m.meta.minGameVersion);

      res.file = m.file.file();

      return res;
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModInfo modInfo = (ModInfo) o;
    return Objects.equals(name, modInfo.name) && Objects.equals(version, modInfo.version) && Objects.equals(author, modInfo.author) && Objects.equals(file, modInfo.file) && Objects.equals(dependencies, modInfo.dependencies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, author, file, dependencies);
  }
}
