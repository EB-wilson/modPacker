package main;

import arc.files.Fi;
import arc.scene.style.Drawable;

import java.util.Objects;

public class ModInfo {
  public String name;
  public String displayName;
  public String description;
  public String version;
  public String author;
  public Fi fi;

  public Drawable icon;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModInfo modInfo = (ModInfo) o;
    return Objects.equals(name, modInfo.name) && Objects.equals(version, modInfo.version) && Objects.equals(author, modInfo.author) && Objects.equals(fi, modInfo.fi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, author, fi);
  }
}
