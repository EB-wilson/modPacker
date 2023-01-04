package main;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.CheckBox;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.mod.Mods;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.ModsDialog;
import mindustry.ui.fragments.MenuFragment;

import static mindustry.Vars.*;
import static mindustry.Vars.ui;

public class InstallHelperDialog extends BaseDialog {
  ReleaseHandle handle;
  boolean useDefaultConf;

  BaseDialog modList = new BaseDialog(Core.bundle.get("package.modList")){
    {
      buttons.defaults().size(210f, 64f);
      buttons.button("@back", Icon.left, () -> {
        InstallHelperDialog.this.show();
        hide();
      }).size(210f, 64f);

      buttons.button(Core.bundle.get("package.next"), Icon.right, () -> {
        fileList.show();
        hide();
      });

      Runnable build = () -> {
        float width = Math.min(Core.graphics.getWidth()/Scl.scl(1.05f), 600f);

        cont.clearChildren();

        cont.table(main -> {
          main.defaults().left();
          main.add(Core.bundle.get("package.modListTip")).width(width - 10).wrap();
          main.row();
          main.image().color(Pal.accent).growX().height(4).padTop(5).padBottom(3);
          main.row();
          main.pane(list -> {
            list.defaults().width(width);
            for (ModInfo mod : handle.meta.mods) {
              buildModItem(list, mod);
              list.row();
            }
          }).fill();
        }).growY().fillX().padTop(60).padBottom(80);
      };

      shown(build);
      resized(build);
    }

    public void buildModItem(Table parent, ModInfo mod){
      parent.button(ta -> {
        ta.top().left();
        ta.margin(12f);

        ta.defaults().left().top();
        ta.table(title1 -> {
          title1.left();

          title1.add(new BorderImage() {{
            if (mod.icon != null) {
              setDrawable(mod.icon);
            } else {
              setDrawable(Tex.nomap);
            }
            border(Pal.accent);
          }}).size(102).padTop(-8f).padLeft(-8f).padRight(8f);

          title1.table(text -> {
            text.add("[accent]" + Strings.stripColors(mod.displayName) + "[]").wrap().top().width(300f).growX().left();
            text.row();
            text.add("[lightgray]" + mod.version + "[]").wrap().top().grow();
            text.row();
            text.add("[gray]" + mod.author + "[]").wrap().bottom().fillY().growX().padBottom(5);
          }).top().growX();

          title1.add().growX();
        }).growX().growY().left();

        ta.check("", b -> {
          if (!handle.selectedMod.add(mod)) handle.selectedMod.remove(mod);
        }).right().size(50).padRight(-8f).padTop(-8f).update(c -> c.setChecked(handle.selectedMod.contains(mod)));
      }, Styles.flatBordert, () -> showMod(mod)).height(110).growX().pad(4f);
    }

    //copy paste codes
    private void showMod(ModInfo mod){
      BaseDialog dialog = new BaseDialog(mod.displayName);

      dialog.addCloseButton();

      dialog.cont.pane(desc -> {
        desc.center();
        desc.defaults().padTop(10).left();

        desc.add("@editor.name").padRight(10).color(Color.gray).padTop(0);
        desc.row();
        desc.add(mod.displayName).growX().wrap().padTop(2);
        desc.row();
        if(mod.author != null){
          desc.add("@editor.author").padRight(10).color(Color.gray);
          desc.row();
          desc.add(mod.author).growX().wrap().padTop(2);
          desc.row();
        }
        if(mod.description != null){
          desc.add("@editor.description").padRight(10).color(Color.gray).top();
          desc.row();
          desc.add(mod.description).growX().wrap().padTop(2);
          desc.row();
        }
      }).width(400f);

      dialog.show();
    }
  };

  BaseDialog fileList = new BaseDialog(Core.bundle.get("package.fileList")){
    {
      buttons.defaults().size(210f, 64f);
      buttons.button("@back", Icon.left, () -> {
        modList.show();
        hide();
      }).size(210f, 64f);

      buttons.button(Core.bundle.get("package.next"), Icon.right, () -> {
        confirmDialog.show();

        hide();
      });

      Runnable build = () -> {
        cont.clearChildren();

        float width = Math.min(Core.graphics.getWidth()/Scl.scl(1.2f), 1600);

        cont.table(but -> {
          but.add(Core.bundle.get("package.repeatFileHandle")).padLeft(4);
          but.button("", () -> handle.repeatFileHandle = (handle.repeatFileHandle + 1)%3).left().size(100, 45)
              .update(b -> b.setText(handle.repeatFileHandle == 0? Core.bundle.get("package.override"):
                  handle.repeatFileHandle == 1? Core.bundle.get("package.skip"): Core.bundle.get("package.retain"))).padLeft(0).padBottom(4);
        }).padTop(60).top().left();
        cont.row();

        cont.table(Tex.pane, pane -> {
          pane.top().pane(Styles.noBarPane, list -> {
            Seq<Fi[]> seq = new Seq<>();
            for (ObjectMap.Entry<Fi, Fi> file : handle.meta.additionalFiles) {
              seq.add(new Fi[]{file.key, file.value});
            }

            for (Fi[] entry : seq.sort((a, b) -> a[1].name().compareTo(b[1].name()))) {
              Fi fi = entry[0];
              list.button(item -> {
                item.add(entry[1].name()).left().padLeft(4).growX();
                item.add("size: " + fi.length()).right().padRight(4);
              }, Styles.underlineb, () -> {
                if(!handle.selectFiles.add(fi)) handle.selectFiles.remove(fi);
              }).height(48).width(width).update(b -> {
                b.setChecked(handle.selectFiles.contains(fi));
              });
              list.row();
            }
          }).scrollX(false).growX().fillY().top().get();
        }).padTop(5).width(width + 40).growY().top();
      };

      shown(build);
      resized(build);
    }
  };

  BaseDialog confirmDialog = new BaseDialog(Core.bundle.get("package.installConfig")){
    {
      buttons.defaults().size(210f, 64f);
      buttons.button("@back", Icon.left, () -> {
        fileList.show();
        hide();
      }).size(210f, 64f);

      buttons.button(Core.bundle.get("package.next"), Icon.right, () -> {
        ui.showConfirm(Core.bundle.get("package.installConfirm"), () -> releasePack());
      });

      Runnable build = () -> {
        cont.clearChildren();

        cont.table(tab -> {
          tab.defaults().left();
          tab.add(Core.bundle.get("package.installTip")).color(Color.lightGray).growX().wrap();
          tab.row();
          tab.image().color(Color.gray).height(4).pad(4).padLeft(0).padRight(0).growX();
          tab.row();
          tab.check(Core.bundle.get("package.deleteAtExit"), handle.deleteAtExit, b -> handle.deleteAtExit = b);
          tab.row();
          tab.check(Core.bundle.get("package.disableOther"), handle.disableOther, b -> handle.disableOther = b);
          tab.row();
          tab.check(Core.bundle.get("package.shouldBackupData"), handle.shouldBackupData, b -> handle.shouldBackupData = b);
          tab.row();
          tab.check(Core.bundle.get("package.shouldClearOldData"), handle.shouldClearOldData, b -> handle.shouldClearOldData = b);
          tab.row();
        }).fill();
      };

      shown(build);
      resized(build);
    }
  };

  public InstallHelperDialog() {
    super(Core.bundle.get("package.installHelper"));

    handle = new ReleaseHandle();
    handle.load();

    buttons.defaults().size(210f, 64f);
    buttons.button("@back", Icon.left, () -> {
      Vars.mods.setEnabled(Main.self, false);
      hide();
    }).size(210f, 64f);

    buttons.button(Core.bundle.get("package.start"), Icon.right, () -> {
      if (!useDefaultConf) {
        modList.show();
        hide();
      }
      else{
        ui.showConfirm(Core.bundle.get("package.installConfirm"), this::releasePack);
      }
    });
    buttons.check(Core.bundle.get("package.useDefaultConfig"), useDefaultConf, b -> useDefaultConf = b);

    resized(this::rebuild);
    shown(this::rebuild);
  }

  private void releasePack() {
    try {
      ui.showInfoOnHidden(Core.bundle.get("package.installed"), () -> {
        Log.info("Exiting to reload mods.");
        Core.app.exit();
      });

      handle.release();
    }catch (Throwable e){
      ui.showException(e);
      Log.err(e);
    }
  }

  public void rebuild(){
    float width = Math.min(Core.graphics.getWidth()/Scl.scl(1.1f), 760);

    cont.clearChildren();

    cont.table(Tex.pane, main -> {
      main.add(Core.bundle.get("package.helperInfo")).width(width-20).pad(5).wrap();
      main.row();
      main.top().pane(Styles.noBarPane, pane -> {
        pane.table(Tex.underlineOver, infos -> {
          BorderImage image = new BorderImage();
          image.border(Pal.accent);

          image.setDrawable(handle.meta.icon == null? Tex.nomap: handle.meta.icon);

          infos.add(image).size(360).left().get();
          infos.top().table(bar -> {
            bar.defaults().padTop(5).padLeft(2).padBottom(2).padRight(12).top().left().growX();
            bar.add(Core.bundle.get("package.name")).color(Color.gray);
            bar.row();
            bar.add(handle.meta.displayName);
            bar.row();
            bar.add(Core.bundle.get("package.author")).color(Color.gray);
            bar.row();
            bar.add(handle.meta.author);
            bar.row();
            bar.add(Core.bundle.get("package.version")).color(Color.gray);
            bar.row();
            bar.add(handle.meta.version);
            bar.row();
            bar.add(Core.bundle.get("package.description")).color(Color.gray);
            bar.row();
            bar.add(handle.meta.description).wrap().maxWidth(400).grow().top().labelAlign(Align.topLeft);
          }).padLeft(6).minWidth(width - 360 - 18).fillX().growY().top();
        });
        pane.row();
        if (handle.meta.installMessage != null) pane.add(handle.meta.installMessage).fill().pad(4).wrap();
      }).top().padTop(20).fill();
    }).growY().fillX().maxWidth(width).pad(60);
  }
}
