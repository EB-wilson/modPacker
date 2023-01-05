package modpacker.ui;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.scene.event.Touchable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.ctype.Content;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.maps.Map;
import mindustry.mod.Mods;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.FileChooser;
import modpacker.ArcMetaGen;
import modpacker.utils.ModInfo;
import modpacker.utils.PackModel;
import modpacker.utils.Packer;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static mindustry.Vars.*;

public class ModPackerDialog extends BaseDialog {
  PackModel model;

  Runnable rebuild;

  String nameContains = "";
  String internalNameCont = "";
  String author = "";

  int minGameVersion = -1;
  int isHidden = -1;

  boolean regexPattern = false;
  boolean patternError0, patternError1, patternError2, patternError3;

  Seq<ModInfo> unselectList = new Seq<>();
  Seq<ModInfo> selectedList = new Seq<>();

  SelectMapDialog mapSelector = new SelectMapDialog();
  SavesSelectorDialog saveSelector = new SavesSelectorDialog();
  SchematicSelectorDialog scheSelector = new SchematicSelectorDialog();

  public ModPackerDialog() {
    super("ModPacker");

    resized(this::rebuild);
    shown(() -> {
      model = new PackModel();
      model.minGameVersion = Version.build + (Version.revision == 0 ? "" : "." + Version.revision);
      rebuild();
    });
  }

  public void rebuild(){
    cont.clearChildren();
    buttons.clearChildren();

    addCloseButton();

    buttons.button(Core.bundle.get("packer.packInfos"), Icon.menu, () -> {
      new BaseDialog(""){
        String name = model.name;
        String displayName = model.displayName;
        String version = model.version;
        String description = model.description;
        String author = model.author;

        File icon = model.icon;
        String installMessage = model.installMessage;

        String minGameVersion = model.minGameVersion;

        boolean deleteAtExit = model.deleteAtExit;
        boolean disableOther = model.disableOther;
        boolean shouldBackupData = model.shouldBackupData;
        boolean shouldClearOldData = model.shouldClearOldData;

        {
          titleTable.clear();
          addCloseButton();
          buttons.button(Core.bundle.get("packer.ensure"), Icon.right, () -> {
            if (name.trim().isEmpty() || name.contains(" ") || version.trim().isEmpty() || author.trim().isEmpty() || minGameVersion == null){
              ui.showInfo(Core.bundle.get("packer.metaFormatError"));
              return;
            }

            model.name = name;
            model.displayName = displayName;
            model.version = version;
            model.description = description;
            model.author = author;
            model.installMessage = installMessage;
            model.minGameVersion = minGameVersion;
            model.deleteAtExit = deleteAtExit;
            model.disableOther = disableOther;
            model.shouldBackupData = shouldBackupData;
            model.shouldClearOldData = shouldClearOldData;
            model.icon = icon;

            rebuild.run();

            ui.showInfo(Core.bundle.get("packer.infosSaved"));
          });

          cont.pane(menu -> {
            menu.defaults().left();
            menu.table(inputs -> {
              inputs.defaults().left().growX();
              inputs.add(Core.bundle.get("packer.internalName"));
              inputs.field(name, t -> {
                name = t;
              }).update(t -> t.setColor(name.trim().isEmpty() || name.contains(" ")? Color.red: Color.white));
              inputs.row();

              inputs.add(Core.bundle.get("packer.displayName"));
              inputs.field(displayName, t -> {
                displayName = t;
              }).update(t -> t.setColor(displayName.trim().isEmpty()? Color.red: Color.white));
              inputs.row();

              inputs.add(Core.bundle.get("packer.version"));
              inputs.field(version, t -> {
                version = t;
              }).update(t -> t.setColor(version.trim().isEmpty()? Color.red: Color.white));
              inputs.row();

              inputs.add(Core.bundle.get("packer.description"));
              inputs.area(description, t -> {
                description = t;
              }).height(200);
              inputs.row();

              inputs.add(Core.bundle.get("packer.author"));
              inputs.field(author, t -> {
                author = t;
              }).update(t -> t.setColor(author.trim().isEmpty()? Color.red: Color.white));
              inputs.row();

              inputs.add(Core.bundle.get("packer.minGameVersion"));
              inputs.field(String.valueOf(minGameVersion), t -> {
                if (t.matches("^\\d+(\\.\\d+)*$")) {
                  minGameVersion = t;
                }
                else minGameVersion = null;
              }).update(t -> t.setColor(minGameVersion == null? Color.red: Color.white));
              inputs.row();
            }).growX();
            menu.row();

            menu.button(Core.bundle.get("packer.addInstallMessage"), Styles.grayt, () -> {
              new BaseDialog(""){{
                cont.margin(30).add(Core.bundle.get("packer.inputInstallMessage"));
                cont.row();

                float w = Math.min(Core.graphics.getWidth()/Scl.scl(1.1f), 620);
                TextArea area = cont.area(installMessage, t -> {}).width(w).growY().get();

                buttons.defaults().size(210f, 64f);
                buttons.button("@cancel", this::hide);
                buttons.button("@ok", () -> {
                  installMessage = area.getText();
                  hide();
                }).disabled(b -> area.getText().isEmpty());

                keyDown(KeyCode.escape, this::hide);
                keyDown(KeyCode.back, this::hide);

                Core.scene.setKeyboardFocus(area);
              }}.show();
            }).size(165, 48).pad(5);
            menu.row();

            menu.check(Core.bundle.get("packer.deleteAtExit"), deleteAtExit, b -> deleteAtExit = b);
            menu.row();

            menu.check(Core.bundle.get("packer.disableOther"), disableOther, b -> disableOther = b);
            menu.row();

            menu.check(Core.bundle.get("packer.shouldBackupData"), shouldBackupData, b -> shouldBackupData = b);
            menu.row();

            menu.check(Core.bundle.get("packer.shouldClearOldData"), shouldClearOldData, b -> shouldClearOldData = b);
            menu.row();

            menu.table(t -> {
              t.add(Core.bundle.get("packer.icon"));

              Image iconEle = t.add(new BorderImage() {{
                setDrawable(Tex.nomap);
                border(Pal.accent);
              }}).size(180).pad(12f).padLeft(30).size(200).get();

              t.clicked(() -> {
                platform.showFileChooser(true, "png", f -> {
                  try {
                    Pixmap pix = PixmapIO.readPNG(f);
                    if (pix.width != pix.height)
                      throw new RuntimeException();

                    iconEle.setDrawable(new TextureRegion(new Texture(pix)));
                    icon = f.file();
                  }catch (Throwable e){
                    ui.showInfo(Core.bundle.get("packer.iconFormatError"));
                    iconEle.setDrawable(Tex.nomap);
                  }
                });
              });
            });
            menu.row();
          }).fill().scrollX(false);
        }
      }.show();
    });

    if (Core.graphics.isPortrait()) buttons.row();

    buttons.button(Core.bundle.get("packer.additionalFile"), Icon.file, () -> {
      new BaseDialog(Core.bundle.get("packer.additionalFile")){
        String searching;
        Runnable rebuild;

        Fi currSelect;

        Fi tempFile;
        String toPath;

        Table newFile;

        {
          addCloseButton();

          resized(() -> {
            newFile = null;
            rebuild.run();
          });

          cont.table(main -> {
            main.table(searcher -> {
              searcher.image(Icon.zoom).size(50);
              searcher.field("", t -> {
                searching = t;
                rebuild.run();
              });
            });
            main.row();
            main.table(table -> {
              rebuild = () -> {
                table.clearChildren();

                float width = Math.min(Core.graphics.getWidth()/Scl.scl(1.2f), 1620)/2;

                Table[] fileList = new Table[1];
                table.table(Tex.pane, pane -> {
                  pane.table(bar -> {
                    bar.defaults().width(width).center();
                    bar.add(Core.bundle.get("packer.sourceFiles")).color(Pal.accent).labelAlign(Align.center);
                    bar.add().width(4).pad(3);
                    bar.add(Core.bundle.get("packer.releasePath")).color(Pal.accent).labelAlign(Align.center);
                  }).growX().center();
                  pane.row();
                  pane.image().color(Pal.accent).height(4).growX().padTop(4).padBottom(4);
                  pane.row();
                  pane.top().pane(Styles.noBarPane, list -> {
                    fileList[0] = list;

                    for (PackModel.Entry entry : Seq.with(model.listFiles()).sort()) {
                      Fi fi = new Fi(entry.fi);
                      list.button(item -> {
                        item.add(fi.name()).width(width).left();
                        item.image().color(Color.gray).width(4).growY().pad(-6).padLeft(3).padRight(3);
                        item.add(entry.to).width(width).left();
                      }, Styles.underlineb, () -> currSelect = fi).height(48).fillX().update(b -> {
                        b.setChecked(fi.equals(currSelect));
                      });
                      list.row();
                    }
                  }).scrollX(false).growX().fillY().top().get();
                }).padTop(60).width(width*2 + 6 + 40).growY().top();

                table.table(tool -> {
                  tool.top().defaults().top().size(40);

                  tool.button(Icon.add, Styles.clearNonei, () -> {
                    if (newFile != null){
                      ui.showInfo(Core.bundle.get("packer.currentAdding"));
                      return;
                    }

                    currSelect = null;
                    fileList[0].table(Tex.pane, newItem -> {
                      newFile = newItem;
                      newItem.table(t -> {
                        t.defaults().height(48).left().growX().top();
                        t.table(ta -> {
                          ta.add("").update(b -> {
                            if (tempFile == null){
                              b.setText(Core.bundle.get("packer.noFile"));
                            }
                            else b.setText(tempFile.name());
                          }).padLeft(5).growX().left();
                          ta.button(Icon.link, Styles.clearNonei, () -> {
                            if (!Core.app.openFolder(tempFile.parent().absolutePath())){
                              ui.showInfo(tempFile.absolutePath());
                            }
                          }).disabled(b -> tempFile == null).size(45);
                        });
                        t.row();
                        t.image().color(Color.gray).height(4).growX().pad(-3).padTop(2).padBottom(4);
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.selectFile")).left().growX().padLeft(5), Styles.cleart, () -> {
                          new FileChooser("", file -> true, true, fi -> {
                            tempFile = fi;
                          }).show();
                        });
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.selectMap")).left().growX().padLeft(5), Styles.cleart, () -> {
                          mapSelector.show(fi -> {
                            tempFile = fi;
                          });
                        });
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.selectSaves")).left().growX().padLeft(5), Styles.cleart, () -> {
                          saveSelector.show(fi -> {
                            tempFile = fi;
                          });
                        });
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.selectSchematic")).left().growX().padLeft(5), Styles.cleart, () -> {
                          scheSelector.show(fi -> {
                            tempFile = fi;
                          });
                        });
                      }).growY().width(width).pad(-8).padRight(0);
                      newItem.image().color(Color.lightGray).width(4).growY().pad(-6).padLeft(3).padRight(3);
                      newItem.table(t -> {
                        t.defaults().height(48).left().growX().top();
                        TextField[] field = new TextField[1];
                        t.table(f -> {
                          f.image(Icon.pencil).size(45);
                          field[0] = f.field("", str -> {
                            toPath = str;
                          }).height(38).growX().get();
                          field[0].setMessageText(Core.bundle.get("packer.inputPath"));
                          f.button(Icon.menu, Styles.clearNonei, () -> {
                            String last = Core.settings.getString("lastDirectory", Core.files.absolute(Core.files.getExternalStoragePath()).absolutePath());
                            Core.settings.put("lastDirectory", dataDirectory.absolutePath());
                            new FileChooser(Core.bundle.get("packer.selectTargetFile"), file -> true, false, fi -> {
                              Core.settings.put("lastDirectory", last);

                              File tmp = fi.file().getParentFile();
                              boolean stat = false;
                              while (tmp != null){
                                if (tmp.equals(dataDirectory.file())){
                                  stat = true;
                                  break;
                                }
                                tmp = tmp.getParentFile();
                              }

                              if (!stat){
                                ui.showInfo(Core.bundle.get("packer.requireDataDir"));
                                return;
                              }

                              toPath = fi.absolutePath();
                              toPath = toPath.replace(dataDirectory.absolutePath() + "/", "");
                              field[0].setText(toPath);
                            }).show();
                          }).size(45);
                        });
                        t.row();
                        t.image().color(Color.gray).height(4).growX().pad(-3).padTop(0).padBottom(6);
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.mapsDirectory")).left().growX().padLeft(5), Styles.cleart, () -> {
                          toPath = "maps/";
                          field[0].setText(toPath);
                        });
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.savesDirectory")).left().growX().padLeft(5), Styles.cleart, () -> {
                          toPath = "saves/";
                          field[0].setText(toPath);
                        });
                        t.row();
                        t.button(b -> b.add(Core.bundle.get("packer.schematicsDirectory")).left().growX().padLeft(5), Styles.cleart, () -> {
                          toPath = "schematics/";
                          field[0].setText(toPath);
                        });
                        t.row();
                        t.button(Core.bundle.get("packer.ensure"), Styles.flatBordert, () -> {
                          model.addFile(tempFile.file(), toPath);
                          currSelect = tempFile;

                          newFile = null;
                          tempFile = null;
                          toPath = null;
                          rebuild.run();
                        }).size(160, 48).right().bottom().padBottom(-4).padRight(-4)
                            .disabled(b -> tempFile == null || !tempFile.exists() || toPath == null);
                      }).growY().width(width).pad(-8).padLeft(0);
                    }).update(b -> {
                      if (currSelect != null){
                        newFile = null;
                        tempFile = null;
                        toPath = null;

                        rebuild.run();
                      }
                    }).fillY();
                  });
                  tool.row();

                  tool.button(Icon.cancel, Styles.clearNonei, () -> {
                    newFile = null;
                    tempFile = null;
                    toPath = null;
                    model.removeFile(currSelect.file());
                    currSelect = null;
                    rebuild.run();
                  }).disabled(b -> currSelect == null);
                  tool.row();

                  tool.button(Icon.map, Styles.clearNonei, () -> {
                    ui.showConfirm(Core.bundle.get("packer.exportAllMaps"), () -> {
                      for (Map map : maps.all()) {
                        model.addFile(map.file.file(), "maps/");
                      }

                      newFile = null;
                      tempFile = null;
                      toPath = null;
                      currSelect = null;
                      rebuild.run();
                    });
                  });
                  tool.row();

                  tool.button(Icon.book, Styles.clearNonei, () -> {
                    ui.showConfirm(Core.bundle.get("packer.exportAllSchematics"), () -> {
                      for (Schematic sch : schematics.all()) {
                        model.addFile(sch.file.file(), "schematics/");
                      }

                      newFile = null;
                      tempFile = null;
                      toPath = null;
                      currSelect = null;
                      rebuild.run();
                    });
                  });
                  tool.row();

                  tool.button(Icon.trash, Styles.clearNonei, () -> {
                    ui.showConfirm(Core.bundle.get("packer.clearFiles"), () -> {
                      model.clearFiles();

                      newFile = null;
                      tempFile = null;
                      toPath = null;
                      currSelect = null;
                      rebuild.run();
                    });
                  });
                  tool.row();
                }).padTop(60).padLeft(5).fillX().growY();
              };

              rebuild.run();
            }).grow().pad(0);
          }).grow().padLeft(30).padRight(30).padBottom(20).padTop(20);
        }
      }.show();
    });

    buttons.button(Core.bundle.get("packer.export"), Icon.export, () -> {
      String state = PackModel.getStateMessage(model.check());

      if (state != null){
        ui.showInfo(Core.bundle.get(state));
      }
      else {
        platform.showFileChooser(false, "zip", file -> {
          try(OutputStream out = file.write()){
            Packer.write(model, out, new ArcMetaGen());
            ui.showInfo("@data.exported");
          }catch(Exception e){
            e.printStackTrace();
            ui.showException(e);
          }
        });
      }
    });

    cont.table(layout -> {
      layout.table(searcher -> {
        searcher.image(Icon.zoom).size(50);
        searcher.field("", t -> {
          nameContains = t;
          patternError1 = false;
          rebuild.run();
        }).update(f -> f.setColor(patternError1? Color.red: Color.white));
        searcher.button(Icon.filter, Styles.clearNonei, () -> {
          BaseDialog dialog = new BaseDialog("");
          dialog.titleTable.clear();
          dialog.addCloseButton();

          Runnable build = () -> {
            dialog.cont.clearChildren();

            dialog.cont.defaults().left();

            dialog.cont.add(Core.bundle.get("packer.name"));
            dialog.cont.field(nameContains, t -> {
              nameContains = t;
              patternError1 = false;
              rebuild.run();
            }).update(f -> f.setColor(patternError1? Color.red: Color.white));
            dialog.cont.row();

            dialog.cont.add(Core.bundle.get("packer.internalName"));
            dialog.cont.field(internalNameCont, t -> {
              internalNameCont = t;
              patternError0 = false;
              rebuild.run();
            }).update(f -> f.setColor(patternError0? Color.red: Color.white));
            dialog.cont.row();

            dialog.cont.add(Core.bundle.get("packer.author"));
            dialog.cont.field(author, t -> {
              author = t;
              patternError2 = false;
              rebuild.run();
            }).update(f -> f.setColor(patternError2? Color.red: Color.white));
            dialog.cont.row();

            dialog.cont.check(Core.bundle.get("packer.regexPattern"), regexPattern, b -> regexPattern = b);
            dialog.cont.row();

            dialog.cont.add(Core.bundle.get("packer.minGameVersion"));
            dialog.cont.field(String.valueOf(minGameVersion), t -> {
              try{
                minGameVersion = Integer.parseInt(t);
                patternError3 = false;
              }catch (NumberFormatException e){
                patternError3 = true;
                minGameVersion = -1;
              }
              rebuild.run();
            }).update(f -> f.setColor(patternError3? Color.red: Color.white));
            dialog.cont.row();

            dialog.cont.add(Core.bundle.get("packer.isHidden"));
            dialog.cont.button("", Styles.flatBordert, () -> {
              isHidden++;
              if (isHidden >= 2) isHidden = -1;

              rebuild.run();
            }).update(b -> b.setText(Core.bundle.get(isHidden == -1? "packer.ignored": isHidden == 0? "packer.no": "packer.yes"))).size(200, 50);
            dialog.cont.row();
          };
          build.run();

          dialog.buttons.button(Core.bundle.get("packer.reset"), Icon.trash, () -> {
            nameContains = "";
            internalNameCont = "";
            author = "";
            minGameVersion = -1;
            isHidden = -1;
            regexPattern = false;
            patternError0 = patternError1 = patternError2 = patternError3 = false;
            build.run();
            rebuild.run();
          });

          dialog.show();
        }).size(50);
      }).fillY().growX();
      layout.row();
      layout.add(Core.bundle.get("packer.selectModsTip")).color(Pal.accent).pad(10).center().growX().labelAlign(Align.center);
      layout.row();
      layout.pane(list -> {
        rebuild = () -> {
          float width = Math.min(Core.graphics.getWidth()/Scl.scl(1.05f), 600f);
          boolean horizontal = width*2 < Core.graphics.getWidth()/Scl.scl(1.05f);

          selectedList.clear();
          unselectList.clear();

          list.clearChildren();

          list.defaults().top().pad(3);

          Runnable uns = () -> {
            list.table(unselect -> {
              unselect.add(Core.bundle.get("packer.candidate"));
              unselect.row();
              unselect.image().color(Pal.accent).height(4).pad(4).padLeft(0).padRight(0).growX();
              unselect.row();
              Cons<Table> c = items -> {
                for (Mods.LoadedMod mod : mods.list()) {
                  if (!model.selected(ModInfo.asLoaded(mod)) && filter(mod)) {
                    unselectList.add(ModInfo.asLoaded(mod));
                    buildModItem(items, mod, false, horizontal);
                    items.row();
                  }
                }
                if (unselectList.isEmpty()){
                  items.table(t -> t.center().add("empty").center()).center().height(110).growX().pad(4f);
                  items.row();
                }
                else{
                  items.button(Core.bundle.get("packer.selectAll"), horizontal? Icon.right: Icon.up, Styles.flatBordert, () -> {
                    for (ModInfo mod : unselectList) {
                      model.addMod(mod);
                    }
                    rebuild.run();
                  }).height(90).growX();
                  items.row();
                }
              };

              if (horizontal) {
                unselect.pane(c).scrollX(false).top().growX().fillY();
              } else unselect.table(c).top().growX().fillY();
            }).width(width);
          };

          Runnable sel = () -> {
            list.table(selected -> {
              selected.add(Core.bundle.get("packer.selected"));
              selected.row();
              selected.image().color(Pal.accent).height(4).pad(4).padLeft(0).padRight(0).growX();
              selected.row();
              Cons<Table> c = items -> {
                for (Mods.LoadedMod mod : mods.list()) {
                  if (model.selected(ModInfo.asLoaded(mod)) && filter(mod)) {
                    selectedList.add(ModInfo.asLoaded(mod));
                    buildModItem(items, mod, true, horizontal);
                    items.row();
                  }
                }
                if (selectedList.isEmpty()){
                  items.table(t -> t.center().add("empty").center()).center().height(110).growX().pad(4f);
                  items.row();
                }
                else{
                  items.button(Core.bundle.get("packer.unselectAll"), horizontal? Icon.left: Icon.down, Styles.flatBordert, () -> {
                    for (ModInfo mod : selectedList) {
                      model.removeMod(mod);
                    }
                    rebuild.run();
                  }).height(90).growX();
                  items.row();
                }
              };

              if (horizontal) {
                selected.pane(c).scrollX(false).top().growX().fillY();
              } else selected.table(c).top().growX().fillY();
            }).width(width);
          };

          if (!horizontal) {
            sel.run();
            list.row();
            list.image().color(Color.gray).height(4).pad(4).padLeft(0).padRight(0).growX();
            list.row();
            uns.run();
          }
          else{
            uns.run();
            list.image().color(Color.gray).width(4).pad(4).padTop(0).padBottom(0).growY();
            sel.run();
          }
        };
        rebuild.run();
      }).scrollX(false).fill().top().padTop(15);
    }).growY();
  }

  public boolean filter(Mods.LoadedMod mod){
    if (isHidden != -1){
      if (mod.meta.hidden && isHidden == 0) return false;
      if (!mod.meta.hidden && isHidden == 1) return false;
    }

    if (regexPattern){
      try {
        if (!internalNameCont.trim().isEmpty() && !Pattern.compile(internalNameCont).matcher(mod.name).find()) return false;
      }
      catch (PatternSyntaxException e){
        patternError0 = true;
        return false;
      }
      try {
        if (!nameContains.trim().isEmpty() && !Pattern.compile(nameContains).matcher(mod.meta.displayName).find()) return false;
      }
      catch (PatternSyntaxException e){
        patternError1 = true;
        return false;
      }
      try {
        if (!author.trim().isEmpty() && !Pattern.compile(author).matcher(mod.meta.author).find()) return false;
      }
      catch (PatternSyntaxException e){
        patternError2 = true;
        return false;
      }
    }
    else {
      if (!internalNameCont.trim().isEmpty() && !mod.name.toLowerCase().contains(internalNameCont.toLowerCase())) return false;
      if (!nameContains.trim().isEmpty() && !mod.meta.displayName.toLowerCase().contains(nameContains.toLowerCase())) return false;
      if (!author.trim().isEmpty() && !mod.meta.author.toLowerCase().contains(author.toLowerCase())) return false;
    }

    if (minGameVersion != -1){
      return Integer.parseInt(mod.meta.minGameVersion.split("\\.")[0]) >= minGameVersion;
    }

    return true;
  }

  public void buildModItem(Table parent, Mods.LoadedMod mod, boolean selected, boolean hor){
    parent.button(ta -> {
      ta.top().left();
      ta.margin(12f);

      ta.defaults().left().top();
      ta.table(title1 -> {
        title1.left();

        title1.add(new BorderImage() {{
          if (mod.iconTexture != null) {
            setDrawable(new TextureRegion(mod.iconTexture));
          } else {
            setDrawable(Tex.nomap);
          }
          border(Pal.accent);
        }}).size(102).padTop(-8f).padLeft(-8f).padRight(8f);

        title1.table(text -> {
          text.add("[accent]" + Strings.stripColors(mod.meta.displayName()) + "[]").wrap().top().width(300f).growX().left();
          text.row();
          text.add("[lightgray]" + mod.meta.version + "[]").wrap().top().grow();
          text.row();
          text.add("[gray]" + mod.meta.author + "[]").wrap().bottom().fillY().growX().padBottom(5);

          String state = getStateText(mod, selected);
          if (state != null) {
            text.row();
            text.add(state).wrap().bottom().fillY().growX().padBottom(5);
          }
        }).top().growX();

        title1.add().growX();
      }).growX().growY().left();
      ta.button(selected? (hor? Icon.leftOpen: Icon.downOpen): (hor? Icon.rightOpen: Icon.upOpen), Styles.clearNonei,
          selected? () -> {
            model.removeMod(ModInfo.asLoaded(mod));
            rebuild.run();
          }: () -> {
            model.addMod(ModInfo.asLoaded(mod));
            rebuild.run();
          }
      ).right().size(50).padRight(-8f).padTop(-8f);
    }, Styles.flatBordert, () -> showMod(mod)).height(110).growX().pad(4f);
  }

  //copy paste codes
  private @Nullable String getStateText(Mods.LoadedMod item, boolean selected){
    if(item.isOutdated()){
      return "@mod.incompatiblemod";
    /*}else if(item.isBlacklisted()){
      return "@mod.blacklisted";*/
    }else if(!isSupported(item)){
      return "[red]" + Core.bundle.get("packer.incompatiblegame") + "[]";
    }else if(selected && hasUnmetDependencies(item)){
      return "@mod.unmetdependencies";
    }else if(item.hasContentErrors()){
      return "@mod.erroredcontent";
    }else if(item.meta.hidden){
      return "@mod.multiplayer.compatible";
    }
    return null;
  }

  private boolean isSupported(Mods.LoadedMod item) {
    return compareVersion(item.meta.minGameVersion, model.minGameVersion) >= 0;
  }

  private boolean hasUnmetDependencies(Mods.LoadedMod item) {
    for (String mod : item.meta.dependencies) {
      if (model.listMods().stream().noneMatch(e -> e.name.equals(mod))) return true;
    }
    return false;
  }

  //copy paste codes
  private void showMod(Mods.LoadedMod mod){
    BaseDialog dialog = new BaseDialog(mod.meta.displayName());

    dialog.addCloseButton();

    if(!mobile){
      dialog.buttons.button("@mods.openfolder", Icon.link, () -> Core.app.openFolder(mod.file.absolutePath()));
    }

    dialog.cont.pane(desc -> {
      desc.center();
      desc.defaults().padTop(10).left();

      desc.add("@editor.name").padRight(10).color(Color.gray).padTop(0);
      desc.row();
      desc.add(mod.meta.displayName()).growX().wrap().padTop(2);
      desc.row();
      if(mod.meta.author != null){
        desc.add("@editor.author").padRight(10).color(Color.gray);
        desc.row();
        desc.add(mod.meta.author).growX().wrap().padTop(2);
        desc.row();
      }
      if(mod.meta.description != null){
        desc.add("@editor.description").padRight(10).color(Color.gray).top();
        desc.row();
        desc.add(mod.meta.description).growX().wrap().padTop(2);
        desc.row();
      }
    }).width(400f);

    Seq<UnlockableContent> all = Seq.with(content.getContentMap()).<Content>flatten().select(c -> c.minfo.mod == mod && c instanceof UnlockableContent).as();
    if(all.any()){
      dialog.cont.row();
      dialog.cont.button("@mods.viewcontent", Icon.book, () -> {
        BaseDialog d = new BaseDialog(mod.meta.displayName());
        d.cont.pane(cs -> {
          int i = 0;
          for(UnlockableContent c : all){
            cs.button(new TextureRegionDrawable(c.uiIcon), Styles.flati, iconMed, () -> {
              ui.content.show(c);
            }).size(50f).with(im -> {
              var click = im.getClickListener();
              im.update(() -> im.getImage().color.lerp(!click.isOver() ? Color.lightGray : Color.white, 0.4f * Time.delta));

            }).tooltip(c.localizedName);

            if(++i % (int)Math.min(Core.graphics.getWidth() / Scl.scl(110), 14) == 0) cs.row();
          }
        }).grow();
        d.addCloseButton();
        d.show();
      }).size(300, 50).pad(4);
    }

    dialog.show();
  }

  public static int compareVersion(String a, String b){
    if (a.equals(b)) return 0;

    String[] verA = a.split("\\."), verB = b.split("\\.");

    int cmp = Integer.compare(Integer.parseInt(verA[0]), Integer.parseInt(verB[0]));
    if (cmp == 0){
      if (verA.length == 1){
        verA = Arrays.copyOf(verA, 2);
        verA[1] = "0";
      }
      if (verB.length == 1){
        verB = Arrays.copyOf(verA, 2);
        verB[1] = "0";
      }

      return Integer.compare(Integer.parseInt(verA[1]), Integer.parseInt(verB[1]));
    }
    else return cmp;
  }
}
