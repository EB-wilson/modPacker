package main;

import arc.files.Fi;
import arc.scene.style.Drawable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModInfo {
  public String name;
  public String displayName;
  public String description;
  public String version;
  public String author;
  public Fi fi;

  public Drawable icon;

  private static final Pattern versionMatcher = Pattern.compile("\\d+(\\.\\d+)*");
  public static int compareVersion(String verA, String verB) throws IllegalArgumentException{
    Matcher mA = versionMatcher.matcher(verA);
    if (mA.find()){
      verA = mA.group();
    }
    else verA = null;

    Matcher mB = versionMatcher.matcher(verB);
    if (mB.find()){
      verB = mB.group();
    }
    else verB = null;

    if (verA == null || verB == null)
      throw new IllegalArgumentException("input can not be compare");

    String[] a = verA.split("\\."), b = verB.split("\\.");
    if (a.length != b.length)
      throw new IllegalArgumentException("input can not be compare");

    for (int i = 0; i < a.length; i++) {
      int cmp = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
      if (cmp != 0) return cmp;
    }

    return 0;
  }

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
