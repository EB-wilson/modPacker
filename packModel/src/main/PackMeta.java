package main;

import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.ArcRuntimeException;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PackMeta{

  public String name;
  public String displayName;
  public String description;
  public String version;
  public String author;

  public String installMessage;

  public Drawable icon;

  public Seq<ModInfo> mods = new Seq<>();
  public ObjectMap<Fi, Fi> additionalFiles = new ObjectMap<>();

  public void load(Jval meta) {
    name = Main.self.name;
    displayName = Main.self.meta.displayName;
    description = Main.self.meta.description;
    version = Main.self.meta.version;
    author = Main.self.meta.author;

    Fi i = Main.selfFile.child("icon.png");
    if (i.exists()){
      try {
        icon = new TextureRegionDrawable(new TextureRegion(new Texture(PixmapIO.readPNG(i))));
      }catch (Throwable ignored){}
    }

    installMessage = meta.getString("installMessage");

    for (Jval jval : meta.get("mods").asArray()) {
      ModInfo mod = new ModInfo();
      mod.name = jval.getString("name");
      mod.version = jval.getString("version");
      mod.author = jval.getString("author");
      mod.fi = Main.modsDir.child(jval.getString("file"));

      try(ZipInputStream in = new ZipInputStream(mod.fi.read());
          Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
      {
        ZipEntry entry;

        boolean check = false;
        while ((entry = in.getNextEntry()) != null){
          if (entry.getName().equals("icon.png")){
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()){
              int n;
              while ((n = in.read()) != -1){
                out.write(n);
              }

              Pixmap pix;
              try{
                PixmapIO.PngReader re = new PixmapIO.PngReader();
                ByteBuffer result = re.read(new ByteArrayInputStream(out.toByteArray()));
                pix = new Pixmap(result, re.width, re.height);
              }catch(Exception e){
                throw new ArcRuntimeException("Error reading PNG");
              }

              mod.icon = new TextureRegionDrawable(new TextureRegion(new Texture(pix)));
            }catch (Throwable e){
              Log.err(e);
            }
          }
          else if (entry.getName().equals("mod.json") || entry.getName().equals("mod.hjson")
              || entry.getName().equals("plugin.json") || entry.getName().equals("plugin.hjson")){
            check = true;

            Jval m = Jval.read(reader);
            mod.displayName = m.getString("displayName");
            mod.description = m.getString("description");
          }
        }

        if (!check){
          Log.err(new RuntimeException("file not a mod"));
          continue;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      mods.add(mod);
    }

    for (Jval jval : meta.get("files").asArray()) {
      additionalFiles.put(
          Main.filesDir.child(jval.getString("file")),
          Vars.dataDirectory.child(jval.getString("to"))
      );
    }
  }
}
