package modpacker.utils;

import arc.files.Fi;
import modpacker.Global;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Packer {
  public static byte[] pack(PackModel model, MetaGenerator gen) throws IOException {
    if (model.check() != 0)
      throw new IllegalStateException("invalid state of packing model");

    try(ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(out);
        OutputStreamWriter writer = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8))
    {
      int tempBit;
      zipOut.putNextEntry(new ZipEntry("mod.hjson"));

      String modMeta = model.genMeta(gen);

      writer.write(modMeta);
      writer.flush();
      zipOut.flush();
      zipOut.closeEntry();

      if (model.icon != null) {
        zipOut.putNextEntry(new ZipEntry("icon.png"));
        zipOut.write(readAllBytes(model.icon));
        zipOut.flush();
        zipOut.closeEntry();
      }

      InputStream in = Global.getResourceInput("assets/packed_template.jar");
      if (in == null)
        throw new RuntimeException("template jar was not found");

      try(ZipInputStream mainReader = new ZipInputStream(in)) {
        ZipEntry entry;
        while ((entry = mainReader.getNextEntry()) != null) {
          if (entry.isDirectory()) continue;

          zipOut.putNextEntry(new ZipEntry(entry));
          while ((tempBit = mainReader.read()) != -1) {
            zipOut.write(tempBit);
            zipOut.flush();
          }
          zipOut.closeEntry();
        }
      }

      for (ModInfo mod : model.mods) {
        zipOut.putNextEntry(new ZipEntry("assets/mods/" + mod.file.getName()));
        zipOut.write(readAllBytes(mod.file));
        zipOut.flush();
        zipOut.closeEntry();
      }

      for (Map.Entry<File, String> fileEntry : model.nameEntry.entrySet()) {
        zipOut.putNextEntry(new ZipEntry("assets/files/" + fileEntry.getValue()));
        zipOut.write(readAllBytes(fileEntry.getKey()));
        zipOut.flush();
        zipOut.closeEntry();
      }

      zipOut.finish();
      zipOut.flush();
      return out.toByteArray();
    }
  }

  private static byte[] readAllBytes(File file) {
    try(FileInputStream input = new FileInputStream(file); ByteArrayOutputStream outBuff = new ByteArrayOutputStream()){
      int i;
      while ((i = input.read()) != -1){
        outBuff.write(i);
      }
      return outBuff.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void write(PackModel model, OutputStream out, MetaGenerator gen){
    try{
      byte[] bytes = pack(model, gen);
      out.write(bytes);
      out.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
