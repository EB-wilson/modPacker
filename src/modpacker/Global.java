package modpacker;


import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Global {
  public static File selfFile;

  static ZipFile file;

  public static void init(){
    try {
      file = new ZipFile(selfFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static InputStream getResourceInput(String path){
    try {
      return file.getInputStream(new ZipEntry(path));
    } catch (IOException e) {
      return null;
    }
  }
}
